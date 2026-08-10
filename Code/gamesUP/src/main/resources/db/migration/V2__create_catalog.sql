CREATE TABLE authors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    normalized_name VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    CONSTRAINT pk_authors PRIMARY KEY (id),
    CONSTRAINT uk_authors_normalized_name UNIQUE (normalized_name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE publishers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    normalized_name VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    CONSTRAINT pk_publishers PRIMARY KEY (id),
    CONSTRAINT uk_publishers_normalized_name UNIQUE (normalized_name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    normalized_name VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_normalized_name UNIQUE (normalized_name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE games (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    min_players INT NOT NULL,
    max_players INT NOT NULL,
    min_age INT NOT NULL,
    duration_minutes INT NOT NULL,
    edition_number INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    publisher_id BIGINT NOT NULL,
    CONSTRAINT pk_games PRIMARY KEY (id),
    CONSTRAINT fk_games_publisher FOREIGN KEY (publisher_id) REFERENCES publishers (id),
    CONSTRAINT chk_games_price CHECK (price >= 0),
    CONSTRAINT chk_games_min_players CHECK (min_players >= 1),
    CONSTRAINT chk_games_player_range CHECK (max_players >= min_players),
    CONSTRAINT chk_games_min_age CHECK (min_age >= 0),
    CONSTRAINT chk_games_duration CHECK (duration_minutes >= 1),
    CONSTRAINT chk_games_edition CHECK (edition_number >= 1)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE game_authors (
    game_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    CONSTRAINT pk_game_authors PRIMARY KEY (game_id, author_id),
    CONSTRAINT fk_game_authors_game FOREIGN KEY (game_id) REFERENCES games (id),
    CONSTRAINT fk_game_authors_author FOREIGN KEY (author_id) REFERENCES authors (id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE INDEX idx_game_authors_author ON game_authors (author_id);

CREATE TABLE game_categories (
    game_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT pk_game_categories PRIMARY KEY (game_id, category_id),
    CONSTRAINT fk_game_categories_game FOREIGN KEY (game_id) REFERENCES games (id),
    CONSTRAINT fk_game_categories_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE INDEX idx_game_categories_category ON game_categories (category_id);
