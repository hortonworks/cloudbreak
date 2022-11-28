-- // CB-19565 Create credential property to be able to skip organizational rule validation
-- Migration SQL that makes the change goes here.
ALTER TABLE credential ADD COLUMN IF NOT EXISTS skiporgpolicydecisions BOOLEAN DEFAULT FALSE;
UPDATE credential SET skiporgpolicydecisions = FALSE WHERE skiporgpolicydecisions IS NULL;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE credential DROP COLUMN IF EXISTS skiporgpolicydecisions;
