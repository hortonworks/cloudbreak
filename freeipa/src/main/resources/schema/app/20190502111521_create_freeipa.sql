-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE freeipa_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table if not exists freeipa
(
  id bigint default nextval('freeipa_id_seq'::regclass) not null
    constraint freeipa_pkey
      primary key,
  stack_id bigint not null
    constraint fk_instancegroup_stack_id
      references stack,
  hostname varchar(255),
  domain varchar(255),
  adminpassword text
);

create unique index if not exists freeipa_id_idx
  on freeipa (id);

create unique index if not exists freeipa_stackid_idx
  on freeipa (stack_id);

-- //@UNDO

DROP TABLE freeipa;

DROP SEQUENCE freeipa_id_seq;