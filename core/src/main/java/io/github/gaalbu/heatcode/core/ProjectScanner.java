package io.github.gaalbu.heatcode.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ProjectScanner {
    private static final Set<String> IGNORED = Set.of("build", "target", ".gradle", ".git");
    private final HeatScoreCalculator calculator = new HeatScoreCalculator();
    private final DuplicationAnalyzer duplicationAnalyzer = new DuplicationAnalyzer();
    private final ChurnCalculator churnCalculator = new ChurnCalculator();
    private final Consumer<Path> parseObserver;
    private final Map<Path, List<ParsedClass>> parsedByFile = new HashMap<>();
    private Path indexedProject;

    public ProjectScanner() { this(path -> { }); }
    ProjectScanner(Consumer<Path> parseObserver) { this.parseObserver = parseObserver; }

    public synchronized ScanReport scan(Path project) throws IOException {
        indexedProject = project.toAbsolutePath().normalize();
        parsedByFile.clear();
        try (var paths = Files.walk(indexedProject)) {
            for (Path file : paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !hasIgnoredSegment(path)).toList()) parseFile(file);
        }
        return buildReport(project, Instant.now(), Map.of());
    }

    /** Re-parses only the changed Java file; other ASTs remain in the project index. */
    public synchronized ScanReport update(Path project, Path changedFile, ScanReport previous) throws IOException {
        Path normalized = changedFile.toAbsolutePath().normalize();
        if (indexedProject == null || !indexedProject.equals(project.toAbsolutePath().normalize())) return scan(project);
        if (Files.isRegularFile(normalized) && normalized.toString().endsWith(".java")) parseFile(normalized);
        else parsedByFile.remove(normalized);
        return buildReport(project, Instant.now(), previous == null ? Map.of() : churnByClass(previous));
    }

    private void parseFile(Path file) throws IOException {
        parseObserver.accept(file);
        String source = Files.readString(file);
        CompilationUnit unit = StaticJavaParser.parse(source);
        List<ParsedClass> classes = new ArrayList<>();
        for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
            if (type.isNestedType()) continue;
            classes.add(new ParsedClass(type.getFullyQualifiedName().orElse(type.getNameAsString()), file, type, source));
        }
        parsedByFile.put(file, classes);
    }

    private ScanReport buildReport(Path project, Instant now, Map<String, ChurnCalculator.Churn> cachedChurn) throws IOException {
        List<ParsedClass> parsed = parsedByFile.values().stream().flatMap(List::stream).toList();
        Set<String> projectTypes = parsed.stream().map(item -> item.type().getNameAsString()).collect(java.util.stream.Collectors.toSet());
        List<DuplicationAnalyzer.MethodOwner> methods = parsed.stream().flatMap(item -> item.type().getMethods().stream()
                .map(method -> new DuplicationAnalyzer.MethodOwner(item.name(), method))).toList();
        var duplication = duplicationAnalyzer.ratios(methods);
        List<ClassMetrics> metrics = new ArrayList<>();
        for (ParsedClass item : parsed) {
            int decisions = item.type().findAll(Node.class).stream().mapToInt(ProjectScanner::decisionContribution).sum();
            int coupling = (int) projectTypes.stream().filter(type -> !type.equals(item.type().getNameAsString())
                    && item.source().matches("(?s).*\\b" + java.util.regex.Pattern.quote(type) + "\\b.*")).count();
            int lines = (int) item.source().lines().filter(line -> !line.trim().isEmpty()).count();
            ChurnCalculator.Churn churn = cachedChurn.get(item.name());
            if (churn == null) churn = churnCalculator.calculate(project, item.file(), now);
            metrics.add(new ClassMetrics(item.name(), 1 + decisions, duplication.getOrDefault(item.name(), 0.0), coupling,
                    churn.last30Days(), churn.last90Days(), lines));
        }
        WeightConfig weights = WeightConfigLoader.load(project);
        if (metrics.stream().noneMatch(metric -> metric.churn30d() > 0 || metric.churn90d() > 0)) weights = weights.withoutChurn();
        return new ScanReport("1", "0.1.0", currentCommit(project), now, weights, calculator.calculate(metrics, weights));
    }

    private static Map<String, ChurnCalculator.Churn> churnByClass(ScanReport report) {
        Map<String, ChurnCalculator.Churn> result = new HashMap<>();
        for (HeatScore score : report.classes()) result.put(score.className(), new ChurnCalculator.Churn(score.metrics().churn30d(), score.metrics().churn90d()));
        return result;
    }

    private static int decisionContribution(Node node) {
        if (node instanceof IfStmt || node instanceof ForStmt || node instanceof ForEachStmt || node instanceof WhileStmt
                || node instanceof CatchClause || node instanceof SwitchEntry) return 1;
        if (node instanceof BinaryExpr binary && (binary.getOperator() == BinaryExpr.Operator.AND || binary.getOperator() == BinaryExpr.Operator.OR)) return 1;
        return 0;
    }

    private static boolean hasIgnoredSegment(Path path) {
        for (Path part : path) if (IGNORED.contains(part.toString())) return true;
        return false;
    }

    private static String currentCommit(Path project) {
        try (Repository repository = new FileRepositoryBuilder().findGitDir(project.toFile()).build()) {
            var head = repository.resolve("HEAD");
            return head == null ? null : head.name();
        } catch (Exception ignored) { return null; }
    }

    private record ParsedClass(String name, Path file, ClassOrInterfaceDeclaration type, String source) { }
}
