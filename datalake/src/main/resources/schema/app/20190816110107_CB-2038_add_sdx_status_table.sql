-- // CB-2038 add sdx status table
-- Migration SQL that makes the change goes here.

CREATE TABLE sdxstatus (id bigserial NOT NULL,
                        datalake bigint,
                        created bigint,
                        statusReason TEXT,
                        status character varying(255) NOT NULL,
                        PRIMARY KEY (id));

CREATE INDEX index_sdxstatus_datalake_status ON sdxstatus (datalake, status);
CREATE INDEX index_sdxstatus_datalake ON sdxstatus (datalake);

INSERT INTO sdxstatus (datalake, created, statusReason, status) SELECT sdxcluster.id, sdxcluster.created, sdxcluster.statusreason, sdxcluster.status from sdxcluster;

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS statusreason;
ALTER TABLE sdxcluster DROP COLUMN IF EXISTS status;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS statusreason TEXT;
ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS status character varying(255) NOT NULL default 'DELETED';

UPDATE sdxcluster SET status = sdxclusterstatus.status, statusreason = sdxclusterstatus.statusreason
FROM (SELECT DISTINCT ON (sdxcluster.id)  sdxcluster.id as datalakeid, sdxstatus.id as statusid, sdxstatus.status as status, sdxstatus.statusreason as statusreason
      FROM sdxcluster INNER JOIN sdxstatus on sdxcluster.id = sdxstatus.datalake ORDER BY sdxcluster.id, sdxstatus.id DESC) AS sdxclusterstatus
WHERE sdxclusterstatus.datalakeid = sdxcluster.id;

DROP INDEX IF EXISTS index_sdxstatus_datalake_status;
DROP INDEX IF EXISTS index_sdxstatus_datalake;
DROP TABLE IF EXISTS sdxstatus;