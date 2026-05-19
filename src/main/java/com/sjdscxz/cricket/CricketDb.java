package com.sjdscxz.cricket;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class CricketDb {

    private final DataSource ds;

    public CricketDb(String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setPoolName("cricket-pool");
        this.ds = new HikariDataSource(cfg);
    }

    public DataSource dataSource() { return ds; }

    public void initSchema() {
        String ddl;
        try (var in = CricketDb.class.getResourceAsStream("/schema.sql")) {
            ddl = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new IllegalStateException("schema.sql missing on classpath", e);
        }
        try (var conn = ds.getConnection(); var st = conn.createStatement()) {
            for (String stmt : ddl.split(";")) {
                String s = stmt.trim();
                if (!s.isEmpty()) st.execute(s);
            }
        } catch (SQLException e) {
            throw new RuntimeException("schema init failed", e);
        }
    }

    public int insertTeam(String name, String country) {
        String sql = "INSERT INTO teams(name, country) VALUES(?, ?)";
        try (var c = ds.getConnection();
             var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, country);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int insertPlayer(String name, int teamId, String role) {
        String sql = "INSERT INTO players(name, team_id, role) VALUES(?, ?, ?)";
        try (var c = ds.getConnection();
             var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setInt(2, teamId); ps.setString(3, role);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public record PlayerScore(int playerId, String name, int runs, int balls) {}

    /** Top scorers in a given match. */
    public List<PlayerScore> topScorersFor(int matchId, int limit) {
        String sql = """
                SELECT p.id, p.name,
                       SUM(b.runs) AS total_runs,
                       COUNT(*)    AS balls_faced
                  FROM balls b
                  JOIN players p ON p.id = b.batter_id
                 WHERE b.match_id = ?
              GROUP BY p.id, p.name
              ORDER BY total_runs DESC
                 LIMIT ?
                """;
        List<PlayerScore> out = new ArrayList<>();
        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, limit);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new PlayerScore(
                            rs.getInt(1), rs.getString(2),
                            rs.getInt(3), rs.getInt(4)));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    public int recordBall(int matchId, int over, int ballNo, int batterId, int bowlerId,
                          int runs, int extras, boolean wicket) {
        String sql = """
                INSERT INTO balls(match_id, over_no, ball_no, batter_id, bowler_id, runs, extras, is_wicket)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var c = ds.getConnection();
             var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, matchId); ps.setInt(2, over); ps.setInt(3, ballNo);
            ps.setInt(4, batterId); ps.setInt(5, bowlerId);
            ps.setInt(6, runs); ps.setInt(7, extras); ps.setBoolean(8, wicket);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int recordMatch(String venue, java.sql.Date day, int teamA, int teamB) {
        String sql = "INSERT INTO matches(venue, played_on, team_a_id, team_b_id) VALUES(?, ?, ?, ?)";
        try (var c = ds.getConnection();
             var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, venue); ps.setDate(2, day);
            ps.setInt(3, teamA); ps.setInt(4, teamB);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public static void main(String[] args) {
        String url  = System.getenv().getOrDefault("DB_URL",  "jdbc:mysql://localhost:3306/cricket");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String pass = System.getenv().getOrDefault("DB_PASS", "");
        var db = new CricketDb(url, user, pass);
        db.initSchema();
        System.out.println("[cricket-db] schema applied to " + url);
    }
}
