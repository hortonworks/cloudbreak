-- // CB-1157 user preferences in env service
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
    user_crn            CHARACTER VARYING (255)
);

-- //@UNDO
-- SQL to undo the change goes here.


