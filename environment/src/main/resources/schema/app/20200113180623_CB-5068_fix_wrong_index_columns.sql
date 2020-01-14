-- // CB-5068 fix wrong index columns in environment_parameters table
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS environment_parameters_accountid_name_idx;
DROP INDEX IF EXISTS environment_parameters_environment_id_idx;

CREATE INDEX IF NOT EXISTS environment_parameters_accountid_name_idx ON environment_parameters USING btree (accountid, name);
CREATE UNIQUE INDEX IF NOT EXISTS environment_parameters_environment_id_idx ON environment_parameters USING btree (environment_id);

-- //@UNDO
-- SQL to undo the change goes here.
