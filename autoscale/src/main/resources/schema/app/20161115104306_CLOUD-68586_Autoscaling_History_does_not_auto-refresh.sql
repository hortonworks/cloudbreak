-- // CLOUD-68586 Autoscaling History does not auto-refresh
-- Migration SQL that makes the change goes here.

CREATE TABLE subscription (
    id bigint NOT NULL,
    clientid character varying(255),
    endpoint character varying(255)
);

CREATE SEQUENCE subscription_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE subscription
   ALTER COLUMN id SET DEFAULT nextval ('subscription_id_seq');

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS subscription;
DROP SEQUENCE IF EXISTS subscription_id_seq;
