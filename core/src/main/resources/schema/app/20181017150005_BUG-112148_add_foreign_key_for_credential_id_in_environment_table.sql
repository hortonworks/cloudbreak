-- // BUG-112148 add foreign key for credential_id in environment table
-- Migration SQL that makes the change goes here.

ALTER TABLE ONLY environment ADD CONSTRAINT fk_environment_credential_id FOREIGN KEY (credential_id) REFERENCES credential(id);
CREATE INDEX IF NOT EXISTS idx_environment_credential_id ON environment (credential_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_environment_credential_id;
ALTER TABLE ONLY environment DROP CONSTRAINT IF EXISTS fk_environment_credential_id;
