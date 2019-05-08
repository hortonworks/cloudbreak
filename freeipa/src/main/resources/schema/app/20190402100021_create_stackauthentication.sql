-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE stackauthentication_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table stackauthentication
(
  id bigint not null
    constraint stackauthentication_pkey
      primary key,
  publickey text,
  publickeyid varchar(255),
  loginusername varchar(255) not null
);



-- //@UNDO

DROP TABLE stackauthentication;

DROP SEQUENCE stackauthentication_id_seq;