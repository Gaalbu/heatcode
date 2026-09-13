package io.github.gaalbu.heatcode.cli;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.gaalbu.heatcode.core.HeatScore;
import io.github.gaalbu.heatcode.core.ReportCodec;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class LlmExplainer {
    private static final int MAX_SOURCE_BYTES = 100_000;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    String explain(String endpoint, String apiKey, HeatScore score, String source) throws IOException, InterruptedException {
        if (source.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_SOURCE_BYTES) {
            throw new IllegalArgumentException("Selected source is larger than 100 KB");
        }
        URI uri = URI.create(endpoint);
        if (!"https".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException("LLM endpoint must use HTTPS");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("HEATCODE_LLM_API_KEY is not configured");
        String prompt = "Explain concrete refactoring risks for this selected Java class. Do not invent facts.\n"
                + "Class: " + score.className() + "\nMetrics: " + score.metrics() + "\nSource:\n" + source;
        var payload = ReportCodec.mapper().createObjectNode().put("prompt", prompt);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(ReportCodec.mapper().writeValueAsString(payload))).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("LLM endpoint returned HTTP " + response.statusCode());
        JsonNode body = ReportCodec.mapper().readTree(response.body());
        return body.path("explanation").asText(body.path("output").asText(response.body()));
    }
}
