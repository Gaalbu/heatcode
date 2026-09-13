# HeatCode

HeatCode maps technical debt in Java projects as a navigable heat map. The MVP scans Java sources and emits a stable JSON report or a readable terminal table.

## Quick start

```bash
./gradlew build
./gradlew :cli:run --args="scan . --format table"
./gradlew :cli:run --args="scan . --format json"
```

The project uses Java 21, Gradle 8.10.2, JavaParser, JGit, Picocli and Lanterna. Source code is split into `core` (analysis and contracts) and `cli` (presentation).

## HeatScore

Each metric is normalized relative to the scanned project and combined as:

`score = 100 * (0.40 * complexity + 0.25 * duplication + 0.20 * coupling + 0.15 * churn)`

Bands are GREEN below 30, YELLOW from 30 through 70, and RED above 70. When no Git history is available, the churn weight is redistributed proportionally among the other metrics.

The JSON report is the source of truth and carries a schema version, scanner version, commit (when available), timestamp, weights and class scores.

## License

MIT.
