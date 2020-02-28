-- // CB-5839
-- Migration SQL that makes the change goes here.

UPDATE sdxcluster SET runtime = '7.0.2' where runtime is null and clustershape != 'CUSTOM';

-- //@UNDO
-- SQL to undo the change goes here.
