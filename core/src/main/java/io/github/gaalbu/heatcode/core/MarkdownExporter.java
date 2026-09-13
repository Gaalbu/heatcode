package io.github.gaalbu.heatcode.core;

public final class MarkdownExporter {
    public String export(ScanReport report) {
        StringBuilder output = new StringBuilder("# HeatCode report\n\n");
        output.append("- HeatCode version: ").append(report.heatcodeVersion()).append('\n');
        output.append("- Schema: ").append(report.schemaVersion()).append('\n');
        output.append("- Git commit: ").append(report.gitCommit() == null ? "none" : report.gitCommit()).append('\n');
        output.append("- Scanned at: ").append(report.scannedAt()).append("\n\n");
        output.append("| Class | Score | Band | Complexity | Duplication | Coupling | Churn 30d | Churn 90d | LOC |\n");
        output.append("|---|---:|---|---:|---:|---:|---:|---:|---:|\n");
        for (HeatScore score : report.classes()) {
            ClassMetrics m = score.metrics();
            output.append('|').append(escape(score.className())).append('|')
                    .append("%.2f|%s|%d|%.2f|%d|%d|%d|%d|%n".formatted(score.score(), score.band(),
                            m.cyclomaticComplexity(), m.duplicationRatio(), m.couplingOut(), m.churn30d(), m.churn90d(), m.linesOfCode()));
        }
        return output.toString();
    }

    private static String escape(String text) { return text.replace("|", "\\|"); }
}
