-- // CLOUD-85193 Ability to set different custom domains for different clusters
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN customDomain VARCHAR(255);
ALTER TABLE stack ADD COLUMN customHostname VARCHAR(255);
ALTER TABLE stack ADD COLUMN clusterNameAsSubdomain boolean default false;
ALTER TABLE stack ADD COLUMN hostgroupNameAsHostname boolean default false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS customDomain;
ALTER TABLE stack DROP COLUMN IF EXISTS customHostname;
ALTER TABLE stack DROP COLUMN IF EXISTS clusterNameAsSubdomain;
ALTER TABLE stack DROP COLUMN IF EXISTS hostgroupNameAsHostname;

