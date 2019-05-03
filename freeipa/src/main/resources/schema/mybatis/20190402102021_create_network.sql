-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE network_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table if not exists network
(
  id bigint default nextval('network_id_seq'::regclass) not null
    constraint network_pkey
      primary key,
  name varchar(255),
  attributes text,
  cloudplatform varchar(255)
);

create unique index network_id_idx
  on network (id);

-- //@UNDO

DROP TABLE network;

DROP SEQUENCE network_id_seq;