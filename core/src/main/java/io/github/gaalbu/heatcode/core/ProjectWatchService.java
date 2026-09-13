package io.github.gaalbu.heatcode.core;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

public final class ProjectWatchService implements AutoCloseable {
    private final WatchService watchService;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final Map<WatchKey, Path> keys = new ConcurrentHashMap<>();
    private final Path root;
    private final Consumer<Path> onJavaFileChanged;
    private ScheduledFuture<?> pending;
    private volatile boolean running;

    public ProjectWatchService(Path root, Consumer<Path> onJavaFileChanged) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        this.onJavaFileChanged = onJavaFileChanged;
        this.watchService = FileSystems.getDefault().newWatchService();
        registerDirectories(this.root);
    }

    public void start() {
        running = true;
        executor.submit(this::poll);
    }

    private void poll() {
        while (running) {
            try {
                WatchKey key = watchService.take();
                Path directory = keys.get(key);
                if (directory == null) continue;
                for (var event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                    Path changed = directory.resolve((Path) event.context());
                    if (Files.isDirectory(changed) && event.kind() == StandardWatchEventKinds.ENTRY_CREATE) registerDirectories(changed);
                    if (changed.toString().endsWith(".java")) debounce(changed);
                }
                if (!key.reset()) keys.remove(key);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException exception) {
                return;
            }
        }
    }

    private synchronized void debounce(Path changed) {
        if (pending != null) pending.cancel(false);
        pending = executor.schedule(() -> onJavaFileChanged.accept(changed), 300, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void registerDirectories(Path directory) throws IOException {
        if (!Files.isDirectory(directory) || isIgnored(directory)) return;
        try (var paths = Files.walk(directory, 20)) {
            for (Path path : paths.filter(Files::isDirectory).filter(path -> !isIgnored(path)).toList()) {
                keys.put(path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE), path);
            }
        }
    }

    private static boolean isIgnored(Path path) {
        return path.getFileName() != null && java.util.Set.of(".git", ".gradle", "build", "target").contains(path.getFileName().toString());
    }

    @Override public void close() throws IOException {
        running = false;
        if (pending != null) pending.cancel(false);
        executor.shutdownNow();
        watchService.close();
    }
}
