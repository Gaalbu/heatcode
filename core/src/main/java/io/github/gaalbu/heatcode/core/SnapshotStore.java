package io.github.gaalbu.heatcode.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SnapshotStore {
    public void save(ScanReport report, Path destination) throws IOException {
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(destination, ReportCodec.mapper().writeValueAsString(report));
    }

    public ScanReport load(Path source) throws IOException {
        return ReportCodec.mapper().readValue(Files.readString(source), ScanReport.class);
    }
}
