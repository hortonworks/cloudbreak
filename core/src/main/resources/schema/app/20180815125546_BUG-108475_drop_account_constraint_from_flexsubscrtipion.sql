-- // BUG-108475 drop account constraint from flexsubscrtipion
-- Migration SQL that makes the change goes here.
ALTER TABLE flexsubscription ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE flexsubscription ALTER COLUMN account DROP NOT NULL;
ALTER TABLE flexsubscription DROP CONSTRAINT IF EXISTS uk_flexsubscription_account_name;
ALTER TABLE flexsubscription DROP CONSTRAINT IF EXISTS uk_flexsubscription_account_subscriptionid;

ALTER TABLE flexsubscription ADD CONSTRAINT flexsubscriptionid_in_org_unique UNIQUE (subscriptionid, organization_id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE flexsubscription DROP CONSTRAINT IF EXISTS flexsubscriptionid_in_org_unique;
