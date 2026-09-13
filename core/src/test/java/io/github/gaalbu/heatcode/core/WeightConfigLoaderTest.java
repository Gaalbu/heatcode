package io.github.gaalbu.heatcode.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightConfigLoaderTest {
    @Test
    void readsOptionalWeightsFromYaml() throws Exception {
        var root = Files.createTempDirectory("heatcode-config-");
        Files.writeString(root.resolve("heatcode.yml"), "weights:\n  complexity: 0.5\n  duplication: 0.2\n  coupling: 0.2\n  churn: 0.1\n");

        var weights = WeightConfigLoader.load(root);

        assertEquals(0.5, weights.complexityWeight());
        assertEquals(0.1, weights.churnWeight());
    }
}
