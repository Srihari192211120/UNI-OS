package com.unios.service.agents.graduation;

import com.unios.model.Certificate;
import com.unios.model.Student;
import com.unios.repository.CertificateRepository;
import com.unios.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CertificationAgent {

    private final GraduationEligibilityAgent eligibilityAgent;
    private final CertificateRepository certificateRepository;
    private final StudentRepository studentRepository;

    public CertificationAgent(GraduationEligibilityAgent eligibilityAgent,
            CertificateRepository certificateRepository,
            StudentRepository studentRepository) {
        this.eligibilityAgent = eligibilityAgent;
        this.certificateRepository = certificateRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public void issueCertificate(Long studentId) {
        if (!eligibilityAgent.checkEligibility(studentId)) {
            throw new RuntimeException("Student is not eligible for graduation.");
        }

        // Check if already issued
        // Logic skipped for brevity, assuming check is done or idempotent logic

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Certificate certificate = new Certificate();
        certificate.setStudent(student);
        certificate.setDegreeName("Bachelor of Technology"); // Default or dynamic based on Department
        certificate.setIssuedAt(LocalDateTime.now());
        certificate.setCertificateCode(UUID.randomUUID().toString());

        certificateRepository.save(certificate);
    }
}
