package io.github.gaalbu.heatcode.core;

public record ClassMetrics(
        String className,
        int cyclomaticComplexity,
        double duplicationRatio,
        int couplingOut,
        int churn30d,
        int churn90d,
        int linesOfCode) {
}
