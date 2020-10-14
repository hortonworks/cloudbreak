-- // CB-9289 ssl certificate added to db stack
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS sslconfig
(
    id BIGINT NOT NULL,
    sslcertificatetype VARCHAR(255) NOT NULL DEFAULT 'NONE',
    PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS sslconfig_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE UNIQUE INDEX IF NOT EXISTS sslconfig_id_idx ON sslconfig(id);

CREATE TABLE IF NOT EXISTS sslconfig_sslcertificates (
    sslconfig_id bigint NOT NULL REFERENCES sslconfig (id),
    sslcertificate_value text
);

ALTER TABLE dbstack ADD COLUMN IF NOT EXISTS sslconfig_id BIGINT REFERENCES sslconfig(id);

-- //@UNDO

ALTER TABLE dbstack DROP COLUMN IF EXISTS sslconfig_id;
DROP INDEX IF EXISTS sslconfig_id_idx;
DROP SEQUENCE IF EXISTS sslconfig_id_seq;
DROP TABLE IF EXISTS sslconfig_sslcertificates;
DROP TABLE IF EXISTS sslconfig;
