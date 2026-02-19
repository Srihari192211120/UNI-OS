package com.unios.controller;

import com.unios.service.agents.graduation.CertificationAgent;
import com.unios.service.agents.graduation.GraduationEligibilityAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map; // Import Map

@RestController
@RequestMapping("/graduation")
public class GraduationController {

    private final GraduationEligibilityAgent eligibilityAgent;
    private final CertificationAgent certificationAgent;

    public GraduationController(GraduationEligibilityAgent eligibilityAgent,
            CertificationAgent certificationAgent) {
        this.eligibilityAgent = eligibilityAgent;
        this.certificationAgent = certificationAgent;
    }

    @PostMapping("/run/{studentId}")
    public ResponseEntity<Map<String, String>> runGraduation(@PathVariable Long studentId) {
        boolean eligible = eligibilityAgent.checkEligibility(studentId);

        if (eligible) {
            certificationAgent.issueCertificate(studentId);
            return ResponseEntity.ok(Collections.singletonMap("status", "GRADUATED"));
        } else {
            return ResponseEntity.ok(Collections.singletonMap("status", "NOT_ELIGIBLE"));
        }
    }
}
