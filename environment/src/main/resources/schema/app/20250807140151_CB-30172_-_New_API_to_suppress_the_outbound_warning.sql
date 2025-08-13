-- // CB-30172 - New API to suppress the outbound warning
-- Migration SQL that makes the change goes here.
ALTER TABLE terms ADD COLUMN IF NOT EXISTS term_type CHARACTER VARYING (255);
UPDATE terms SET term_type='AZURE_MARKETPLACE_IMAGE_TERMS';
DROP INDEX IF EXISTS terms_accountid_idx;
CREATE UNIQUE INDEX IF NOT EXISTS terms_accountid_term_type_idx ON terms (accountid, term_type);

-- //@UNDO
-- SQL to undo the change goes here.

-- Drop the composite index first
DROP INDEX IF EXISTS terms_accountid_term_type_idx;

-- Handle potential duplicate accountid entries before creating the unique index
-- Keep only one record per accountid (the most recently created one)
DELETE FROM terms a
WHERE a.id < (
    SELECT MAX(b.id)
    FROM terms b
    WHERE a.accountid = b.accountid
);

-- Now create the unique index on accountid
CREATE UNIQUE INDEX IF NOT EXISTS terms_accountid_idx ON terms (accountid);

-- Finally drop the term_type column
ALTER TABLE terms DROP COLUMN term_type;
