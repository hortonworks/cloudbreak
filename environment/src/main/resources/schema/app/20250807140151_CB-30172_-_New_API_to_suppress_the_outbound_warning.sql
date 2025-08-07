-- // CB-30172 - New API to suppress the outbound warning
-- Migration SQL that makes the change goes here.
ALTER TABLE terms ADD COLUMN IF NOT EXISTS term_type CHARACTER VARYING (255);
UPDATE terms SET term_type='AZURE_MARKETPLACE_IMAGE_TERMS';
DROP INDEX IF EXISTS terms_accountid_idx;
CREATE UNIQUE INDEX IF NOT EXISTS terms_accountid_term_type_idx ON terms (accountid, term_type);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE terms DROP COLUMN term_type;
CREATE UNIQUE INDEX IF NOT EXISTS terms_accountid_idx ON terms (accountid);
