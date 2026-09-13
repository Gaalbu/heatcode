package io.github.gaalbu.heatcode.core;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ReportDiff {
    public String toMarkdown(ScanReport before, ScanReport after) {
        Map<String, HeatScore> oldScores = before.classes().stream().collect(Collectors.toMap(HeatScore::className, Function.identity()));
        StringBuilder output = new StringBuilder("# HeatCode comparison\n\n| Class | Before | After | Delta |\n|---|---:|---:|---:|\n");
        for (HeatScore current : after.classes()) {
            double previous = oldScores.containsKey(current.className()) ? oldScores.get(current.className()).score() : 0;
            output.append('|').append(current.className()).append('|').append("%.2f|%.2f|%+.2f|%n".formatted(previous, current.score(), current.score() - previous));
        }
        return output.toString();
    }
}
