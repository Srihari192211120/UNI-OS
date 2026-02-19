package com.unios.repository;

import com.unios.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    long countBySlotEnrollmentId(Long slotEnrollmentId);

    long countBySlotEnrollmentIdAndPresentTrue(Long slotEnrollmentId);
}
