-- // CB-29042 Add ccmv2 secret rotation to a scheduler
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS dynamicentitlement_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE IF NOT EXISTS dynamicentitlement
(
  id BIGINT DEFAULT nextval('dynamicentitlement_id_seq'::regclass) NOT NULL
    CONSTRAINT dynamicentitlement_pkey
      PRIMARY KEY,
  entitlement VARCHAR(255),
  entitlementvalue BOOLEAN,
  stack_id BIGINT NOT NULL
    CONSTRAINT fk_dynamicentitlement_stack_id
      REFERENCES stack(id)
      ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dynamicentitlement_stack_id
  ON dynamicentitlement (stack_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_dynamicentitlement_stack_id;
DROP TABLE IF EXISTS dynamicentitlement;
DROP SEQUENCE IF EXISTS dynamicentitlement_id_seq;

