package com.carmanagement.resource;

import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Exposes which LLM the current Quarkus profile is using (Ollama, MaaS, or OpenAI).
 */
@Path("/llm")
public class LlmInfoResource {

    @ConfigProperty(name = "demo.llm.label")
    String label;

    @ConfigProperty(name = "quarkus.langchain4j.chat-model.provider")
    String provider;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> info() {
        return Map.of("provider", provider, "label", label);
    }
}
