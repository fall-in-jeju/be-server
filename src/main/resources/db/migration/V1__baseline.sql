-- V1__init.sql
-- Baseline migration for existing schema
-- This migration represents the current production schema
-- DO NOT MODIFY OR RE-RUN THIS FILE

-- users
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sub VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_sub (sub)
);

-- disaster_message
CREATE TABLE disaster_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '내부 DB PK',
    external_id VARCHAR(22) NOT NULL COMMENT '재난 데이터 일련 코드 (API SN)',
    region_name VARCHAR(255) NOT NULL COMMENT '재난 발령 지역명',
    disaster_type VARCHAR(100) NOT NULL COMMENT '재해 구분 (태풍, 호우 등)',
    emergency_step VARCHAR(100) NOT NULL COMMENT '긴급 단계 구분 (긴급재난, 안전안내 등)',
    content VARCHAR(4000) NOT NULL COMMENT '재난 발령 내용',
    issued_at DATETIME NOT NULL COMMENT '재난 발령 시각 (API CRT_DT)',
    created_at DATETIME NOT NULL COMMENT 'DB 저장 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_disaster_message_external_id UNIQUE (external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- disaster_message_read
CREATE TABLE disaster_message_read (
    disaster_message_id BIGINT NOT NULL COMMENT '재난 메시지 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    read_at DATETIME NOT NULL COMMENT '재난 메시지 읽은 시각',
    PRIMARY KEY (disaster_message_id, user_id),
    CONSTRAINT fk_disaster_message_read_message
       FOREIGN KEY (disaster_message_id)
           REFERENCES disaster_message (id)
           ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- tourist_place
CREATE TABLE tourist_place (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    lat DOUBLE NOT NULL,
    lon DOUBLE NOT NULL,
    road_address VARCHAR(255) NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    score INT NOT NULL DEFAULT 0,
    type VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tourist_place_name UNIQUE (name),
    CONSTRAINT uk_tourist_place_lat_lon UNIQUE (lat, lon)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- travel_info
CREATE TABLE travel_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_date DATE,
    end_date DATE,
    capacity BIGINT,
    money BIGINT,
    region VARCHAR(50),
    language VARCHAR(10),
    create_date DATETIME(6) NOT NULL,
    update_date DATETIME(6),
    CONSTRAINT fk_travel_info_user
     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
