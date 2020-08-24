-- // CB-1068 multi az
-- Migration SQL that makes the change goes here.

CREATE TABLE instancegroupnetwork (
    id bigint NOT NULL,
    cloudPlatform CHARACTER VARYING(255),
    attributes TEXT,
    PRIMARY KEY (id)
);

CREATE SEQUENCE instancegroupnetwork_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE instancegroup ADD COLUMN IF NOT EXISTS network_id BIGINT NULL;

ALTER TABLE ONLY instancegroup ADD CONSTRAINT fk_instancegroup_network_id FOREIGN KEY (network_id) REFERENCES instancegroupnetwork(id);

ALTER TABLE instancegroup ADD COLUMN IF NOT EXISTS availabilityZone TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancegroup DROP COLUMN IF EXISTS availabilityZone;

ALTER TABLE instancegroup DROP COLUMN IF EXISTS network_id;

DROP SEQUENCE IF EXISTS instancegroupnetwork_id_seq;

DROP TABLE IF EXISTS instancegroupnetwork;


