-- // CB-1358 Rename every Ambari related DB column to generic one
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster
ADD COLUMN IF NOT EXISTS cloudbreakclustermanageruser varchar(255);

ALTER TABLE cluster
ADD COLUMN IF NOT EXISTS cloudbreakclustermanagerpassword varchar(255);

ALTER TABLE cluster
ADD COLUMN IF NOT EXISTS clustermanagersecuritymasterkey text;

ALTER TABLE cluster
ADD COLUMN IF NOT EXISTS dpclustermanageruser varchar(255);

ALTER TABLE cluster
ADD COLUMN IF NOT EXISTS dpclustermanagerpassword varchar(255);

ALTER TABLE datalakeresources
ADD COLUMN IF NOT EXISTS datalakeclustermanagerip varchar(255);
ALTER  TABLE datalakeresources
ALTER COLUMN datalakeclustermanagerip SET NOT NULL;
ALTER TABLE datalakeresources
ALTER COLUMN datalakeambariip DROP NOT NULL;

ALTER TABLE datalakeresources
ADD COLUMN IF NOT EXISTS datalakeclustermanagerfqdn varchar(255);
ALTER  TABLE datalakeresources
ALTER COLUMN datalakeclustermanagerfqdn SET NOT NULL;
ALTER TABLE datalakeresources
ALTER COLUMN datalakeambarifqdn DROP NOT NULL;

ALTER TABLE datalakeresources
ADD COLUMN IF NOT EXISTS datalakeclustermanagerurl varchar(255);

ALTER TABLE instancemetadata
ADD COLUMN IF NOT EXISTS clustermanagerserver bool;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster
DROP COLUMN cloudbreakclustermanageruser;

ALTER TABLE cluster
DROP COLUMN cloudbreakclustermanagerpassword;

ALTER TABLE cluster
DROP COLUMN clustermanagersecuritymasterkey;

ALTER TABLE cluster
DROP COLUMN dpclustermanageruser;

ALTER TABLE cluster
DROP COLUMN dpclustermanagerpassword;

ALTER TABLE datalakeresources
DROP COLUMN datalakeclustermanagerfqdn;

ALTER TABLE datalakeresources
ALTER COLUMN datalakeambarifqdn SET NOT NULL;

ALTER TABLE datalakeresources
DROP COLUMN datalakeclustermanagerip;

ALTER TABLE datalakeresources
ALTER COLUMN datalakeambariip SET NOT NULL;

ALTER TABLE datalakeresources
DROP COLUMN datalakeclustermanagerurl;

ALTER TABLE instancemetadata
DROP COLUMN clustermanagerserver;