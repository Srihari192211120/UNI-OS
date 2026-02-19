package com.unios.service.agents.admissions;

import com.unios.model.Application;
import com.unios.repository.ApplicationRepository;
import com.unios.service.llm.LLMClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EligibilityAgent {

    private final ApplicationRepository applicationRepository;
    private final LLMClient llmClient;
    private final com.unios.service.policy.PolicyEngineService policyEngineService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public EligibilityAgent(ApplicationRepository applicationRepository, LLMClient llmClient,
            com.unios.service.policy.PolicyEngineService policyEngineService,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.applicationRepository = applicationRepository;
        this.llmClient = llmClient;
        this.policyEngineService = policyEngineService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void run(Long batchId) {
        List<Application> submittedApps = applicationRepository.findByBatchIdAndStatus(batchId, "SUBMITTED");

        if (submittedApps.isEmpty())
            return;

        Double cutoff = policyEngineService.getPolicyValue("ELIGIBILITY_CUTOFF", 60.0,
                "Minimum score for admission eligibility");

        for (Application app : submittedApps) {
            // Deterministic Logic: Check academic score and document verification
            boolean scorePass = app.getAcademicScore() != null && app.getAcademicScore() >= cutoff;
            boolean docsVerified = Boolean.TRUE.equals(app.getDocumentsVerified());

            if (scorePass && docsVerified) {
                app.setStatus("ELIGIBLE");
            } else {
                app.setStatus("INELIGIBLE");
            }
            applicationRepository.save(app);
        }

        // Publish Event
        eventPublisher.publishEvent(new com.unios.domain.events.EligibilityCompletedEvent(this, batchId));
    }
}
