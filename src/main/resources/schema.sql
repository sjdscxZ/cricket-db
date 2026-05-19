-- cricket-db: normalized schema
CREATE TABLE IF NOT EXISTS teams (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL UNIQUE,
    country VARCHAR(60) NOT NULL
);

CREATE TABLE IF NOT EXISTS players (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    team_id INT NOT NULL,
    role VARCHAR(30) NOT NULL,
    born DATE,
    FOREIGN KEY (team_id) REFERENCES teams(id)
);

CREATE TABLE IF NOT EXISTS matches (
    id INT PRIMARY KEY AUTO_INCREMENT,
    venue VARCHAR(120) NOT NULL,
    played_on DATE NOT NULL,
    team_a_id INT NOT NULL,
    team_b_id INT NOT NULL,
    winner_team_id INT,
    FOREIGN KEY (team_a_id) REFERENCES teams(id),
    FOREIGN KEY (team_b_id) REFERENCES teams(id),
    FOREIGN KEY (winner_team_id) REFERENCES teams(id)
);

CREATE TABLE IF NOT EXISTS balls (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id INT NOT NULL,
    over_no SMALLINT NOT NULL,
    ball_no SMALLINT NOT NULL,
    batter_id INT NOT NULL,
    bowler_id INT NOT NULL,
    runs SMALLINT NOT NULL DEFAULT 0,
    extras SMALLINT NOT NULL DEFAULT 0,
    is_wicket BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (match_id) REFERENCES matches(id),
    FOREIGN KEY (batter_id) REFERENCES players(id),
    FOREIGN KEY (bowler_id) REFERENCES players(id)
);

-- Indexes (added in benchmarks to demonstrate the 60% speed-up claim)
CREATE INDEX IF NOT EXISTS idx_balls_match ON balls(match_id);
CREATE INDEX IF NOT EXISTS idx_balls_batter ON balls(batter_id);
CREATE INDEX IF NOT EXISTS idx_balls_bowler ON balls(bowler_id);
CREATE INDEX IF NOT EXISTS idx_players_team ON players(team_id);
