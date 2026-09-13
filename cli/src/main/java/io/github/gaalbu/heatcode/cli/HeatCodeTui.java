package io.github.gaalbu.heatcode.cli;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import io.github.gaalbu.heatcode.core.HeatScore;
import io.github.gaalbu.heatcode.core.ProjectScanner;
import io.github.gaalbu.heatcode.core.ProjectWatchService;
import io.github.gaalbu.heatcode.core.ScanReport;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class HeatCodeTui {
    public void open(Path project, boolean watch, boolean light, boolean noColor) throws Exception {
        ProjectScanner scanner = new ProjectScanner();
        AtomicReference<ScanReport> report = new AtomicReference<>(scanner.scan(project));
        try (var terminal = new DefaultTerminalFactory().createTerminal(); Screen screen = new TerminalScreen(terminal)) {
            screen.startScreen();
            ProjectWatchService watcher = watch ? new ProjectWatchService(project, changed -> {
                try { report.set(scanner.update(project, changed, report.get())); } catch (Exception ignored) { }
            }) : null;
            if (watcher != null) watcher.start();
            try {
                int selected = 0;
                boolean details = false;
                while (true) {
                    List<HeatScore> scores = report.get().classes();
                    selected = Math.max(0, Math.min(selected, Math.max(0, scores.size() - 1)));
                    render(screen, scores, selected, watch, light, noColor, details);
                    KeyStroke key = screen.readInput();
                    if (key.getKeyType() == KeyType.Escape || (key.getKeyType() == KeyType.Character && (key.getCharacter() == 'q' || key.getCharacter() == 'Q'))) break;
                    if (key.getKeyType() == KeyType.ArrowDown || (key.getKeyType() == KeyType.Character && key.getCharacter() == 'j')) selected++;
                    if (key.getKeyType() == KeyType.ArrowUp || (key.getKeyType() == KeyType.Character && key.getCharacter() == 'k')) selected--;
                    if (key.getKeyType() == KeyType.Enter) details = !details;
                }
            } finally { if (watcher != null) watcher.close(); }
        }
    }

    private void render(Screen screen, List<HeatScore> scores, int selected, boolean watch, boolean light,
                        boolean noColor, boolean details) throws Exception {
        screen.clear();
        TerminalSize size = screen.getTerminalSize();
        int footer = Math.min(details ? 5 : 3, size.getRows());
        var blocks = new TreemapLayout().layout(scores, size.getColumns(), Math.max(1, size.getRows() - footer));
        var graphics = screen.newTextGraphics();
        boolean listMode = blocks.stream().anyMatch(block -> block.width() < 2 || block.height() < 2);
        if (listMode) {
            graphics.setForegroundColor(light ? TextColor.ANSI.BLACK : TextColor.ANSI.WHITE);
            for (int i = 0; i < scores.size() && i < size.getRows() - footer; i++) {
                HeatScore score = scores.get(i);
                graphics.putString(0, i, (i == selected ? "> " : "  ") + symbol(score.band(), noColor) + " " + score.className()
                        + " " + String.format("%.1f", score.score()));
            }
        } else for (int i = 0; i < blocks.size(); i++) {
            var block = blocks.get(i);
            graphics.setBackgroundColor(color(block.score().band(), i == selected, light, noColor));
            graphics.fillRectangle(new TerminalPosition(block.x(), block.y()), new TerminalSize(block.width(), block.height()), ' ');
            graphics.setForegroundColor(light ? TextColor.ANSI.BLACK : TextColor.ANSI.WHITE);
            graphics.putString(block.x(), block.y(), shortName(symbol(block.score().band(), noColor) + " " + block.score().className(), block.width() - 1));
        }
        graphics.setBackgroundColor(light ? TextColor.ANSI.WHITE : TextColor.ANSI.BLACK);
        graphics.setForegroundColor(light ? TextColor.ANSI.BLACK : TextColor.ANSI.WHITE);
        graphics.fillRectangle(new TerminalPosition(0, size.getRows() - footer), new TerminalSize(size.getColumns(), footer), ' ');
        if (!scores.isEmpty()) {
            var current = scores.get(Math.min(selected, scores.size() - 1));
            graphics.putString(0, size.getRows() - footer, current.className() + " " + String.format("%.2f (%s)", current.score(), current.band()));
            if (details && footer >= 5) {
                var m = current.metrics();
                graphics.putString(0, size.getRows() - footer + 1, "complexity=" + m.cyclomaticComplexity() + " duplication=" + String.format("%.1f%%", m.duplicationRatio() * 100));
                graphics.putString(0, size.getRows() - footer + 2, "coupling=" + m.couplingOut() + " churn30d=" + m.churn30d() + " churn90d=" + m.churn90d() + " loc=" + m.linesOfCode());
            }
        }
        graphics.putString(0, size.getRows() - 2, (watch ? "watch on | " : "") + "j/k/arrows navigate | Enter details | q/Esc exit");
        screen.refresh();
    }

    private static TextColor color(String band, boolean selected, boolean light, boolean noColor) {
        if (noColor) return light ? TextColor.ANSI.WHITE : TextColor.ANSI.BLACK;
        if (selected) return light ? TextColor.ANSI.WHITE : TextColor.ANSI.WHITE;
        return switch (band) { case "RED" -> TextColor.ANSI.RED; case "YELLOW" -> TextColor.ANSI.YELLOW; default -> TextColor.ANSI.GREEN; };
    }

    private static String symbol(String band, boolean noColor) {
        if (!noColor) return "";
        return switch (band) { case "RED" -> "!"; case "YELLOW" -> "~"; default -> "."; };
    }

    private static String shortName(String name, int max) { return name.length() <= max ? name : name.substring(0, Math.max(1, max)); }
}
