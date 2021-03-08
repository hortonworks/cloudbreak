-- // CB-10578 Populate new targetgroup_loadbalancer table
-- Migration SQL that makes the change goes here.

INSERT INTO targetgroup_loadbalancer (
    SELECT id, loadbalancer_id FROM targetgroup
    WHERE id IS NOT NULL AND loadbalancer_id IS NOT NULL)
ON CONFLICT DO NOTHING;

-- //@UNDO
-- SQL to undo the change goes here.


