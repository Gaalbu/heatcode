package io.github.gaalbu.heatcode.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HeatScoreCalculator {
    public List<HeatScore> calculate(List<ClassMetrics> metrics, WeightConfig requestedWeights) {
        if (metrics.isEmpty()) return List.of();
        WeightConfig weights = requestedWeights == null ? WeightConfig.defaults() : requestedWeights;
        boolean hasChurn = metrics.stream().anyMatch(m -> m.churn30d() > 0 || m.churn90d() > 0);
        if (!hasChurn) weights = weights.withoutChurn();
        final WeightConfig effectiveWeights = weights;
        int maxComplexity = max(metrics, ClassMetrics::cyclomaticComplexity);
        int maxCoupling = max(metrics, ClassMetrics::couplingOut);
        int maxChurn = max(metrics, ClassMetrics::churn90d);
        return metrics.stream().map(m -> {
            double complexity = relative(m.cyclomaticComplexity(), maxComplexity);
            double coupling = relative(m.couplingOut(), maxCoupling);
            double churn = relative(m.churn90d(), maxChurn);
            double score = 100 * (effectiveWeights.complexityWeight() * complexity
                    + effectiveWeights.duplicationWeight() * clamp(m.duplicationRatio())
                    + effectiveWeights.couplingWeight() * coupling
                    + effectiveWeights.churnWeight() * churn);
            return new HeatScore(m.className(), score, band(score), m);
        }).sorted(Comparator.comparingDouble(HeatScore::score).reversed()).toList();
    }

    private static int max(List<ClassMetrics> metrics, java.util.function.ToIntFunction<ClassMetrics> getter) {
        return metrics.stream().mapToInt(getter).max().orElse(0);
    }

    private static double relative(int value, int max) { return max == 0 ? 0 : (double) value / max; }
    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
    private static String band(double score) { return score < 30 ? "GREEN" : score <= 70 ? "YELLOW" : "RED"; }
}
