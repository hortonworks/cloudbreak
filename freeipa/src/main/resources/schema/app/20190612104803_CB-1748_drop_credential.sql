-- // CB-1748 drop credential
-- Migration SQL that makes the change goes here.

ALTER TABLE stack DROP CONSTRAINT fk_stack_credential_id;

ALTER TABLE stack DROP COLUMN credential_id;

DROP TABLE credential;

DROP SEQUENCE credential_id_seq;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE SEQUENCE credential_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS credential
(
    id BIGINT DEFAULT nextval('credential_id_seq'::regclass) NOT NULL
        CONSTRAINT credential_pkey
            PRIMARY KEY,
    name VARCHAR(255),
    attributes text
);

CREATE UNIQUE INDEX IF NOT EXISTS credential_id_idx
    ON credential (id);

ALTER TABLE stack ADD COLUMN credential_id BIGINT;

ALTER TABLE stack
    ADD CONSTRAINT fk_stack_credential_id
        FOREIGN KEY (credential_id) REFERENCES credential;
