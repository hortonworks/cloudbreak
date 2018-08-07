-- // RMP-11180 Rename Nifi blueprint
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET name='Flow Management: Apache NiFi, Apache NiFi Registry' WHERE name='Flow Management: Apache NiFi';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET name='Flow Management: Apache NiFi' WHERE name='Flow Management: Apache NiFi, Apache NiFi Registry';