package io.github.gaalbu.heatcode.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProjectScanner {
    private static final Set<String> IGNORED = Set.of("build", "target", ".gradle", ".git");
    private final HeatScoreCalculator calculator = new HeatScoreCalculator();

    public ScanReport scan(Path project) throws IOException {
        List<Path> files;
        try (var paths = Files.walk(project)) {
            files = paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.getNameCount() == 0 || path.getNameCount() > 0 && !hasIgnoredSegment(path)).toList();
        }
        List<ClassMetrics> metrics = new ArrayList<>();
        Set<String> projectTypes = new HashSet<>();
        List<ParsedClass> parsed = new ArrayList<>();
        for (Path file : files) {
            String source = Files.readString(file);
            CompilationUnit unit = StaticJavaParser.parse(source);
            for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                if (type.isNestedType()) continue;
                String name = type.getFullyQualifiedName().orElse(type.getNameAsString());
                projectTypes.add(type.getNameAsString());
                parsed.add(new ParsedClass(name, type, source));
            }
        }
        for (ParsedClass item : parsed) {
            int decisions = item.type().findAll(Node.class).stream().mapToInt(node ->
                    node instanceof IfStmt || node instanceof ForStmt || node instanceof ForEachStmt
                            || node instanceof WhileStmt || node instanceof CatchClause || node instanceof SwitchEntry ? 1
                            : node instanceof BinaryExpr binary && (binary.getOperator() == BinaryExpr.Operator.AND
                            || binary.getOperator() == BinaryExpr.Operator.OR) ? 1 : 0).sum();
            int coupling = (int) projectTypes.stream().filter(type -> !type.equals(item.type().getNameAsString())
                    && item.source().matches("(?s).*\\b" + java.util.regex.Pattern.quote(type) + "\\b.*")).count();
            int lines = (int) item.source().lines().filter(line -> !line.trim().isEmpty()).count();
            metrics.add(new ClassMetrics(item.name(), 1 + decisions, 0, coupling, 0, 0, lines));
        }
        String commit = currentCommit(project);
        return new ScanReport("1", "0.1.0", commit, Instant.now(), WeightConfig.defaults(), calculator.calculate(metrics, WeightConfig.defaults()));
    }

    private static boolean hasIgnoredSegment(Path path) {
        for (Path part : path) if (IGNORED.contains(part.toString())) return true;
        return false;
    }

    private static String currentCommit(Path project) {
        try (Repository repository = new FileRepositoryBuilder().findGitDir(project.toFile()).build()) {
            return repository.getObjectDatabase().exists() ? repository.resolve("HEAD").name() : null;
        } catch (Exception ignored) { return null; }
    }

    private record ParsedClass(String name, ClassOrInterfaceDeclaration type, String source) { }
}
