-- // CLOUD-76776 support SmartSense and Flex subscriptions
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE smartsense_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE smartsensesubscription
(
   id               bigint PRIMARY KEY NOT NULL DEFAULT nextval('smartsense_id_seq'),
   subscriptionid   VARCHAR(255) NOT NULL,
   account          VARCHAR(255) NOT NULL,
   owner            VARCHAR(255) NOT NULL,
   publicinaccount  boolean NOT NULL
);

CREATE SEQUENCE flexsubscription_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE flexsubscription
(
   id                           bigint PRIMARY KEY NOT NULL DEFAULT nextval('flexsubscription_id_seq'),
   name                         VARCHAR(255) NOT NULL,
   subscriptionid               VARCHAR(255) NOT NULL,
   smartsensesubscription_id    bigint NOT NULL,
   account                      VARCHAR(255) NOT NULL,
   owner                        VARCHAR(255) NOT NULL,
   publicinaccount              boolean NOT NULL
);

ALTER TABLE flexsubscription
   ADD CONSTRAINT fk_flexsubscription_smartsensesubscription FOREIGN KEY (smartsensesubscription_id)
   REFERENCES smartsensesubscription (id);


ALTER TABLE stack ADD COLUMN flexsubscription_id bigint;

ALTER TABLE ONLY stack
    ADD CONSTRAINT fk_flexsubscription_smartsensesubscription FOREIGN KEY (flexsubscription_id) REFERENCES flexsubscription(id);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE ONLY stack DROP CONSTRAINT IF EXISTS fk_flexsubscription_stack;
ALTER TABLE stack DROP COLUMN IF EXISTS flexsubscription_id;

ALTER TABLE ONLY flexsubscription DROP CONSTRAINT IF EXISTS fk_flexsubscription_smartsensesubscription;
DROP TABLE IF EXISTS flexsubscription;
DROP SEQUENCE IF EXISTS flexsubscription_id_seq;


DROP TABLE IF EXISTS smartsensesubscription;
DROP SEQUENCE IF EXISTS smartsense_id_seq;

