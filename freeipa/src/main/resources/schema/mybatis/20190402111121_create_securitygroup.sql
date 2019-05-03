-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE securitygroup_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table if not exists securitygroup
(
  id bigint default nextval('securitygroup_id_seq'::regclass) not null
    constraint securitygroup_pkey
      primary key,
  securitygroupid varchar(255),
  cloudplatform varchar(255),
  name varchar(255)
);

create table if not exists securitygroup_securitygroupids
(
  securitygroup_id bigint not null,
  securitygroupid_value text
);

create unique index if not exists securitygroup_id_idx
  on securitygroup (id);

create index if not exists securitygroup_ids_idx
  on securitygroup_securitygroupids (securitygroup_id);

-- //@UNDO

DROP TABLE securitygroup;

DROP TABLE securitygroup_securitygroupids;

DROP SEQUENCE securitygroup_id_seq;