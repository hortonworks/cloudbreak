-- // CB-14785 create table keytabcache
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS keytabcache_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS keytabcache (
    id                BIGINT NOT NULL,
    environmentcrn    VARCHAR(255) NOT NULL,
    accountid         VARCHAR(255) NOT NULL,
    principalhash     VARCHAR(256) NOT NULL,
    hostname          VARCHAR(255) NOT NULL,
    keytab            TEXT,
    principal         TEXT,

    CONSTRAINT        pk_keytabcache_id          PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_keytabcache_envcrn_princhash ON keytabcache (environmentcrn, principalhash);
CREATE INDEX IF NOT EXISTS idx_keytabcache_envcrn_hostname ON keytabcache (environmentcrn, hostname);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS keytabcache;
DROP SEQUENCE IF EXISTS keytabcache_id_seq;
