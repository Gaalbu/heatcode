package io.github.gaalbu.heatcode.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WeightConfigLoader {
    private WeightConfigLoader() { }

    public static WeightConfig load(Path project) throws IOException {
        Path config = project.resolve("heatcode.yml");
        if (!Files.isRegularFile(config)) return WeightConfig.defaults();
        JsonNode weights = new ObjectMapper(new YAMLFactory()).readTree(Files.readString(config)).path("weights");
        if (weights.isMissingNode()) weights = new ObjectMapper(new YAMLFactory()).readTree(Files.readString(config));
        WeightConfig defaults = WeightConfig.defaults();
        return new WeightConfig(
                value(weights, "complexity", "complexityWeight", defaults.complexityWeight()),
                value(weights, "duplication", "duplicationWeight", defaults.duplicationWeight()),
                value(weights, "coupling", "couplingWeight", defaults.couplingWeight()),
                value(weights, "churn", "churnWeight", defaults.churnWeight()));
    }

    private static double value(JsonNode node, String shortName, String longName, double fallback) {
        JsonNode value = node.has(shortName) ? node.get(shortName) : node.get(longName);
        return value != null && value.isNumber() ? value.asDouble() : fallback;
    }
}
