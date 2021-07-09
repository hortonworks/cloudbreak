-- // CB-12965 add flow operation stats table & add operation type field for flowlog
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS flowlog add COLUMN IF NOT EXISTS operationtype varchar(255);

CREATE TABLE IF NOT EXISTS flowoperationstats (
    id bigserial NOT NULL,
    operationtype varchar(255) NOT NULL,
    cloudplatform varchar(255) NOT NULL,
    durationhistory text,
    CONSTRAINT flowoperationstats_pkey PRIMARY KEY (id),
    CONSTRAINT uk_flowoperationstats_operationtype_cloudplatform UNIQUE (operationtype, cloudplatform)
);

CREATE SEQUENCE IF NOT EXISTS flowoperationstats_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE INDEX IF NOT EXISTS idx_flowoperationstats_operationtype_cloudplatform ON flowoperationstats USING btree (operationtype, cloudplatform);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_flowoperationstats_operationtype_cloud_platform;

DROP SEQUENCE IF EXISTS flowoperationstats_id_seq;

DROP TABLE IF EXISTS flowoperationstats;

ALTER TABLE IF EXISTS flowlog DROP COLUMN IF EXISTS operationtype;