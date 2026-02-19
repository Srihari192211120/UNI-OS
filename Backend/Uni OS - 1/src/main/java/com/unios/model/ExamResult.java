package com.unios.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "exam_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private Integer rank;

    @OneToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;
}
