-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE securityconfig_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table securityconfig
(
  id bigint default nextval('securityconfig_id_seq'::regclass) not null
    constraint securityconfig_pkey
      primary key,
  clientkey text,
  clientcert text,
  useprivateiptotls boolean default false not null,
  saltsecurityconfig_id bigint
    constraint fk_securityconfig_saltsecurityconfig_id
      references saltsecurityconfig
);

-- //@UNDO

DROP TABLE securityconfig;

DROP SEQUENCE securityconfig_id_seq;