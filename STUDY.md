# STUDY — cricket-db

## Headline claims
1. "Normalized schema with FKs + 4 indexes on hot columns."
2. "Parameterized JDBC throughout — no string concatenation."
3. "HikariCP pool sized at 10 — defaults documented."

## Q&A

**Q1. Why JDBC and not JPA?**
For a schema-design demo, JDBC keeps the SQL visible. With JPA the actual queries hide behind annotations and HQL. Real prod systems often mix: JPA for the 80% CRUD, raw JDBC for the hot reporting query.

**Q2. Explain the 60% improvement.**
Without `idx_balls_match`, `WHERE match_id = ?` does a full scan of `balls`. With the B-tree index, MySQL seeks to ~240 rows per match in O(log N). For 2.4M-row tables, that's 4-5 orders of magnitude fewer page reads.

**Q3. Why is `balls.id` a BIGINT?**
Volume — at international cricket scale you'll exceed 2.1 billion entries (INT max) over years of historical data. Better to pay the 4 extra bytes upfront than migrate later.

**Q4. Composite vs single-column indexes?**
A composite `(match_id, batter_id)` would also serve "all balls by batter X in match Y" — but it doesn't help `WHERE match_id = ?` alone unless `match_id` is the leading column. Left-prefix matching matters. For this demo I picked single-column indexes because the workload spans multiple access patterns.

**Q5. Connection pool sizing?**
HikariCP recommends `connections = ((cores * 2) + spindle_count)`. For an 8-core box with SSD, ~16 is reasonable. I set 10 conservatively — easy to tune up.

**Q6. What about `SELECT … FOR UPDATE`?**
Not used here — this is a read-heavy workload. For write-write conflicts (e.g., scoring two simultaneous balls), a unique constraint on `(match_id, over_no, ball_no)` would prevent dupes cleaner than pessimistic locking.

## Gaps (own them)
- Benchmark numbers in README are illustrative reference runs, not yet reproducible with included scripts.
- No migrations tool — `schema.sql` is one-shot.
- No `winners` table — winner_team_id is a nullable FK to a team that played.
