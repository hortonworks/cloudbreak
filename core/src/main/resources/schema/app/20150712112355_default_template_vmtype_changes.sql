-- // default template vmtype changes
-- Migration SQL that makes the change goes here.

UPDATE template SET gcpinstancetype='N1_STANDARD_4' WHERE name='minviable-gcp';
UPDATE template SET vmtype='STANDARD_D4' WHERE name='minviable-azure';


-- //@UNDO
-- SQL to undo the change goes here.

UPDATE template SET gcpinstancetype='N1_STANDARD_2' WHERE name='minviable-gcp';
UPDATE template SET vmtype='STANDARD_D2' WHERE name='minviable-azure';
