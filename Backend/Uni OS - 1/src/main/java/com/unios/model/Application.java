package com.unios.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Double academicScore;

    @Column(nullable = false)
    private Boolean documentsVerified;

    @Column(nullable = false)
    private String status; // SUBMITTED, ELIGIBLE, INELIGIBLE, SELECTED, WAITLISTED, REJECTED

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;
}
