-- // CB-11572 datahub connected to the datalake without datalakeresource
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS datalakecrn varchar(255);

CREATE INDEX idx_stack_datalakecrn ON stack (datalakecrn);

UPDATE stack s
SET datalakecrn = dl.resourcecrn
FROM (
  SELECT s1.id, s2.resourcecrn FROM stack s1, datalakeresources d, stack s2 WHERE s1.type = 'WORKLOAD' AND s1.datalakeresourceid = d.id AND d.datalakestack_id = s2.id
) dl WHERE s.id = dl.id;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS datalakecrn;
