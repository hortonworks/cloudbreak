-- // BUG-91325_remove_backward_compatibel_fields
-- Migration SQL that makes the change goes here.

ALTER TABLE gateway DROP COLUMN IF EXISTS topologyname;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE gateway ADD COLUMN topologyname VARCHAR (255) NULL;