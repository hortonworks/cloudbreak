-- // CB-13055 adding primary key to instancegroup_availabilityzones
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS instancegroup_availabilityzones DROP CONSTRAINT IF EXISTS instancegroup_availabilityzones_pkey;
CREATE SEQUENCE IF NOT EXISTS instancegroup_availabilityzones_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER TABLE instancegroup_availabilityzones ADD COLUMN IF NOT EXISTS id BIGINT DEFAULT nextval('instancegroup_availabilityzones_id_seq');
UPDATE instancegroup_availabilityzones SET id=nextval('instancegroup_availabilityzones_id_seq') WHERE id IS NULL;
ALTER TABLE ONLY instancegroup_availabilityzones ADD CONSTRAINT instancegroup_availabilityzones_pkey PRIMARY KEY (id, instancegroup_id);

-- //@UNDO
-- SQL to undo the change goes here.


