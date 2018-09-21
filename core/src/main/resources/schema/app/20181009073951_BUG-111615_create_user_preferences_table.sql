-- // BUG-111615 create user preferences table
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS userpreferences_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS user_preferences
(
    id                  BIGINT PRIMARY KEY NOT NULL DEFAULT nextval('userpreferences_id_seq'),
    externalId          CHARACTER VARYING (255),
    user_id             BIGINT NOT NULL
);

ALTER TABLE ONLY user_preferences
    ADD CONSTRAINT fk_user_preferences_user_id FOREIGN KEY (user_id) REFERENCES users(id);

INSERT INTO user_preferences (user_id)
	SELECT id AS "userId" FROM users WHERE id NOT IN (SELECT user_id FROM user_preferences);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY user_preferences DROP CONSTRAINT IF EXISTS fk_user_preferences_user_id;

DROP TABLE IF EXISTS user_preferences;

DROP SEQUENCE IF EXISTS userpreferences_id_seq;
