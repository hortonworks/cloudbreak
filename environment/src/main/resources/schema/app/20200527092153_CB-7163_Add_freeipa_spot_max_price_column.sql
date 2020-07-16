-- // CB-7163 Add freeipa spot max price column
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS freeipa_spot_max_price double precision;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS freeipa_spot_max_price;

