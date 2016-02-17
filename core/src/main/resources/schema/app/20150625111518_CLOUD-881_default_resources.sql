-- // CLOUD-881 default resources
-- Migration SQL that makes the change goes here.

ALTER TABLE template ADD COLUMN status CHARACTER VARYING (255);
ALTER TABLE blueprint ADD COLUMN status CHARACTER VARYING (255);

UPDATE template SET status='USER_MANAGED';
UPDATE template SET status='DEFAULT' WHERE name='minviable-gcp' OR name='minviable-azure' OR name='minviable-aws';
UPDATE template SET status='DEFAULT_DELETED' WHERE deleted IS TRUE AND status='DEFAULT';

UPDATE blueprint SET status='USER_MANAGED';
UPDATE blueprint SET status='DEFAULT' WHERE name='hdp-small-default' OR name='hdp-spark-cluster' OR name='hdp-streaming-cluster';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE template DROP COLUMN status;
ALTER TABLE blueprint DROP COLUMN status;
