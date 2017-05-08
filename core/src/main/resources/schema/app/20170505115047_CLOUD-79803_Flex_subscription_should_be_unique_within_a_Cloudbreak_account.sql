-- // CLOUD-79803 Flex subscription should be unique within a Cloudbreak account
-- Migration SQL that makes the change goes here.

ALTER TABLE ONLY flexsubscription ADD CONSTRAINT uk_flexsubscription_account_subscriptionid UNIQUE (subscriptionid);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY flexsubscription DROP CONSTRAINT IF EXISTS uk_flexsubscription_account_subscriptionid;
