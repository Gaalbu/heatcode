package io.github.gaalbu.heatcode.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectScannerIncrementalTest {
    @Test
    void updateReparsesOnlyTheChangedFile() throws Exception {
        var root = Files.createTempDirectory("heatcode-incremental-");
        var first = root.resolve("First.java");
        var second = root.resolve("Second.java");
        Files.writeString(first, "class First { int value() { return 1; } }\n");
        Files.writeString(second, "class Second { int value() { return 2; } }\n");
        var parsed = new ArrayList<java.nio.file.Path>();
        var scanner = new ProjectScanner(parsed::add);

        var initial = scanner.scan(root);
        Files.writeString(first, "class First { int value() { return 3; } }\n");
        var updated = scanner.update(root, first, initial);

        assertEquals(3, parsed.size());
        assertEquals(first.toAbsolutePath(), parsed.get(2));
        assertEquals(2, updated.classes().size());
    }
}
