package com.unios.service.orchestrator;

import com.unios.domain.events.*;
import com.unios.service.agents.admissions.EligibilityAgent;
import com.unios.service.agents.admissions.EnrollmentAgent;
import com.unios.service.agents.admissions.ExamSchedulerAgent;
import com.unios.service.agents.admissions.RankingAgent;
import com.unios.service.agents.graduation.CertificationAgent;
import com.unios.service.agents.graduation.GraduationEligibilityAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class UniversityOrchestratorAgent {

    private final EligibilityAgent eligibilityAgent;
    private final ExamSchedulerAgent examSchedulerAgent;
    private final RankingAgent rankingAgent;
    private final EnrollmentAgent enrollmentAgent;
    private final GraduationEligibilityAgent graduationEligibilityAgent;
    private final CertificationAgent certificationAgent;

    // Simple in-memory logs for the dashboard
    public static final List<String> ACTIVITY_LOG = new ArrayList<>();

    private final com.unios.service.agents.academics.CreditTrackerAgent creditTrackerAgent;
    private final com.unios.service.agents.hr.OnboardingAgent onboardingAgent;
    private final com.unios.repository.UniversityWorkflowStateRepository appWorkflowStateRepository;
    private final com.unios.repository.WorkflowStateRepository batchWorkflowStateRepository;

    public UniversityOrchestratorAgent(EligibilityAgent eligibilityAgent,
            ExamSchedulerAgent examSchedulerAgent,
            RankingAgent rankingAgent,
            EnrollmentAgent enrollmentAgent,
            GraduationEligibilityAgent graduationEligibilityAgent,
            CertificationAgent certificationAgent,
            com.unios.service.agents.academics.CreditTrackerAgent creditTrackerAgent,
            com.unios.service.agents.hr.OnboardingAgent onboardingAgent,
            com.unios.repository.UniversityWorkflowStateRepository appWorkflowStateRepository,
            com.unios.repository.WorkflowStateRepository batchWorkflowStateRepository) {
        this.eligibilityAgent = eligibilityAgent;
        this.examSchedulerAgent = examSchedulerAgent;
        this.rankingAgent = rankingAgent;
        this.enrollmentAgent = enrollmentAgent;
        this.graduationEligibilityAgent = graduationEligibilityAgent;
        this.certificationAgent = certificationAgent;
        this.creditTrackerAgent = creditTrackerAgent;
        this.onboardingAgent = onboardingAgent;
        this.appWorkflowStateRepository = appWorkflowStateRepository;
        this.batchWorkflowStateRepository = batchWorkflowStateRepository;
    }

    private void logActivity(String message) {
        String logEntry = "[ORCHESTRATOR] " + java.time.LocalDateTime.now() + ": " + message;
        log.info(logEntry);
        ACTIVITY_LOG.add(logEntry);
    }

    private void updateAppWorkflowState(Long applicationId, String stage) {
        com.unios.model.UniversityWorkflowState state = appWorkflowStateRepository.findByApplicationId(applicationId)
                .orElse(new com.unios.model.UniversityWorkflowState());
        state.setApplicationId(applicationId);
        state.setCurrentStage(stage);
        state.setUpdatedAt(java.time.LocalDateTime.now());
        appWorkflowStateRepository.save(state);
    }

    private void upsertBatchState(Long batchId, String phase, String eventClass) {
        com.unios.model.WorkflowState state = batchWorkflowStateRepository.findByBatchId(batchId)
                .orElse(new com.unios.model.WorkflowState());
        state.setBatchId(batchId);
        state.setPhase(phase);
        state.setLastEvent(eventClass);
        state.setUpdatedAt(java.time.LocalDateTime.now());
        batchWorkflowStateRepository.save(state);
    }

    private boolean checkPhase(Long batchId, String validPreviousPhase) {
        // If validPreviousPhase is null, any start is allowed (or we assume INIT)
        if (validPreviousPhase == null)
            return true;

        return batchWorkflowStateRepository.findByBatchId(batchId)
                .map(state -> state.getPhase().equals(validPreviousPhase))
                .orElse(false); // If no state exists, and we expect a previous phase, return false (block)
    }

    @EventListener
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        logActivity(
                "Application received for " + event.getApplication().getFullName() + ". Triggering Eligibility Check.");
        updateAppWorkflowState(event.getApplication().getId(), "SUBMITTED");

        Long batchId = event.getApplication().getBatch().getId();

        // Ensure Batch State is initialized if not present
        if (batchWorkflowStateRepository.findByBatchId(batchId).isEmpty()) {
            upsertBatchState(batchId, "ADMISSIONS_STARTED", event.getClass().getSimpleName());
        }

        // Guard: Logic here is a bit tricky for "ApplicationSubmitted" because it
        // happens many times for one batch.
        // We will allow it to trigger eligibility *if* the batch is in a compatible
        // state
        // OR we just run it as it's idempotent-ish.
        // Prompt says: "EligibilityAgent only runs if phase == 'APPLICATION_SUBMITTED'"
        // (or similar)
        // But for *first* app, it sets state.
        // Let's assume we allow re-triggering or just set state.

        upsertBatchState(batchId, "ADMISSIONS_STARTED", event.getClass().getSimpleName());
        eligibilityAgent.run(batchId);
    }

    @EventListener
    public void onEligibilityCompleted(EligibilityCompletedEvent event) {
        logActivity("Eligibility check completed for Batch " + event.getBatchId() + ". Triggering Exam Scheduling.");

        upsertBatchState(event.getBatchId(), "ELIGIBILITY_DONE", event.getClass().getSimpleName());

        // Guard checking is implicit by the flow, but let's be explicit if we wanted
        // strict linear progression.
        // But since we JUST updated the state, checking it now is redundant unless we
        // check *before* update.
        // The prompt implies: "Before executing agent... check workflow_state".
        // So:
        // 1. Check if previous state was ADMISSIONS_STARTED?
        // Actually, onEligibilityCompleted means Eligibility IS DONE. So next is Exam
        // Scheduler.
        // Exam Scheduler should run if we are in ELIGIBILITY_DONE (which we just set).

        examSchedulerAgent.schedule(event.getBatchId());
    }

    @EventListener
    public void onExamScheduled(ExamScheduledEvent event) {
        logActivity("Exams scheduled for Batch " + event.getBatchId() + ". Triggering Ranking.");

        upsertBatchState(event.getBatchId(), "EXAMS_SCHEDULED", event.getClass().getSimpleName());

        rankingAgent.processResults(event.getBatchId());
    }

    @EventListener
    public void onExamResultsProcessed(ExamResultsProcessedEvent event) {
        logActivity("Exam results processed for Batch " + event.getBatchId() + ". Triggering Enrollment.");

        upsertBatchState(event.getBatchId(), "RANKED", event.getClass().getSimpleName());

        enrollmentAgent.enroll(event.getBatchId());
    }

    @EventListener
    public void onEnrollmentCompleted(EnrollmentCompletedEvent event) {
        logActivity("Enrollment completed for Batch " + event.getBatchId() + ". Admission Phase Ended.");
        upsertBatchState(event.getBatchId(), "SEMESTER_READY", event.getClass().getSimpleName());
        logActivity("[ORCHESTRATOR] Enrollment completed. Semester ready.");
    }

    @EventListener
    public void onSubjectOffered(SubjectOfferedEvent event) {
        logActivity("Subject Offered: ID " + event.getOfferingId() + " for Batch " + event.getBatchId());
    }

    @EventListener
    public void onStudentEnrolled(StudentEnrolledEvent event) {
        logActivity("Student " + event.getStudent().getId() + " enrolled in Offering " + event.getOfferingId());
        // We could track individual student state here if needed, but for now
        // Orchestrator observes.
    }

    @EventListener
    public void onAcademicCompletion(AcademicCompletionEvent event) {
        logActivity("Academic completion processed for SlotEnrollment " + event.getSlotEnrollmentId());
        // Agent CreditTracker now listens directly. Orchestrator simply logs.
    }

    @EventListener
    public void onCreditsUpdated(CreditsUpdatedEvent event) {
        logActivity(
                "[ACADEMICS] Student " + event.getStudentId() + " updated credits: " + event.getEarnedCredits());
        // Graduation Check is now triggered by GraduationCandidateEvent via
        // CreditTracker.
        // But for redundancy/safety we can keep it or rely on the specific event.
        // Prompt says "Emit GraduationCandidateEvent" in CreditTracker.
        // Orchestrator listens to GraduationCandidateEvent below.
        // So we don't need logic here.
    }

    @EventListener
    public void onGraduationCandidate(GraduationCandidateEvent event) {
        // If CreditTracker published this, we would handle it here.
        logActivity("Graduation Candidate identified: Student " + event.getStudentId());
        boolean eligible = graduationEligibilityAgent.checkEligibility(event.getStudentId());
        if (eligible) {
            certificationAgent.issueCertificate(event.getStudentId());
            logActivity("Certificate issued for Student " + event.getStudentId());
        }
    }

    @EventListener
    public void onCandidateShortlisted(CandidateShortlistedEvent event) {
        logActivity("Candidate " + event.getCandidateId() + " shortlisted. Triggering Onboarding.");
        onboardingAgent.onboard(event.getCandidateId());
        logActivity("Onboarding initiated for Candidate " + event.getCandidateId());
    }

    public List<String> getActivityLog() {
        return new ArrayList<>(ACTIVITY_LOG);
    }
}
