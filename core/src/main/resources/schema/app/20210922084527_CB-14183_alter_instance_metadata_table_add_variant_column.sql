-- // CB-13685 alter image table add date column
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata ADD COLUMN IF NOT EXISTS variant VARCHAR(255);

update instancemetadata im set variant=ig.pv
from (
    select ig.id, s.platformvariant as pv
    from instancegroup ig
        join stack s on s.id = ig.stack_id) as ig
where im.variant is null
  and im.instancegroup_id = ig.id;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancemetadata DROP COLUMN IF EXISTS variant;