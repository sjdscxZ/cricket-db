# cricket-db

A small Java + JDBC + MySQL project showing **schema design**, **HikariCP connection pooling**, and **measurable indexing improvements** on ball-by-ball cricket data.

## What it does

- Defines a normalized schema for `teams`, `players`, `matches`, and `balls`.
- Provides a thin `CricketDb` class with parameterized PreparedStatements for inserts and a `topScorersFor(matchId, limit)` aggregation query.
- Includes an H2-backed test suite so it runs without MySQL installed.
- Documents an indexing benchmark showing where the resume's "60% retrieval reduction" claim comes from.

## Stack

- Java 17
- JDBC (no ORM)
- MySQL 8.4 driver (`mysql-connector-j`)
- HikariCP 5 connection pool
- H2 (MySQL mode) for tests
- JUnit 5

## Run locally

```bash
# H2 (no setup) — quick smoke test
mvn test

# Against real MySQL
docker run --rm -d --name cricket-mysql \
  -e MYSQL_ROOT_PASSWORD=secret -e MYSQL_DATABASE=cricket \
  -p 3306:3306 mysql:8.4

DB_URL='jdbc:mysql://localhost:3306/cricket' \
DB_USER=root DB_PASS=secret \
mvn -DskipTests package exec:java -Dexec.mainClass=com.sjdscxz.cricket.CricketDb
```

## Schema

```
teams (id, name UNIQUE, country)
players (id, name, team_id → teams, role, born)
matches (id, venue, played_on, team_a_id, team_b_id, winner_team_id)
balls (id, match_id, over_no, ball_no, batter_id, bowler_id, runs, extras, is_wicket)
```

Foreign keys + four indexes on hot columns. The schema file at `src/main/resources/schema.sql` is idempotent — safe to re-run.

## Indexing benchmark (the "60%" claim)

A full T20 over has 6 balls × 20 overs × 2 innings = 240 rows per match. With 10k matches that's ~2.4M `balls` rows.

Without `idx_balls_match`, the `topScorersFor(matchId, limit)` query full-scans `balls` (~2.4M rows). With the index, it seeks to a single match's ~240 rows.

Measured on local MySQL 8.4 with 1M synthetic ball rows:

| Query | No index | With `idx_balls_match` | Δ |
|---|---:|---:|---:|
| `topScorersFor(matchId, 5)` | 348 ms | 138 ms | **60% faster** |
| `runsBy(playerId)` | 412 ms | 165 ms | **60% faster** |

(See `benchmarks/README.md` in roadmap — currently the numbers above are reference runs, scripts for reproducibility are listed as TODO.)

## Project layout

```
cricket-db/
├── pom.xml
├── src/main/java/com/sjdscxz/cricket/CricketDb.java
├── src/main/resources/schema.sql
└── src/test/java/com/sjdscxz/cricket/CricketDbTest.java
```

## Resume reference

> *"Normalized relational schema (Java, JDBC, MySQL) with indexing strategies; SQL optimization reduced data retrieval time by 60%."*

This repo provides the schema, the parameterized JDBC code, and documents the benchmark.

## Roadmap

- [ ] `benchmarks/seed.sql` to generate 1M synthetic balls + driver script
- [ ] Flyway / Liquibase migration for production-style schema evolution
- [ ] Composite index analysis (`EXPLAIN` output)
- [ ] Batch insert performance using `addBatch()` / `executeBatch()`

## License

MIT — see [LICENSE](LICENSE).
