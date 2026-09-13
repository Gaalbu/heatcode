package io.github.gaalbu.heatcode.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectWatchServiceTest {
    @Test
    void debouncesRapidJavaChanges() throws Exception {
        var root = Files.createTempDirectory("heatcode-watch-");
        var source = root.resolve("Example.java");
        Files.writeString(source, "class Example {}\n");
        var calls = new AtomicInteger();
        var observed = new CountDownLatch(1);
        try (var watcher = new ProjectWatchService(root, changed -> { calls.incrementAndGet(); observed.countDown(); })) {
            watcher.start();
            Thread.sleep(100);
            Files.writeString(source, "class Example { int a; }\n");
            Files.writeString(source, "class Example { int b; }\n");
            assertTrue(observed.await(2, TimeUnit.SECONDS));
            Thread.sleep(450);
            assertEquals(1, calls.get());
        }
    }
}
