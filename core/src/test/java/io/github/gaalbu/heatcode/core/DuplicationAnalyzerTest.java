package io.github.gaalbu.heatcode.core;

import com.github.javaparser.StaticJavaParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DuplicationAnalyzerTest {
    @Test
    void marksMethodsWithEquivalentBodiesAfterLocalVariableRenaming() {
        var unit = StaticJavaParser.parse("""
                class Example {
                    int first(int input) {
                        int result = input + 1;
                        result += 2;
                        result += 3;
                        result += 4;
                        result += 5;
                        result += 6;
                        result += 7;
                        result += 8;
                        result += 9;
                        return result;
                    }
                    int second(int value) {
                        int answer = value + 1;
                        answer += 2;
                        answer += 3;
                        answer += 4;
                        answer += 5;
                        answer += 6;
                        answer += 7;
                        answer += 8;
                        answer += 9;
                        return answer;
                    }
                }
                """);

        var methods = unit.findAll(com.github.javaparser.ast.body.MethodDeclaration.class).stream()
                .map(method -> new DuplicationAnalyzer.MethodOwner("Example", method)).toList();

        assertEquals(1.0, new DuplicationAnalyzer().ratios(methods).get("Example"), 0.0001);
    }
}
