-- // CLOUD-67379 Remove daily usage report generation mechanism
-- Migration SQL that makes the change goes here.

ALTER TABLE cloudbreakusage ADD COLUMN PeriodStarted timestamp without time zone;
ALTER TABLE cloudbreakusage ADD COLUMN Duration character varying(255);
ALTER TABLE cloudbreakusage ADD COLUMN Status character varying(255) NOT NULL DEFAULT 'CLOSED';
ALTER TABLE cloudbreakusage ADD COLUMN InstanceNum integer;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cloudbreakusage DROP COLUMN IF EXISTS PeriodStarted;
ALTER TABLE cloudbreakusage DROP COLUMN IF EXISTS Duration;
ALTER TABLE cloudbreakusage DROP COLUMN IF EXISTS Status;
ALTER TABLE cloudbreakusage DROP COLUMN IF EXISTS InstanceNum;
