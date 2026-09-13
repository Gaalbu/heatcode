package io.github.gaalbu.heatcode.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gaalbu.heatcode.core.ProjectScanner;
import io.github.gaalbu.heatcode.core.ReportCodec;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanJsonGoldenTest {
    @Test
    void matchesTheStableJsonContractGoldenFile() throws Exception {
        Path fixture = Files.createTempDirectory("heatcode-golden-");
        Files.copy(Path.of(getClass().getResource("/fixture/Sample.java").toURI()), fixture.resolve("Sample.java"));
        JsonNode actual = ReportCodec.mapper().readTree(ReportCodec.mapper().writeValueAsString(new ProjectScanner().scan(fixture)));
        ((ObjectNode) actual).remove("scannedAt");
        JsonNode expected = new ObjectMapper().readTree(getClass().getResourceAsStream("/golden/scan.json"));

        assertEquals(expected, actual);
    }
}
