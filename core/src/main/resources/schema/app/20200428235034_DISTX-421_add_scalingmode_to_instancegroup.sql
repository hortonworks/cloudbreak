-- // DISTX-421 Add scalingmode column to instancegroup table
-- Migration SQL that makes the change goes here.

ALTER TABLE instancegroup ADD IF NOT EXISTS scalingmode VARCHAR(255);
ALTER TABLE instancegroup ALTER COLUMN scalingmode SET DEFAULT 'UNSPECIFIED';
UPDATE instancegroup SET scalingmode = 'UNSPECIFIED' WHERE scalingmode IS NULL;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancegroup DROP COLUMN IF EXISTS scalingmode;
