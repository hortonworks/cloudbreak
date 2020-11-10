-- // CB-9647 initial nodecount must 0 by default
-- Migration SQL that makes the change goes here.

UPDATE instancegroup SET initialnodecount = 0 WHERE initialnodecount IS NULL;
ALTER TABLE instancegroup ALTER initialNodeCount SET DEFAULT 0;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancegroup ALTER initialNodeCount DROP DEFAULT;