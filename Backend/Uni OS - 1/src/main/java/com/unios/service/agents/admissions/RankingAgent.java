package com.unios.service.agents.admissions;

import com.unios.model.Application;
import com.unios.model.ExamResult;
import com.unios.repository.ApplicationRepository;
import com.unios.repository.ExamResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class RankingAgent {

    private final ApplicationRepository applicationRepository;
    private final ExamResultRepository examResultRepository;
    private final com.unios.repository.EntranceExamSessionRepository entranceExamSessionRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public RankingAgent(ApplicationRepository applicationRepository,
            ExamResultRepository examResultRepository,
            com.unios.repository.EntranceExamSessionRepository entranceExamSessionRepository,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.applicationRepository = applicationRepository;
        this.examResultRepository = examResultRepository;
        this.entranceExamSessionRepository = entranceExamSessionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processResults(Long batchId) {
        List<com.unios.model.EntranceExamSession> examSessions = entranceExamSessionRepository.findByBatchId(batchId);

        if (examSessions.isEmpty())
            return;

        List<ExamResult> results = new java.util.ArrayList<>();

        // 1. Generate/Fetch Scores (Deterministic)
        for (com.unios.model.EntranceExamSession session : examSessions) {
            // Requirement: Use examScore. If null, use fallback (deterministic).
            // Prompt says: "Default: examScore = random 40–100 ONLY if NULL (temporary
            // fallback)."
            // But also: "No randomness allowed anywhere in Admissions."
            // So I will use deterministic fallback: (seatNumber * 5) % 100 or similar if
            // seat exists, else ID based.

            Integer score = session.getExamScore();
            if (score == null) {
                // Deterministic fallback
                int seed = session.getSeatNumber() != null ? session.getSeatNumber() : session.getId().intValue();
                score = 40 + (seed % 61); // 40 to 100
                session.setExamScore(score);
                entranceExamSessionRepository.save(session);
            }

            ExamResult result = new ExamResult();
            result.setApplication(session.getApplication());
            result.setScore((double) score); // ExamResult uses Double currently
            results.add(result);
        }

        // 2. Sort by Score Descending
        results.sort(Comparator.comparingDouble(ExamResult::getScore).reversed());

        // 3. Assign Ranks and Status
        int total = results.size();
        int selectedCount = (int) Math.ceil(total * 0.6); // Top 60%
        int waitlistedCount = (int) Math.ceil(total * 0.2); // Next 20%

        for (int i = 0; i < total; i++) {
            ExamResult result = results.get(i);
            int rank = i + 1;
            result.setRank(rank);
            examResultRepository.save(result);

            Application app = result.getApplication();
            if (i < selectedCount) {
                app.setStatus("SELECTED");
            } else if (i < selectedCount + waitlistedCount) {
                app.setStatus("WAITLISTED");
            } else {
                app.setStatus("REJECTED");
            }
            applicationRepository.save(app);

            System.out.println(
                    "[ADMISSIONS] Application " + app.getId() + " scored " + result.getScore() + " ranked #" + rank);
        }

        // Publish Event
        eventPublisher.publishEvent(new com.unios.domain.events.ExamResultsProcessedEvent(this, batchId));
    }
}
