-- // CB-4299 Add new sequence for instancemetadata with increased step
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE component_id_big_step_seq
	INCREMENT BY 20
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START SELECT currval('component_id_seq') + 10000;

-- //@UNDO
-- SQL to undo the change goes here.


