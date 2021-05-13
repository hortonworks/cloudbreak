-- // CB-12457 create image_history table
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS revision_id_seq INCREMENT 1 MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;

CREATE TABLE IF NOT EXISTS revision_info
(
    rev         INT4 NOT NULL DEFAULT nextval('revision_id_seq')
        CONSTRAINT revinfo_pkey PRIMARY KEY,
    "timestamp" INT8
);

CREATE TABLE IF NOT EXISTS image_history
(
    id               BIGINT NOT NULL,
    rev              INT4   NOT NULL
        CONSTRAINT fk_component_revinfo_rev REFERENCES revision_info,
    revtype          INT2,
    stack_id         BIGINT NOT NULL,
    imagename        VARCHAR(255),
    os               VARCHAR(255),
    ostype           VARCHAR(255),
    imagecatalogurl  TEXT,
    imageid          VARCHAR(255),
    imagecatalogname VARCHAR(255),
    userdata         TEXT,
    CONSTRAINT image_history_pkey PRIMARY KEY (id, rev)
);

CREATE INDEX IF NOT EXISTS image_history_id_idx
    ON image_history (id);

CREATE INDEX IF NOT EXISTS image_history_stackid_idx
    ON image_history (stack_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS image_history_stackid_idx;
DROP INDEX IF EXISTS image_history_id_idx;
DROP TABLE IF EXISTS image_history;
DROP TABLE IF EXISTS revision_info;
DROP SEQUENCE IF EXISTS revision_id_seq;

