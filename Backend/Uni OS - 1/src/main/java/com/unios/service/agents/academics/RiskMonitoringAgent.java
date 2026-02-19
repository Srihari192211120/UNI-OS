package com.unios.service.agents.academics;

import com.unios.model.SlotEnrollment;
import com.unios.model.StudentCredit;
import com.unios.repository.SlotEnrollmentRepository;
import com.unios.repository.StudentCreditRepository;
import com.unios.service.llm.LLMClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RiskMonitoringAgent {

    private final StudentCreditRepository studentCreditRepository;
    private final SlotEnrollmentRepository slotEnrollmentRepository;
    private final LLMClient llmClient;
    private final com.unios.service.policy.PolicyEngineService policyEngineService;

    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public RiskMonitoringAgent(StudentCreditRepository studentCreditRepository,
            SlotEnrollmentRepository slotEnrollmentRepository,
            LLMClient llmClient,
            com.unios.service.policy.PolicyEngineService policyEngineService,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.studentCreditRepository = studentCreditRepository;
        this.slotEnrollmentRepository = slotEnrollmentRepository;
        this.llmClient = llmClient;
        this.policyEngineService = policyEngineService;
        this.eventPublisher = eventPublisher;
    }

    @org.springframework.context.event.EventListener
    @Transactional
    public void onCreditsUpdated(com.unios.domain.events.CreditsUpdatedEvent event) {
        String risk = evaluateRisk(event.getStudentId());
        System.out.println("[RISK] Student " + event.getStudentId() + " Status: " + risk);

        if (risk.contains("High Risk")) {
            eventPublisher
                    .publishEvent(new com.unios.domain.events.RiskDetectedEvent(this, event.getStudentId(), risk));
        }
    }

    @Transactional(readOnly = true)
    public String evaluateRisk(Long studentId) {
        StudentCredit credit = studentCreditRepository.findByStudentId(studentId).orElse(null);
        int earned = (credit != null) ? credit.getEarnedCredits() : 0;

        List<SlotEnrollment> failed = slotEnrollmentRepository.findByStudentIdAndStatus(studentId, "FAILED");
        int failedCount = failed.size();

        String systemPrompt = "You are an Academic Advisor. Evaluate the student's risk level based on performance data.";
        String userPrompt = "Earned Credits: " + earned + ". Failed Courses: " + failedCount;

        Double maxFailed = policyEngineService.getPolicyValue("MAX_FAILED_COURSES", 2.0,
                "Max failed courses before High Risk auto-flag");

        if (failedCount > maxFailed) {
            return "High Risk (Policy Violation: >" + maxFailed + " failed courses)";
        }

        String response = llmClient.generateResponse(systemPrompt, userPrompt);

        // Extract risk from response or pass full response if short
        if (response.contains("High") || response.contains("Risk")) {
            // Return the LLM's assessment
            return response;
        }

        return "Active (LLM-Verified)";
    }
}
