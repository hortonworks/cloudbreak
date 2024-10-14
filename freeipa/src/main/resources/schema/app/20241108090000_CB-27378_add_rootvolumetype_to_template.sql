-- // CB-27378 Add rootvolumetype to template table
-- Migration SQL that makes the change goes here.

ALTER TABLE template ADD COLUMN IF NOT EXISTS rootvolumetype varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE template DROP COLUMN IF EXISTS rootvolumetype;