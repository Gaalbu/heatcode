package io.github.gaalbu.heatcode.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.util.ArrayList;
import java.util.List;

public final class JavaSourceParser {
    static { StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17); }

    public List<ClassMetrics> parse(String source) {
        CompilationUnit unit = StaticJavaParser.parse(source);
        int lines = (int) source.lines().filter(line -> !line.trim().isEmpty()).count();
        List<ClassMetrics> result = new ArrayList<>();
        unit.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(type -> !type.isNestedType())
                .forEach(type -> result.add(new ClassMetrics(
                        type.getFullyQualifiedName().orElse(type.getNameAsString()), 1, 0, 0, 0, 0, lines)));
        return result;
    }
}
