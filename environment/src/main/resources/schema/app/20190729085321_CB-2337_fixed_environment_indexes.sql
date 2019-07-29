-- // CB-2337 fixed environment indexes
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS environment_status_accountid_idx;
DROP INDEX IF EXISTS environment_credential_id_idx;
DROP INDEX IF EXISTS environment_accountid_name_idx;
DROP INDEX IF EXISTS environment_id_accountid_idx;

CREATE INDEX IF NOT EXISTS environment_accountid_archived_idx ON environment USING btree (accountid, archived);
CREATE INDEX IF NOT EXISTS environment_accountid_name_archived_idx ON environment USING btree (accountid, name, archived);
CREATE INDEX IF NOT EXISTS environment_accountid_resourcecrn_archived_idx ON environment USING btree (accountid, resourcecrn, archived);
CREATE INDEX IF NOT EXISTS environment_id_status_archived_idx ON environment USING btree (id, status, archived);
CREATE INDEX IF NOT EXISTS environment_status_archived_idx ON environment USING btree (status, archived);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS environment_accountid_archived_idx;
DROP INDEX IF EXISTS environment_accountid_name_archived_idx;
DROP INDEX IF EXISTS environment_accountid_resourcecrn_archived_idx;
DROP INDEX IF EXISTS environment_id_status_archived_idx;
DROP INDEX IF EXISTS environment_status_archived_idx;

CREATE INDEX IF NOT EXISTS environment_status_accountid_idx ON environment USING btree (status, accountid);
CREATE INDEX IF NOT EXISTS environment_credential_id_idx ON environment USING btree (credential_id);
CREATE INDEX IF NOT EXISTS environment_accountid_name_idx ON environment USING btree (accountid, name);
CREATE INDEX IF NOT EXISTS environment_id_accountid_idx ON credential USING btree (id, accountid);