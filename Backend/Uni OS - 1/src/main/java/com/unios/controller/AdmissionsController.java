package com.unios.controller;

import com.unios.domain.events.ApplicationSubmittedEvent;
import com.unios.model.Application;
import com.unios.model.Batch;
import com.unios.repository.ApplicationRepository;
import com.unios.repository.BatchRepository;
import com.unios.service.agents.admissions.EligibilityAgent;
import com.unios.service.agents.admissions.EnrollmentAgent;
import com.unios.service.agents.admissions.ExamSchedulerAgent;
import com.unios.service.agents.admissions.RankingAgent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class AdmissionsController {

    private final ApplicationRepository applicationRepository;
    private final BatchRepository batchRepository;
    private final EligibilityAgent eligibilityAgent;
    private final ExamSchedulerAgent examSchedulerAgent;
    private final RankingAgent rankingAgent;
    private final EnrollmentAgent enrollmentAgent;
    private final ApplicationEventPublisher eventPublisher;

    public AdmissionsController(ApplicationRepository applicationRepository,
            BatchRepository batchRepository,
            EligibilityAgent eligibilityAgent,
            ExamSchedulerAgent examSchedulerAgent,
            RankingAgent rankingAgent,
            EnrollmentAgent enrollmentAgent,
            ApplicationEventPublisher eventPublisher) {
        this.applicationRepository = applicationRepository;
        this.batchRepository = batchRepository;
        this.eligibilityAgent = eligibilityAgent;
        this.examSchedulerAgent = examSchedulerAgent;
        this.rankingAgent = rankingAgent;
        this.enrollmentAgent = enrollmentAgent;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/applications")
    public ResponseEntity<Application> createApplication(@RequestBody Application application) {
        if (application.getBatch() != null && application.getBatch().getId() != null) {
            Batch batch = batchRepository.findById(application.getBatch().getId())
                    .orElseThrow(() -> new RuntimeException("Batch not found"));
            application.setBatch(batch);
        }
        application.setStatus("SUBMITTED");
        Application saved = applicationRepository.save(application);

        // Publish Event instead of doing nothing or calling agent directly
        eventPublisher.publishEvent(new ApplicationSubmittedEvent(this, saved));

        return ResponseEntity.ok(saved);
    }

    // Manual endpoints removed as per strict autonomous pipeline requirements.
    // Restoring processResults for verification purposes as per latest request.
    @PostMapping("/admissions/results/process")
    public ResponseEntity<String> processResults(@RequestParam Long batchId) {
        rankingAgent.processResults(batchId);
        return ResponseEntity.ok("Results processed (Ranking complete) for batch " + batchId);
    }
}
