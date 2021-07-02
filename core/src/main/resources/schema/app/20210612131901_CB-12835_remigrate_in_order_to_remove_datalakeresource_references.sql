-- // CB-12835 rerunning migration in order to remove datalakeresource reference
-- Migration SQL that makes the change goes here.

UPDATE stack s
SET datalakecrn = dl.resourcecrn
FROM (
  SELECT s1.id, s2.resourcecrn FROM stack s1, datalakeresources d, stack s2 WHERE s1.type = 'WORKLOAD' AND s1.datalakeresourceid = d.id AND d.datalakestack_id = s2.id
) dl WHERE s.id = dl.id;

-- //@UNDO
-- SQL to undo the change goes here.
