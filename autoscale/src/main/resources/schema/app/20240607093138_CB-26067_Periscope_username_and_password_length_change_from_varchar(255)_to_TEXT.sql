-- // CB-26067 Periscope username and password length change from varchar(255) to TEXT
-- Migration SQL that makes the change goes here.
ALTER TABLE cluster_manager
    ALTER COLUMN lgn_pass TYPE TEXT,
    ALTER COLUMN lgn_user TYPE TEXT;


-- //@UNDO
-- SQL to undo the change goes here.


