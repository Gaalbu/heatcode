package io.github.gaalbu.heatcode.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaSourceParserTest {
    @Test
    void readsOneClassAndCountsItsLinesOfCode() {
        String source = "package demo;\n\npublic class Example {\n  int value;\n}\n";

        ClassMetrics result = new JavaSourceParser().parse(source).getFirst();

        assertEquals("demo.Example", result.className());
        assertEquals(4, result.linesOfCode());
    }
}
