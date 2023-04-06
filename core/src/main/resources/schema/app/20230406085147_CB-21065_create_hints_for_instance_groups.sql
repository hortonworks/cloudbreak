-- // CB-20941 Slow query LegacyPagingStructuredEventRepository. findByEventTypeAndResourceTypeAndResourceId, due to missing index
-- Migration SQL that makes the change goes here.

ALTER TABLE instancegroup ADD COLUMN IF NOT EXISTS hints VARCHAR(10) NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancegroup DROP COLUMN IF EXISTS hints;