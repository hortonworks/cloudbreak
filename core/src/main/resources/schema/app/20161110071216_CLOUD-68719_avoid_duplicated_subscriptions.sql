-- // CLOUD-68719_avoid_duplicated_subscriptions
-- Migration SQL that makes the change goes here.

DELETE FROM subscription s WHERE id IN (SELECT id FROM subscription WHERE clientid = s.clientid AND endpoint = s.endpoint OFFSET 1);

ALTER TABLE subscription ADD CONSTRAINT unq_subscription_clientid_endpoint UNIQUE(clientid, endpoint);

CREATE INDEX idx_subscription_clientid_endpoint ON subscription (clientid, endpoint);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE subscription DROP CONSTRAINT unq_subscription_clientid_endpoint;

DROP INDEX idx_subscription_clientid_endpoint;