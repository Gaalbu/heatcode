package io.github.gaalbu.heatcode.cli;

import io.github.gaalbu.heatcode.core.ClassMetrics;
import io.github.gaalbu.heatcode.core.HeatScore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TreemapLayoutTest {
    @Test
    void createsPositiveNonOverlappingBlocksInsideTheTerminal() {
        var scores = List.of(
                score("A", 40, 30), score("B", 60, 20), score("C", 20, 10));

        var blocks = new TreemapLayout().layout(scores, 80, 24);

        assertTrue(blocks.stream().allMatch(block -> block.width() > 0 && block.height() > 0
                && block.x() >= 0 && block.y() >= 0 && block.x() + block.width() <= 80 && block.y() + block.height() <= 24));
        for (int i = 0; i < blocks.size(); i++) for (int j = i + 1; j < blocks.size(); j++) {
            var a = blocks.get(i); var b = blocks.get(j);
            assertTrue(a.x() + a.width() <= b.x() || b.x() + b.width() <= a.x()
                    || a.y() + a.height() <= b.y() || b.y() + b.height() <= a.y());
        }
    }

    private static HeatScore score(String name, double value, int lines) {
        return new HeatScore(name, value, "YELLOW", new ClassMetrics(name, 2, 0, 0, 0, 0, lines));
    }
}
