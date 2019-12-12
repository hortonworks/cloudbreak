-- // CB-4299 Increase instancemetadata id sequence step size
-- Migration SQL that makes the change goes here.

ALTER SEQUENCE instancemetadata_id_seq INCREMENT BY 50;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER SEQUENCE instancemetadata_id_seq INCREMENT BY 1;


