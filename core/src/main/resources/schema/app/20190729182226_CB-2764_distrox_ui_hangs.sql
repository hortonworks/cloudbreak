-- // CB-2764 distrox ui hangs
-- Migration SQL that makes the change goes here.

DELETE FROM clustertemplate where status='DEFAULT';

-- //@UNDO
-- SQL to undo the change goes here.


