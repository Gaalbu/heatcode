package io.github.gaalbu.heatcode.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeatScoreCalculatorTest {
    @Test
    void redistributesChurnWeightWhenNoGitHistoryExists() {
        var metrics = List.of(
                new ClassMetrics("Clean", 1, 0, 0, 0, 0, 10),
                new ClassMetrics("Hot", 5, 1, 4, 0, 0, 40));

        var scores = new HeatScoreCalculator().calculate(metrics, WeightConfig.defaults());

        assertEquals("Hot", scores.getFirst().className());
        assertEquals(100.0, scores.getFirst().score(), 0.0001);
        assertEquals("RED", scores.getFirst().band());
    }
}
