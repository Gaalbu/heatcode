package io.github.gaalbu.heatcode.cli;

import io.github.gaalbu.heatcode.core.HeatScore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TreemapLayout {
    public List<Block> layout(List<HeatScore> scores, int width, int height) {
        if (width <= 0 || height <= 0 || scores.isEmpty()) return List.of();
        double totalWeight = scores.stream().mapToDouble(score -> Math.max(1, score.metrics().linesOfCode())).sum();
        double scale = width * (double) height / totalWeight;
        List<Item> remaining = scores.stream().sorted(Comparator.comparingDouble((HeatScore score) -> score.metrics().linesOfCode()).reversed())
                .map(score -> new Item(score, Math.max(1, score.metrics().linesOfCode()) * scale)).toList();
        List<Block> result = new ArrayList<>();
        int x = 0, y = 0, w = width, h = height;
        int index = 0;
        while (index < remaining.size() && w > 0 && h > 0) {
            List<Item> row = new ArrayList<>();
            double side = Math.min(w, h);
            while (index < remaining.size()) {
                Item candidate = remaining.get(index);
                double current = worst(row, side);
                row.add(candidate);
                if (row.size() > 1 && worst(row, side) > current) {
                    row.removeLast();
                    break;
                }
                index++;
            }
            double rowArea = row.stream().mapToDouble(Item::area).sum();
            boolean horizontal = w >= h;
            if (horizontal) {
                int rowHeight = Math.max(1, Math.min(h, (int) Math.round(rowArea / w)));
                int cursor = x;
                for (int i = 0; i < row.size(); i++) {
                    int blockWidth = i == row.size() - 1 ? x + w - cursor
                            : Math.max(1, Math.min(w - (cursor - x), (int) Math.round(row.get(i).area() / rowHeight)));
                    result.add(new Block(row.get(i).score(), cursor, y, blockWidth, rowHeight));
                    cursor += blockWidth;
                }
                y += rowHeight; h -= rowHeight;
            } else {
                int rowWidth = Math.max(1, Math.min(w, (int) Math.round(rowArea / h)));
                int cursor = y;
                for (int i = 0; i < row.size(); i++) {
                    int blockHeight = i == row.size() - 1 ? y + h - cursor
                            : Math.max(1, Math.min(h - (cursor - y), (int) Math.round(row.get(i).area() / rowWidth)));
                    result.add(new Block(row.get(i).score(), x, cursor, rowWidth, blockHeight));
                    cursor += blockHeight;
                }
                x += rowWidth; w -= rowWidth;
            }
        }
        return result;
    }

    private static double worst(List<Item> row, double side) {
        if (row.isEmpty()) return Double.POSITIVE_INFINITY;
        double sum = row.stream().mapToDouble(Item::area).sum();
        double max = row.stream().mapToDouble(Item::area).max().orElse(0);
        double min = row.stream().mapToDouble(Item::area).min().orElse(1);
        return Math.max(side * side * max / (sum * sum), (sum * sum) / (side * side * min));
    }

    public record Block(HeatScore score, int x, int y, int width, int height) { }
    private record Item(HeatScore score, double area) { }
}
