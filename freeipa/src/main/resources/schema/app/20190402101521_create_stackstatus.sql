-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE stackstatus_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table if not exists stackstatus
(
  id bigint default nextval('stackstatus_id_seq'::regclass) not null
    constraint pk_stackstatus
      primary key,
  created bigint default (date_part('epoch'::text, now()) * (1000)::double precision) not null,
  stack_id bigint,
  status varchar(255),
  detailedstackstatus varchar(255),
  statusreason text
);

create unique index stackstatus_id_idx
  on stackstatus (id);

create index if not exists stackstatus_stack_idx
  on stackstatus (stack_id);

create index if not exists stackstatus_status_idx
  on stackstatus (status);

-- //@UNDO

DROP TABLE stackstatus;

DROP SEQUENCE stackstatus_id_seq;