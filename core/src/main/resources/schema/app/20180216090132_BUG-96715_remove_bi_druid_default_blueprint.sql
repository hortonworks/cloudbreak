-- // BUG-96715_remove_bi_druid_default_blueprint
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name = 'BI: Druid (Technical Preview)';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name = 'BI: Druid (Technical Preview)';