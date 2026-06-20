\c auth_db;

CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(255),
                       email VARCHAR(255),
                       password VARCHAR(255),
                       role VARCHAR(50)
);

INSERT INTO users (username, email, password, role)
VALUES ('test', 'test@test.com', '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', 'PLAYER');
