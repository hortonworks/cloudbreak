-- // CLOUD-601 migration script for network abstraction
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE network_table
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

CREATE TABLE network
(
   id                  bigint NOT NULL DEFAULT nextval('network_table'),
   dtype               CHARACTER VARYING (31) NOT NULL,
   account             CHARACTER VARYING (255),
   description         text,
   name                CHARACTER VARYING (255) NOT NULL,
   owner               CHARACTER VARYING (255),
   publicinaccount     boolean NOT NULL,
   status              CHARACTER VARYING (255),
   subnetcidr          CHARACTER VARYING (255) NOT NULL,
   addressprefixcidr   CHARACTER VARYING (255),
   internetgatewayid   CHARACTER VARYING (255),
   vpcid               CHARACTER VARYING (255),
   publicnetid         CHARACTER VARYING (255)
);

ALTER TABLE network ADD COLUMN stack_id bigint;

ALTER TABLE ONLY network
ADD CONSTRAINT network_pkey PRIMARY KEY (id);


ALTER TABLE ONLY network
ADD CONSTRAINT uk_networknameinaccount UNIQUE (account, name);



ALTER TABLE stack ADD COLUMN network_id bigint;

ALTER TABLE ONLY stack
ADD CONSTRAINT fk_networkidinstack FOREIGN KEY (network_id) REFERENCES network(id);

INSERT INTO network (account,
                     owner,
                     stack_id,
                     vpcid,
                     internetgatewayid,
                     subnetcidr,
                     name,
                     dtype,
                     status,
                     publicinaccount)
   SELECT s.account AS account,
          s.owner AS owner,
          sp1.stack_id AS stack_id,
          sp1.value AS vpcid,
          sp2.value AS internetgatewayid,
          sp3.value AS subnetcidr,
          s.name || '_net' AS name,
          'AwsNetwork' AS dtype,
          'USER_MANAGED' AS status,
          FALSE AS publicinaccount
     FROM stack_parameters sp1
          JOIN stack_parameters sp2
             ON sp1.stack_id = sp2.stack_id AND sp2.key = 'internetGatewayId'
          JOIN stack_parameters sp3
             ON sp1.stack_id = sp3.stack_id AND sp3.key = 'subnetCIDR'
          JOIN stack s
             ON     s.id = sp1.stack_id
                AND sp1.key = 'vpcId'
                AND s.status != 'DELETE_COMPLETED';


UPDATE stack s SET network_id = NULL;

UPDATE stack s SET network_id = n.id FROM network n WHERE s.id = n.stack_id;

ALTER TABLE network DROP COLUMN stack_id;

ALTER TABLE template DROP COLUMN publicnetid;

DROP TABLE IF EXISTS stack_parameters;

-- //@UNDO
-- SQL to undo the change goes here.


