package com.unios.service.agents.graduation;

import com.unios.model.StudentCredit;
import com.unios.repository.StudentCreditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GraduationEligibilityAgent {

    private final StudentCreditRepository studentCreditRepository;

    public GraduationEligibilityAgent(StudentCreditRepository studentCreditRepository) {
        this.studentCreditRepository = studentCreditRepository;
    }

    @Transactional(readOnly = true)
    public boolean checkEligibility(Long studentId) {
        StudentCredit credit = studentCreditRepository.findByStudentId(studentId).orElse(null);
        if (credit == null)
            return false;

        // Requirement: 120 credits
        return credit.getEarnedCredits() >= 120;
    }
}
