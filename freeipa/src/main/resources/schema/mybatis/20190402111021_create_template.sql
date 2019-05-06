-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE template_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table if not exists template
(
  id bigint default nextval('template_id_seq'::regclass) not null
    constraint template_pkey
      primary key,
  deleted boolean,
  description text,
  name varchar(255),
  volumecount integer,
  volumesize integer,
  instancetype varchar(255),
  volumetype varchar(255),
  status varchar(255),
  attributes text,
  rootvolumesize integer,
  secretattributes text
);

create unique index if not exists template_id_idx
  on template (id);

create index if not exists template_name_idx
  on template (name);

-- //@UNDO

DROP TABLE template;

DROP SEQUENCE template_id_seq;