-- // CB-20934 Collect sdx related db properties in SdxDatabase entity
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS sdxdatabase (
    id bigserial NOT NULL,
	sdxcluster_id bigserial NOT NULL,
    createdatabase boolean NOT NULL DEFAULT false,
    databaseavailabilitytype character varying(25),
    databaseengineversion character varying(255),
    databasecrn character varying(255),
    attributes text,
    PRIMARY KEY (id)
);

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS sdxdatabase_id bigint;

ALTER TABLE ONLY sdxcluster ADD CONSTRAINT fk_sdxdatabaseidsdxcluster FOREIGN KEY (sdxdatabase_id) REFERENCES sdxdatabase(id);

INSERT INTO sdxdatabase (sdxcluster_id, createdatabase, databaseavailabilitytype, databaseengineversion, databasecrn)
   SELECT sdxcluster.id, sdxcluster.createdatabase, sdxcluster.databaseavailabilitytype, sdxcluster.databaseengineversion, sdxcluster.databasecrn FROM sdxcluster;

UPDATE sdxcluster
   SET sdxdatabase_id = sdxdatabase.id
   FROM sdxdatabase
   WHERE sdxdatabase.sdxcluster_id = sdxcluster.id;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY sdxcluster DROP CONSTRAINT IF EXISTS fk_sdxdatabaseidsdxcluster;

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS sdxdatabase_id;

DROP TABLE IF EXISTS sdxdatabase;
