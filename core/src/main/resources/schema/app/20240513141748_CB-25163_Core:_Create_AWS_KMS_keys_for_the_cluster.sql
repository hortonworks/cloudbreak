-- // CB-25163 Core: Create AWS KMS keys for the cluster
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS stackencryption_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table IF NOT EXISTS stackencryption
(
  id bigint not null
    constraint stackencryption_pkey
      primary key,
  stackId bigint not null
    constraint fk_stackencryption_stackid
      references stack
    constraint stackid_in_stackencryption_unique unique,
  encryptionKeyLuks varchar(255) not null,
  encryptionKeyCloudSecretManager varchar(255) not null,
  accountid varchar(255) not null
);

-- //@UNDO
-- SQL to undo the change goes here.
DROP TABLE IF EXISTS stackencryption;

DROP SEQUENCE IF EXISTS stackencryption_id_seq;





