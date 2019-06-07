-- // CB-1740 cleanup defaultfs
-- Migration SQL that makes the change goes here.

ALTER TABLE filesystem
    DROP COLUMN IF EXISTS publicinaccount,
    DROP COLUMN IF EXISTS account;

ALTER TABLE filesystem ALTER COLUMN defaultfs SET DEFAULT false;

DROP TABLE IF EXISTS filesystem_properties;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE filesystem
    ADD COLUMN IF NOT EXISTS publicinaccount boolean      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS account         VARCHAR(255) NOT NULL DEFAULT '';

CREATE TABLE filesystem_properties (
    filesystem_id bigint NOT NULL,
    value text,
    key character varying(255) NOT NULL
);

ALTER TABLE ONLY filesystem_properties
    ADD CONSTRAINT filesystem_properties_pkey PRIMARY KEY (filesystem_id, key);

ALTER TABLE ONLY filesystem_properties
    ADD CONSTRAINT fk_filesystem_properties_filesystem_id FOREIGN KEY (filesystem_id) REFERENCES filesystem(id);