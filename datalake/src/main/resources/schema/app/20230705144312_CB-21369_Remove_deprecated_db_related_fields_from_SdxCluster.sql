-- // CB-21369 Remove deprecated db related fields from SdxCluster
-- Migration SQL that makes the change goes here.

INSERT INTO sdxdatabase (sdxcluster_id, createdatabase, databaseavailabilitytype, databaseengineversion, databasecrn)
    SELECT sdxcluster.id, sdxcluster.createdatabase, sdxcluster.databaseavailabilitytype, sdxcluster.databaseengineversion, sdxcluster.databasecrn
        FROM sdxcluster WHERE sdxdatabase_id IS NULL;

UPDATE sdxcluster
    SET sdxdatabase_id = sdxdatabase.id
    FROM sdxdatabase
    WHERE sdxdatabase_id IS NULL AND sdxdatabase.sdxcluster_id = sdxcluster.id;

UPDATE sdxdatabase
    SET databaseengineversion = sdxcluster.databaseengineversion
    FROM sdxcluster
    WHERE sdxcluster.sdxdatabase_id = sdxdatabase.id AND
        sdxcluster.databaseengineversion IS NOT NULL AND
        sdxdatabase.databaseengineversion != sdxcluster.databaseengineversion;

ALTER TABLE sdxcluster ALTER COLUMN createdatabase DROP NOT NULL;
ALTER TABLE sdxcluster ALTER COLUMN createdatabase DROP DEFAULT;
ALTER TABLE sdxdatabase ALTER COLUMN sdxcluster_id DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE sdxcluster
    SET
        createdatabase = sdxdatabase.createdatabase,
        databaseavailabilitytype = sdxdatabase.databaseavailabilitytype,
        databaseengineversion = sdxdatabase.databaseengineversion,
        databasecrn = sdxdatabase.databasecrn
    FROM sdxdatabase
    WHERE sdxcluster.sdxdatabase_id = sdxdatabase.id;

UPDATE sdxdatabase
    SET sdxcluster_id = sdxcluster.id
    FROM sdxcluster
    WHERE sdxdatabase.sdxcluster_id IS NULL AND sdxcluster.sdxdatabase_id = sdxdatabase.id;

ALTER TABLE sdxdatabase ALTER COLUMN sdxcluster_id SET NOT NULL;
ALTER TABLE sdxcluster ALTER COLUMN createdatabase SET NOT NULL;
ALTER TABLE sdxcluster ALTER COLUMN createdatabase SET DEFAULT FALSE;
