package com.unios.service.agents.academics;

import com.unios.model.Attendance;
import com.unios.model.SlotEnrollment;
import com.unios.repository.AttendanceRepository;
import com.unios.repository.SlotEnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AttendanceAgent {

    private final AttendanceRepository attendanceRepository;
    private final SlotEnrollmentRepository slotEnrollmentRepository;

    public AttendanceAgent(AttendanceRepository attendanceRepository,
            SlotEnrollmentRepository slotEnrollmentRepository) {
        this.attendanceRepository = attendanceRepository;
        this.slotEnrollmentRepository = slotEnrollmentRepository;
    }

    @Transactional
    public void markAttendance(Long enrollmentId, boolean present) {
        SlotEnrollment enrollment = slotEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        Attendance attendance = new Attendance();
        attendance.setSlotEnrollment(enrollment);
        attendance.setDate(LocalDate.now());
        attendance.setPresent(present);

        attendanceRepository.save(attendance);
    }
}
