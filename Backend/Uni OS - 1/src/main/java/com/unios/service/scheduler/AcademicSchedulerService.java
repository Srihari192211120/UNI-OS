package com.unios.service.scheduler;

import com.unios.domain.events.AcademicCompletionEvent;
import com.unios.model.SlotEnrollment;
import com.unios.repository.AttendanceRepository;
import com.unios.repository.SlotEnrollmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class AcademicSchedulerService {

    private final SlotEnrollmentRepository slotEnrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Random random = new Random();

    public AcademicSchedulerService(SlotEnrollmentRepository slotEnrollmentRepository,
            AttendanceRepository attendanceRepository,
            ApplicationEventPublisher eventPublisher) {
        this.slotEnrollmentRepository = slotEnrollmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedRate = 60000) // Every 1 minute
    @Transactional
    public void runAcademicCycle() {
        log.info("[SCHEDULER] Running Academic Cycle...");

        List<SlotEnrollment> activeEnrollments = slotEnrollmentRepository.findAll().stream()
                .filter(e -> "ENROLLED".equals(e.getStatus()))
                .toList();

        for (SlotEnrollment enrollment : activeEnrollments) {
            long totalClasses = attendanceRepository.countBySlotEnrollmentId(enrollment.getId());
            if (totalClasses == 0)
                continue;

            long present = attendanceRepository.countBySlotEnrollmentIdAndPresentTrue(enrollment.getId());
            double attendancePercentage = (double) present / totalClasses * 100.0;

            // Simplified Logic: If attendance >= 75% AND we have enough data points (e.g.,
            // > 0),
            // we simulate an assessment and complete the subject.
            // "Assessment exists" is simulated by just checking if they attended enough to
            // be eligible.

            if (attendancePercentage >= 75.0) {
                // Simulate Marks (40-100)
                int marks = 40 + random.nextInt(61);

                log.info(
                        "[SCHEDULER] Auto-completing subject for Student {} in Offering {}. Attendance: {}%, Marks: {}",
                        enrollment.getStudent().getId(), enrollment.getSubjectOffering().getId(), attendancePercentage,
                        marks);

                eventPublisher.publishEvent(new AcademicCompletionEvent(this, enrollment.getId(), marks));
            }
        }
    }
}
