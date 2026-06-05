-- 创建数据库
CREATE DATABASE IF NOT EXISTS novel2script
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE novel2script;

-- 小说上传记录表
CREATE TABLE novel_upload (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    chapter_count INT DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    upload_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_status (status),
    INDEX idx_file_type (file_type),
    INDEX idx_upload_time (upload_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 剧本输出记录表
CREATE TABLE script_output (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    original_author VARCHAR(255),
    genre VARCHAR(50),
    yaml_file_path VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    chapter_count INT DEFAULT 0,
    total_scenes INT DEFAULT 0,
    error_message VARCHAR(1000),
    created_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_novel_id (novel_id),
    INDEX idx_status (status),
    INDEX idx_created_time (created_time),
    FOREIGN KEY (novel_id) REFERENCES novel_upload(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
