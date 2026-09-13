package io.github.gaalbu.heatcode.core;

import org.eclipse.jgit.api.Git;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class ChurnCalculator {
    public Churn calculate(Path repositoryRoot, Path file, Instant now) {
        try (Git git = Git.open(repositoryRoot.toFile())) {
            String relative = repositoryRoot.relativize(file).toString().replace('\\', '/');
            var commits = git.log().addPath(relative).call();
            Instant day30 = now.minus(30, ChronoUnit.DAYS);
            Instant day90 = now.minus(90, ChronoUnit.DAYS);
            int churn30 = 0;
            int churn90 = 0;
            for (var commit : commits) {
                Instant authored = commit.getAuthorIdent().getWhen().toInstant();
                if (authored.isAfter(day90)) {
                    churn90++;
                    if (authored.isAfter(day30)) churn30++;
                }
            }
            return new Churn(churn30, churn90);
        } catch (Exception ignored) {
            return new Churn(0, 0);
        }
    }

    public record Churn(int last30Days, int last90Days) { }
}
