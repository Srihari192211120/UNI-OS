package com.unios.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "slot_enrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "subject_offering_id", nullable = false)
    private SubjectOffering subjectOffering;

    @Column(nullable = false)
    private String status; // ENROLLED, PASSED, FAILED
}
