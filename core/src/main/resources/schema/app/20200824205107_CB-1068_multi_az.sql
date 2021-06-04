-- // CB-1068 multi az
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS instancegroupnetwork (
    id bigint NOT NULL,
    cloudPlatform CHARACTER VARYING(255),
    attributes TEXT,
    instancegroup_id bigint NOT NULL,
    stack_network_id bigint,
    subnetid TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE instancegroup_availabilityzones (
    instancegroup_id bigint NOT NULL,
    availabilityzone text
);

ALTER TABLE ONLY instancegroupnetwork ADD CONSTRAINT fk_instancegroup_network_id FOREIGN KEY (instancegroup_id) REFERENCES instancegroup(id);
CREATE SEQUENCE IF NOT EXISTS instancegroupnetwork_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
INSERT INTO instancegroup_availabilityzones (instancegroup_id, availabilityzone) SELECT instancegroup.id, availabilityzone FROM instancegroup INNER JOIN stack ON stack.id = instancegroup.stack_id WHERE stack.availabilityzone IS NOT NULL;
INSERT INTO instancegroupnetwork (id, cloudPlatform, instancegroup_id, stack_network_id) SELECT nextval('instancegroupnetwork_id_seq') AS "id", cloudplatform, instancegroup.id, stack.network_id FROM instancegroup INNER JOIN stack ON stack.id = instancegroup.stack_id;
UPDATE instancegroupnetwork SET attributes=network.attributes FROM network WHERE network.id=instancegroupnetwork.stack_network_id;
UPDATE instancegroupnetwork SET subnetid=instancegroupnetwork.attributes::json->'subnetId';
UPDATE instancegroupnetwork SET subnetid = REPLACE(subnetid,'"','');
UPDATE instancegroupnetwork SET attributes = json_build_object('subnetIds', json_build_array(subnetid), 'cloudPlatform', cloudPlatform);

ALTER TABLE instancegroupnetwork DROP COLUMN stack_network_id;
ALTER TABLE instancegroupnetwork DROP COLUMN subnetid;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancegroup DROP COLUMN IF EXISTS network_id;
DROP SEQUENCE IF EXISTS instancegroupnetwork_id_seq;
DROP TABLE IF EXISTS instancegroupnetwork;
DROP TABLE IF EXISTS instancegroup_availabilityzones;