-- // multiple datalakeresources per environment
-- Migration SQL that makes the change goes here.

ALTER TABLE datalakeresources ADD COLUMN IF NOT EXISTS environment_id BIGINT;

ALTER TABLE ONLY datalakeresources ADD CONSTRAINT fk_datalakeresources_environment_id FOREIGN KEY (environment_id) REFERENCES environment(id);

UPDATE datalakeresources set environment_id=environment.id from environment where environment.datalakeresources_id=datalakeresources.id;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY datalakeresources DROP CONSTRAINT IF EXISTS fk_datalakeresources_environment_id;

ALTER TABLE datalakeresources DROP COLUMN IF EXISTS environment_id;
