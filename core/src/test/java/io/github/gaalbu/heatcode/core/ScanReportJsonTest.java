package io.github.gaalbu.heatcode.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanReportJsonTest {
    @Test
    void roundTripsTheReportContract() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ClassMetrics metrics = new ClassMetrics("Example", 3, 0.5, 2, 1, 2, 20);
        ScanReport original = new ScanReport("1", "0.1.0", "abc", Instant.parse("2026-01-01T00:00:00Z"),
                WeightConfig.defaults(), List.of(new HeatScore("Example", 55, "YELLOW", metrics)));

        ScanReport restored = mapper.readValue(mapper.writeValueAsString(original), ScanReport.class);

        assertEquals(original, restored);
    }
}
