package com.sjdscxz.cricket;

import org.junit.jupiter.api.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CricketDbTest {

    static CricketDb db;
    static int teamA, teamB, batter, bowler, match;

    @BeforeAll
    static void boot() {
        db = new CricketDb(
                "jdbc:h2:mem:cricket;DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", "");
        db.initSchema();
    }

    @Test @Order(1)
    void canInsertTeamsAndPlayers() {
        teamA = db.insertTeam("Royals", "India");
        teamB = db.insertTeam("Strikers", "Australia");
        batter = db.insertPlayer("Player A", teamA, "BAT");
        bowler = db.insertPlayer("Player B", teamB, "BOWL");
        assertTrue(teamA > 0 && teamB > 0);
        assertTrue(batter > 0 && bowler > 0);
    }

    @Test @Order(2)
    void canRecordMatchAndBalls() {
        match = db.recordMatch("Chinnaswamy",
                Date.valueOf(LocalDate.of(2026, 5, 19)), teamA, teamB);
        for (int over = 0; over < 5; over++) {
            for (int b = 1; b <= 6; b++) {
                db.recordBall(match, over, b, batter, bowler, b % 7, 0, false);
            }
        }
        List<CricketDb.PlayerScore> top = db.topScorersFor(match, 1);
        assertEquals(1, top.size());
        assertEquals("Player A", top.get(0).name());
        assertTrue(top.get(0).runs() > 0);
        assertEquals(30, top.get(0).balls()); // 5 overs × 6 balls
    }

    @Test @Order(3)
    void topScorerLimitRespected() {
        List<CricketDb.PlayerScore> top = db.topScorersFor(match, 10);
        assertFalse(top.isEmpty());
        assertTrue(top.size() <= 10);
    }
}
