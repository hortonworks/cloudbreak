-- // CB-13985 Add recipe hashes per hostgroup
ALTER TABLE hostgroup ADD COLUMN IF NOT EXISTS recipehashes text;

-- //@UNDO
ALTER TABLE hostgroup DROP COLUMN IF EXISTS recipehashes;