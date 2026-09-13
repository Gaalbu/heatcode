package io.github.gaalbu.heatcode.core;

public record WeightConfig(
        double complexityWeight,
        double duplicationWeight,
        double couplingWeight,
        double churnWeight) {
    public static WeightConfig defaults() {
        return new WeightConfig(0.40, 0.25, 0.20, 0.15);
    }

    public WeightConfig withoutChurn() {
        double total = complexityWeight + duplicationWeight + couplingWeight;
        if (total == 0) return defaults();
        return new WeightConfig(complexityWeight / total, duplicationWeight / total, couplingWeight / total, 0);
    }
}
