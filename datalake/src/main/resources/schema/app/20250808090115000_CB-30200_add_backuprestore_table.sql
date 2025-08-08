-- // CB-30200 Add backup/restore settings table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS sdxbackuprestoresettings (
                                          sdxClusterCrn CHARACTER VARYING(255) NOT NULL,
                                          backupTempLocation TEXT,
                                          backupTimeoutInMinutes INTEGER NOT NULL,
                                          restoreTempLocation TEXT,
                                          restoreTimeoutInMinutes INTEGER NOT NULL,
                                          PRIMARY KEY (sdxClusterCrn)
);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS sdxbackuprestoresettings;