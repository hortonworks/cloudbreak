-- // CB-4587 cluster template name modification
-- Migration SQL that makes the change goes here.

delete from clustertemplate where status='DEFAULT';

-- //@UNDO
-- SQL to undo the change goes here.


