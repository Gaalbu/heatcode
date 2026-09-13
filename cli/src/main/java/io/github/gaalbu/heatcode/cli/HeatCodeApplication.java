package io.github.gaalbu.heatcode.cli;

import io.github.gaalbu.heatcode.core.HeatScore;
import io.github.gaalbu.heatcode.core.ProjectScanner;
import io.github.gaalbu.heatcode.core.ReportCodec;
import io.github.gaalbu.heatcode.core.ReportDiff;
import io.github.gaalbu.heatcode.core.ScanReport;
import io.github.gaalbu.heatcode.core.SnapshotStore;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "heatcode", mixinStandardHelpOptions = true, version = "0.1.0",
        description = "Maps technical debt heat in Java projects.", subcommands = {HeatCodeApplication.ScanCommand.class, HeatCodeApplication.CompareCommand.class, HeatCodeApplication.TuiCommand.class})
public final class HeatCodeApplication implements Runnable {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new HeatCodeApplication()).execute(args);
        System.exit(exitCode);
    }

    @Override public void run() { new CommandLine(this).usage(System.out); }

    @Command(name = "scan", description = "Scans Java sources and prints a report.")
    static final class ScanCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Project path")
        Path project;

        @Option(names = "--format", defaultValue = "table", description = "Output: json or table")
        String format;

        @Option(names = "--save", description = "Save the JSON report as a snapshot")
        Path save;

        @Option(names = "--markdown", description = "Write a Markdown report to this path")
        Path markdown;

        @Override public Integer call() throws Exception {
            ScanReport report = new ProjectScanner().scan(project);
            if (save != null) new SnapshotStore().save(report, save);
            if (markdown != null) java.nio.file.Files.writeString(markdown, new io.github.gaalbu.heatcode.core.MarkdownExporter().export(report));
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(ReportCodec.mapper().writeValueAsString(report));
            } else if ("table".equalsIgnoreCase(format)) {
                System.out.printf("%-55s %8s %8s %s%n", "CLASS", "SCORE", "BAND", "MAIN METRICS");
                for (HeatScore score : report.classes()) {
                    var m = score.metrics();
                    System.out.printf("%-55s %8.2f %8s complexity=%d coupling=%d lines=%d%n",
                            score.className(), score.score(), score.band(), m.cyclomaticComplexity(), m.couplingOut(), m.linesOfCode());
                }
            } else {
                throw new CommandLine.ParameterException(new CommandLine(this), "Unsupported format: " + format);
            }
            return 0;
        }
    }

    @Command(name = "compare", description = "Compares two JSON snapshots and prints Markdown.")
    static final class CompareCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Previous snapshot") Path before;
        @Parameters(index = "1", description = "Current snapshot") Path after;

        @Option(names = "--output", description = "Write comparison to a file") Path output;

        @Override public Integer call() throws Exception {
            String result = new ReportDiff().toMarkdown(new SnapshotStore().load(before), new SnapshotStore().load(after));
            if (output == null) System.out.print(result);
            else java.nio.file.Files.writeString(output, result);
            return 0;
        }
    }

    @Command(name = "tui", description = "Opens the navigable terminal heat map.")
    static final class TuiCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Project path") Path project;
        @Option(names = "--watch", description = "Refresh after Java file changes") boolean watch;
        @Option(names = "--light", description = "Use a light terminal palette") boolean light;
        @Option(names = "--no-color", description = "Use symbols and high contrast instead of color") boolean noColor;

        @Override public Integer call() throws Exception {
            new HeatCodeTui().open(project, watch, light, noColor);
            return 0;
        }
    }
}
