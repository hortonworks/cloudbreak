-- // CLOUD-828 stack - cluster relation
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN stack_id BIGINT REFERENCES stack(id);

UPDATE cluster SET stack_id=stack.id FROM stack WHERE stack.cluster_id=cluster.id;
ALTER TABLE cluster ALTER COLUMN stack_id set NOT NULL;

ALTER TABLE stack drop COLUMN cluster_id;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack ADD COLUMN cluster_id BIGINT REFERENCES cluster(id);

UPDATE stack SET cluster_id=cluster.id FROM cluster WHERE stack.id=cluster.stack_id;

ALTER TABLE cluster DROP COLUMN stack_id;

