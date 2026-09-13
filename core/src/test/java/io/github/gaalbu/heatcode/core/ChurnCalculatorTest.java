package io.github.gaalbu.heatcode.core;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChurnCalculatorTest {
    @Test
    void countsCommitsInTheConfiguredWindowsUsingRealGit() throws Exception {
        Path root = Files.createTempDirectory("heatcode-git-");
        Path source = root.resolve("Example.java");
        Files.writeString(source, "class Example {}\n");
        Instant now = Instant.parse("2026-09-13T12:00:00Z");
        try (Git git = Git.init().setDirectory(root.toFile()).call()) {
            git.add().addFilepattern("Example.java").call();
            PersonIdent person = new PersonIdent("HeatCode", "test@heatcode.local", Date.from(now.minusSeconds(3600)), java.util.TimeZone.getTimeZone("UTC"));
            git.commit().setMessage("initial").setAuthor(person).setCommitter(person).call();

            var churn = new ChurnCalculator().calculate(root, source, now);

            assertEquals(1, churn.last30Days());
            assertEquals(1, churn.last90Days());
        }
    }
}
