-- // CB-4299 Increase component and cluster_component id sequence step size
-- Migration SQL that makes the change goes here.

ALTER SEQUENCE component_id_seq INCREMENT BY 20;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER SEQUENCE component_id_seq INCREMENT BY 1;


