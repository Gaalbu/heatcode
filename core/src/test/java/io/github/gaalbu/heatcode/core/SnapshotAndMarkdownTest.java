package io.github.gaalbu.heatcode.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotAndMarkdownTest {
    @Test
    void savesLoadsAndExportsTheReport() throws Exception {
        var report = new ScanReport("1", "0.1.0", null, Instant.parse("2026-01-01T00:00:00Z"), WeightConfig.defaults(),
                List.of(new HeatScore("Example", 80, "RED", new ClassMetrics("Example", 4, 0.2, 1, 0, 0, 20))));
        var path = Files.createTempFile("heatcode", ".json");

        new SnapshotStore().save(report, path);

        assertEquals(report, new SnapshotStore().load(path));
        assertTrue(new MarkdownExporter().export(report).contains("|Example|80.00|RED|"));
    }
}
