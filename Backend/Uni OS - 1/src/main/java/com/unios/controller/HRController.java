package com.unios.controller;

import com.unios.model.Candidate;
import com.unios.repository.CandidateRepository;
import com.unios.service.agents.hr.OnboardingAgent;
import com.unios.service.agents.hr.RecruitmentAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recruitment")
public class HRController {

    private final CandidateRepository candidateRepository;
    private final RecruitmentAgent recruitmentAgent;
    private final OnboardingAgent onboardingAgent;

    public HRController(CandidateRepository candidateRepository,
            RecruitmentAgent recruitmentAgent,
            OnboardingAgent onboardingAgent) {
        this.candidateRepository = candidateRepository;
        this.recruitmentAgent = recruitmentAgent;
        this.onboardingAgent = onboardingAgent;
    }

    @PostMapping
    public ResponseEntity<Candidate> apply(@RequestBody Candidate candidate) {
        candidate.setStatus("APPLIED");
        Candidate saved = candidateRepository.save(candidate);

        // Trigger process
        recruitmentAgent.processCandidate(saved.getId());

        return ResponseEntity.ok(candidateRepository.findById(saved.getId()).orElse(saved));
    }

    @PostMapping("/{id}/onboard")
    public ResponseEntity<String> onboard(@PathVariable Long id) {
        onboardingAgent.onboard(id);
        return ResponseEntity.ok("Candidate status updated to ONBOARDED");
    }
}
