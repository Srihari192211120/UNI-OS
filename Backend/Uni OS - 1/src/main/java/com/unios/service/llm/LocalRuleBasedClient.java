package com.unios.service.llm;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Primary
public class LocalRuleBasedClient implements LLMClient {

    @Override
    public String generateResponse(String systemPrompt, String userPrompt) {
        if (systemPrompt.contains("Admissions Officer")) {
            return handleEligibility(userPrompt);
        } else if (systemPrompt.contains("HR Manager")) {
            return handleRecruitment(userPrompt);
        } else if (systemPrompt.contains("Academic Advisor")) {
            return handleRiskAssessment(userPrompt);
        }
        return "Unrecognized prompt context.";
    }

    private String handleEligibility(String prompt) {
        // Expected Prompt: "Applicant: John Doe. Document Text: ... Physics: 90,
        // Chemistry: 85 ..."
        // Logic: Extract scores, check avg > 60.

        int physics = extractScore(prompt, "Physics");
        int chemistry = extractScore(prompt, "Chemistry");
        int math = extractScore(prompt, "Math");

        // If scores missing, try to find "Score: XX" or similar generic pattern
        if (physics == 0 && chemistry == 0 && math == 0) {
            // Fallback for "Academic Score: 85"
            int academicScore = extractScore(prompt, "Academic Score");
            if (academicScore > 60)
                return "ELIGIBLE (Based on Academic Score)";
            return "INELIGIBLE (Insufficient Academic Score)";
        }

        double avg = (physics + chemistry + math) / 3.0;
        if (avg > 60) {
            return "ELIGIBLE (Average Score: " + String.format("%.2f", avg) + ")";
        }
        return "INELIGIBLE (Average Score: " + String.format("%.2f", avg) + " < 60)";
    }

    private String handleRecruitment(String prompt) {
        // Expected: "Candidate Name: ... Department: Computer Science"
        // Logic: Accept CS, Engineering, IT. Reject others.

        String lower = prompt.toLowerCase();
        if (lower.contains("computer science") || lower.contains("engineering")
                || lower.contains("information technology")) {
            return "SELECTED (Department match)";
        }
        return "REJECTED (Department not priority)";
    }

    private String handleRiskAssessment(String prompt) {
        // Expected: "Earned Credits: 10. Failed Courses: 2"
        // Logic: Failed > 0 -> High Risk.

        int failed = extractScore(prompt, "Failed Courses");
        int earned = extractScore(prompt, "Earned Credits");

        if (failed > 0) {
            return "High Risk (Failed " + failed + " courses)";
        }
        if (earned < 15) {
            return "Moderate Risk (Low credits)";
        }
        return "Active (Good standing)";
    }

    private int extractScore(String text, String label) {
        // Regex to find "Label: 90" or "Label 90"
        Pattern p = Pattern.compile(label + "[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0; // Not found
    }
}
