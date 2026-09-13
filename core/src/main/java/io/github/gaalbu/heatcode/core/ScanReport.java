package io.github.gaalbu.heatcode.core;

import java.time.Instant;
import java.util.List;

public record ScanReport(
        String schemaVersion,
        String heatcodeVersion,
        String gitCommit,
        Instant scannedAt,
        WeightConfig weights,
        List<HeatScore> classes) {
}
