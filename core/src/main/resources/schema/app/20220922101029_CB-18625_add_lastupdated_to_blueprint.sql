-- // CB-18625 add created to blueprint, cluster template and recipe
-- Migration SQL that makes the change goes here.

ALTER TABLE blueprint ADD COLUMN lastupdated int8 NOT NULL DEFAULT (date_part('epoch'::text, now()) * 1000::double precision);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE blueprint DROP COLUMN lastupdated;
