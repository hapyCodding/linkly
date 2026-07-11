-- NAS MariaDB에 linkly 데이터베이스와 전용 계정을 만든다.
-- phpMyAdmin(root) 또는 SSH의 mysql 클라이언트에서 root로 실행한다.
-- 아래 'CHANGE_ME'를 실제 비밀번호로 바꿀 것.

CREATE DATABASE IF NOT EXISTS linkly
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'linkly'@'%' IDENTIFIED BY 'CHANGE_ME';
GRANT ALL PRIVILEGES ON linkly.* TO 'linkly'@'%';
FLUSH PRIVILEGES;

-- 테이블은 애플리케이션 첫 기동 시 JPA(ddl-auto: update)가 자동 생성한다.
