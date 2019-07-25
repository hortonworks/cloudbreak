-- // CB-2637 Redbeams: Redbeams databaseserver list and get does not return status
-- Migration SQL that makes the change goes here.

ALTER TABLE databaseserverconfig ADD COLUMN dbstack_id BIGINT REFERENCES dbstack(id);
UPDATE databaseserverconfig AS c SET dbstack_id=dbs.id FROM (SELECT id, resourcecrn FROM dbstack) AS dbs WHERE c.resourcecrn=dbs.resourcecrn;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE databaseserverconfig SET dbstack_id=NULL WHERE dbstack_id IS NOT NULL;
ALTER TABLE databaseserverconfig DROP COLUMN dbstack_id;


