package io.github.gaalbu.heatcode.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gaalbu.heatcode.core.HeatScore;
import io.github.gaalbu.heatcode.core.ProjectScanner;
import io.github.gaalbu.heatcode.core.ScanReport;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "heatcode", mixinStandardHelpOptions = true, version = "0.1.0",
        description = "Maps technical debt heat in Java projects.", subcommands = HeatCodeApplication.ScanCommand.class)
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

        @Override public Integer call() throws Exception {
            ScanReport report = new ProjectScanner().scan(project);
            if ("json".equalsIgnoreCase(format)) {
                ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                        .enable(SerializationFeature.INDENT_OUTPUT);
                System.out.println(mapper.writeValueAsString(report));
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
}
