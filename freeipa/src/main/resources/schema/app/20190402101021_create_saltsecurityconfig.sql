-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE saltsecurityconfig_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table saltsecurityconfig
(
  id bigint not null
    constraint saltsecurityconfig_pkey
      primary key,
  saltpassword varchar(255),
  saltsignpublickey text,
  saltsignprivatekey text,
  saltbootpassword varchar(255),
  saltbootsignpublickey text,
  saltbootsignprivatekey text
);

-- //@UNDO

DROP TABLE saltsecurityconfig;

DROP SEQUENCE saltsecurityconfig_id_seq;