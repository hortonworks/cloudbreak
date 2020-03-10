-- // CB-5734_cluster_table_enhancements
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS cb_stack_name VARCHAR(255);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS cb_stack_type VARCHAR(255);

CREATE INDEX idx_cluster_cb_stack_type ON cluster (cb_stack_type);

CREATE UNIQUE INDEX IF NOT EXISTS cluster_cb_stack_name_uindex
    ON cluster (cb_stack_name);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS cluster_cb_stack_name_uindex;

DROP INDEX IF EXISTS idx_cluster_cb_stack_type;

ALTER TABLE cluster DROP COLUMN IF EXISTS cb_stack_name;

ALTER TABLE cluster DROP COLUMN IF EXISTS cb_stack_type;

