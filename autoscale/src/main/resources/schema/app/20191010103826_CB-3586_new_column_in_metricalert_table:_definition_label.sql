-- // CB-3586 new column in metricalert table: definition_label
-- Migration SQL that makes the change goes here.

ALTER TABLE metricalert ADD COLUMN IF NOT EXISTS definition_label TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

alter table metricalert DROP COLUMN IF EXISTS definition_label;
