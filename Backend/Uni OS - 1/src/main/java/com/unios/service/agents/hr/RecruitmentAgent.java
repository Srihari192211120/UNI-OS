package com.unios.service.agents.hr;

import com.unios.model.Candidate;
import com.unios.repository.CandidateRepository;
import com.unios.service.llm.LLMClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruitmentAgent {

    private final CandidateRepository candidateRepository;
    private final LLMClient llmClient;

    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public RecruitmentAgent(CandidateRepository candidateRepository, LLMClient llmClient,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.candidateRepository = candidateRepository;
        this.llmClient = llmClient;
        this.eventPublisher = eventPublisher;
    }

    @org.springframework.context.event.EventListener
    @Transactional
    public void onJobOpeningCreated(com.unios.domain.events.JobOpeningCreatedEvent event) {
        processCandidate(event.getCandidateId());
    }

    @Transactional
    public void processCandidate(Long candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        if (candidate.getDepartment() != null && !candidate.getDepartment().isEmpty()) {
            candidate.setStatus("SHORTLISTED");

            // LLM Analysis
            String systemPrompt = "You are an HR Manager. Analyze the candidate and provide a hiring recommendation (SELECTED/REJECTED).";
            String userPrompt = "Candidate Name: " + candidate.getFullName() + ". Department: "
                    + candidate.getDepartment();

            String response = llmClient.generateResponse(systemPrompt, userPrompt);

            if (response.contains("SELECTED")) {
                candidate.setStatus("SELECTED");
                // Emit Shortlisted Event (which implies "Selected for Onboarding")
                eventPublisher
                        .publishEvent(new com.unios.domain.events.CandidateShortlistedEvent(this, candidate.getId()));
            } else {
                candidate.setStatus("REJECTED");
            }
        } else {
            candidate.setStatus("REJECTED"); // No department? Reject.
        }

        candidateRepository.save(candidate);
    }
}
