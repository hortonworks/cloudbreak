-- // CB-6366 update terminated in stack for DELETE_COMPLETED state
-- Migration SQL that makes the change goes here.

UPDATE stack s SET terminated = 0 FROM stackstatus ss WHERE s.terminated = -1 AND s.stackstatus_id = ss.id AND ss.status = 'DELETE_COMPLETED';

-- //@UNDO
-- SQL to undo the change goes here.


