-- // CB-29677 add extra database index to customconfigurations table
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_customconfigs_account ON customconfigurations (account);
CREATE INDEX IF NOT EXISTS idx_customconfigproperties_config_id ON customconfigurations_properties (customconfigurations_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_customconfigs_account;
DROP INDEX IF EXISTS idx_customconfigproperties_config_id