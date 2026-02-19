package com.unios.service.llm;

import reactor.core.publisher.Mono;

public interface LLMClient {
    /**
     * Sends a prompt to the LLM and returns the response text.
     * 
     * @param systemPrompt Context or role definition for the LLM.
     * @param userPrompt   The specific task or input data.
     * @return The LLM's response.
     */
    String generateResponse(String systemPrompt, String userPrompt);
}
