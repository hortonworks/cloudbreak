-- // CB-2934 add created to blueprint, cluster template and recipe
-- Migration SQL that makes the change goes here.

ALTER TABLE recipe ADD COLUMN created int8 NOT NULL DEFAULT (date_part('epoch'::text, now()) * 1000::double precision);
ALTER TABLE clustertemplate ADD COLUMN created int8 NOT NULL DEFAULT (date_part('epoch'::text, now()) * 1000::double precision);
ALTER TABLE blueprint ADD COLUMN created int8 NOT NULL DEFAULT (date_part('epoch'::text, now()) * 1000::double precision);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE recipe DROP COLUMN created;
ALTER TABLE clustertemplate DROP COLUMN created;
ALTER TABLE blueprint DROP COLUMN created;
