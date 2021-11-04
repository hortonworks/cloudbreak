-- // CB-14617 remove idbroker for cluster templates
-- Migration SQL that makes the change goes here.

DELETE FROM idbroker WHERE cluster_id IN
(SELECT c.id FROM stack s JOIN cluster c ON s.id = c.stack_id WHERE s.type = 'TEMPLATE');

-- //@UNDO
-- SQL to undo the change goes here.