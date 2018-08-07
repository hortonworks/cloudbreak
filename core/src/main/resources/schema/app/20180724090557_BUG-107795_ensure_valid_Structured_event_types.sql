-- // BUG-107795 ensure valid Structured event types
-- Migration SQL that makes the change goes here.

DELETE FROM structuredevent WHERE eventtype NOT IN ('NOTIFICATION', 'FLOW', 'REST');

UPDATE structuredevent SET resourcetype = 'stacks' WHERE resourcetype = 'STACK';

-- //@UNDO
-- SQL to undo the change goes here.


