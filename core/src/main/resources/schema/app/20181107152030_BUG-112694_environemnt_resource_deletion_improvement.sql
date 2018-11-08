-- // BUG-112694_environemnt_resource_deletion_improvement
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS environment_id bigint;
ALTER TABLE ONLY cluster ADD CONSTRAINT fk_cluster_environment FOREIGN KEY (environment_id) REFERENCES environment(id);
CREATE INDEX IF NOT EXISTS idx_cluster_environment_id ON cluster(environment_id);

UPDATE cluster
SET environment_id = stack.environment_id
FROM stack
WHERE stack.id = cluster.stack_id;

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_cluster_environment_id;
ALTER TABLE ONLY cluster DROP CONSTRAINT IF EXISTS fk_cluster_environment;
ALTER TABLE cluster DROP COLUMN IF EXISTS environment_id;


