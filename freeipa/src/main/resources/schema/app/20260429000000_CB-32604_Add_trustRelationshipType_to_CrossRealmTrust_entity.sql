-- // CB-XXXXX Add trustRelationshipType field to CrossRealmTrust entity
-- Migration SQL that makes the change goes here.

ALTER TABLE crossrealmtrust ADD COLUMN IF NOT EXISTS trustrelationshiptype VARCHAR(255);

UPDATE crossrealmtrust
SET trustrelationshiptype = CASE
    WHEN truststatus = 'TRUST_ACTIVE'                   THEN 'TWO_WAY'
    WHEN truststatus = 'TRUST_SETUP_FINISH_REQUIRED'    THEN 'ONE_WAY'
    WHEN truststatus = 'TRUST_SETUP_FINISH_IN_PROGRESS' THEN 'ONE_WAY'
    ELSE 'UNKNOWN'
END;

ALTER TABLE crossrealmtrust ALTER COLUMN trustrelationshiptype SET DEFAULT 'UNKNOWN';
ALTER TABLE crossrealmtrust ALTER COLUMN trustrelationshiptype SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE crossrealmtrust DROP COLUMN IF EXISTS trustrelationshiptype;

