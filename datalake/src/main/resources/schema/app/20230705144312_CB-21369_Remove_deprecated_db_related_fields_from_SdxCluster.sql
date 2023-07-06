-- // CB-21369 Remove deprecated db related fields from SdxCluster
-- Migration SQL that makes the change goes here.

INSERT INTO sdxdatabase (sdxcluster_id, createdatabase, databaseavailabilitytype, databaseengineversion, databasecrn)
   SELECT sdxcluster.id, sdxcluster.createdatabase, sdxcluster.databaseavailabilitytype, sdxcluster.databaseengineversion, sdxcluster.databasecrn FROM sdxcluster WHERE sdxdatabase_id == NULL;

UPDATE sdxcluster
   SET sdxdatabase_id = sdxdatabase.id
   FROM sdxdatabase
   WHERE sdxdatabase.sdxcluster_id = sdxcluster.id;

ALTER TABLE sdxdatabase DROP COLUMN IF EXISTS sdxcluster_id;

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS createdatabase;
ALTER TABLE sdxcluster DROP COLUMN IF EXISTS databaseavailabilitytype;
ALTER TABLE sdxcluster DROP COLUMN IF EXISTS databaseengineversion;
ALTER TABLE sdxcluster DROP COLUMN IF EXISTS databasecrn;

ALTER TABLE sdxcluster ALTER COLUMN sdxdatabase_id SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS createdatabase boolean NOT NULL DEFAULT false;
ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS databaseavailabilitytype character varying(25);
ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS databaseengineversion character varying(255);
ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS databasecrn character varying(255);

UPDATE sdxcluster
    SET
        createdatabase = sdxdatabase.createdatabase,
        databaseavailabilitytype = sdxdatabase.databaseavailabilitytype,
        databaseengineversion = sdxdatabase.databaseengineversion,
        databasecrn = sdxdatabase.databasecrn
    FROM sdxdatabase
    WHERE sdxcluster.sdxdatabase_id = sdxdatabase.id;
