package com.unios.controller;

import com.unios.model.SlotEnrollment;
import com.unios.model.StudentCredit;
import com.unios.model.SubjectOffering;
import com.unios.repository.SlotEnrollmentRepository;
import com.unios.repository.StudentCreditRepository;
import com.unios.service.agents.academics.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class AcademicController {

    private final SubjectOfferingAgent subjectOfferingAgent;
    private final SlotEnrollmentAgent slotEnrollmentAgent;
    private final AttendanceAgent attendanceAgent;
    private final RiskMonitoringAgent riskMonitoringAgent;
    private final StudentCreditRepository studentCreditRepository;
    private final SlotEnrollmentRepository slotEnrollmentRepository;

    private final ApplicationEventPublisher eventPublisher;

    public AcademicController(SubjectOfferingAgent subjectOfferingAgent,
            SlotEnrollmentAgent slotEnrollmentAgent,
            AttendanceAgent attendanceAgent,
            RiskMonitoringAgent riskMonitoringAgent,
            StudentCreditRepository studentCreditRepository,
            SlotEnrollmentRepository slotEnrollmentRepository,
            ApplicationEventPublisher eventPublisher) {
        this.subjectOfferingAgent = subjectOfferingAgent;
        this.slotEnrollmentAgent = slotEnrollmentAgent;
        this.attendanceAgent = attendanceAgent;
        this.riskMonitoringAgent = riskMonitoringAgent;
        this.studentCreditRepository = studentCreditRepository;
        this.slotEnrollmentRepository = slotEnrollmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/offerings")
    public ResponseEntity<SubjectOffering> createOffering(@RequestBody SubjectOffering request) {
        // Request body should ideally be a DTO, but using Entity for speed as per
        // instructions.
        // Expecting facultyId and batchId to be handled.
        // Logic inside agent expects raw params, extracting them here.
        // Assuming request has faculty and batch objects or IDs.
        // For simplicity, let's assume the client sends the full object structure or we
        // adjust Agent.
        // Let's adjust Controller to extract IDs if passed in JSON, or expect full
        // objects.
        // Given prompt "Calls SubjectOfferingAgent.createOffering", I'll extract IDs.

        Long facultyId = request.getFaculty() != null ? request.getFaculty().getId() : null;
        Long batchId = request.getBatch() != null ? request.getBatch().getId() : null;

        SubjectOffering offering = subjectOfferingAgent.createOffering(
                request.getSubjectName(),
                request.getSlot(),
                request.getCapacity(),
                request.getCredits(),
                request.getPrerequisite(),
                facultyId,
                batchId);
        return ResponseEntity.ok(offering);
    }

    @PostMapping("/offerings/{id}/activate")
    public ResponseEntity<String> activateOffering(@PathVariable Long id) {
        subjectOfferingAgent.activateOffering(id);
        return ResponseEntity.ok("Offering activated.");
    }

    @PostMapping("/enroll")
    public ResponseEntity<String> enroll(@RequestParam Long studentId, @RequestParam Long offeringId) {
        slotEnrollmentAgent.enroll(studentId, offeringId);
        return ResponseEntity.ok("Enrolled successfully.");
    }

    @PostMapping("/attendance")
    public ResponseEntity<String> markAttendance(@RequestParam Long enrollmentId, @RequestParam boolean present) {
        attendanceAgent.markAttendance(enrollmentId, present);
        return ResponseEntity.ok("Attendance marked.");
    }

    @GetMapping("/academic/progress/{studentId}")
    public ResponseEntity<Map<String, Object>> getProgress(@PathVariable Long studentId) {
        Map<String, Object> response = new HashMap<>();

        StudentCredit credit = studentCreditRepository.findByStudentId(studentId).orElse(null);
        response.put("earnedCredits", credit != null ? credit.getEarnedCredits() : 0);

        List<SlotEnrollment> enrollments = slotEnrollmentRepository.findByStudentId(studentId);
        response.put("enrolledSubjects", enrollments.size());

        long passed = enrollments.stream().filter(e -> "PASSED".equals(e.getStatus())).count();
        response.put("passedSubjects", passed);

        long failed = enrollments.stream().filter(e -> "FAILED".equals(e.getStatus())).count();
        response.put("failedSubjects", failed);

        response.put("riskStatus", riskMonitoringAgent.evaluateRisk(studentId));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/academics/complete")
    public ResponseEntity<String> completeSubject(@RequestBody CompletionRequest request) {
        eventPublisher.publishEvent(new com.unios.domain.events.AcademicCompletionEvent(this,
                request.getSlotEnrollmentId(), request.getMarks()));
        return ResponseEntity.ok("Academic completion recorded. Processing results.");
    }

    @lombok.Data
    static class CompletionRequest {
        private Long slotEnrollmentId;
        private int marks;
    }
}
