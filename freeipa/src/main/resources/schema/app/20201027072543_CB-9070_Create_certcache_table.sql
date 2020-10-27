-- // CB-9070 Create certcache table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS certcache (
    key TEXT NOT NULL,
    cert TEXT NOT NULL,

    CONSTRAINT pk_certcache_key PRIMARY KEY (key)
);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS certcache;
