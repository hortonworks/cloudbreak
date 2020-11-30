-- // CB-9580 new field on cluster: embedded db is configured on attached disk or not
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS embeddeddatabaseonattacheddisk boolean;
ALTER TABLE cluster ALTER COLUMN embeddeddatabaseonattacheddisk SET DEFAULT FALSE;
UPDATE cluster SET embeddeddatabaseonattacheddisk = FALSE WHERE embeddeddatabaseonattacheddisk IS NULL;
ALTER TABLE cluster ALTER COLUMN embeddeddatabaseonattacheddisk SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS embeddeddatabaseonattacheddisk;
