package io.github.gaalbu.heatcode.cli;

import io.github.gaalbu.heatcode.core.ClassMetrics;
import io.github.gaalbu.heatcode.core.HeatScore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LlmExplainerTest {
    @Test
    void rejectsNonHttpsEndpointsBeforeSendingSource() {
        var score = new HeatScore("Example", 50, "YELLOW", new ClassMetrics("Example", 2, 0, 0, 0, 0, 10));

        assertThrows(IllegalArgumentException.class, () -> new LlmExplainer().explain("http://localhost:8080", "secret", score, "class Example {}"));
    }
}
