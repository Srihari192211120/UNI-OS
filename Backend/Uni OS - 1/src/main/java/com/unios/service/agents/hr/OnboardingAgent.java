package com.unios.service.agents.hr;

import com.unios.model.Candidate;
import com.unios.model.Staff;
import com.unios.repository.CandidateRepository;
import com.unios.repository.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingAgent {

    private final CandidateRepository candidateRepository;
    private final StaffRepository staffRepository;

    public OnboardingAgent(CandidateRepository candidateRepository, StaffRepository staffRepository) {
        this.candidateRepository = candidateRepository;
        this.staffRepository = staffRepository;
    }

    @Transactional
    public void onboard(Long candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        if (!"SELECTED".equals(candidate.getStatus())) {
            throw new RuntimeException("Candidate is not in SELECTED status.");
        }

        Staff staff = new Staff();
        staff.setFullName(candidate.getFullName());
        staff.setEmail(candidate.getEmail());
        staff.setDepartment(candidate.getDepartment());
        staff.setRole("FACULTY"); // Defaulting to Faculty for now as per prompt implication or generic role
        staff.setActive(true);
        staffRepository.save(staff);

        candidate.setStatus("ONBOARDED");
        candidateRepository.save(candidate);
    }
}
