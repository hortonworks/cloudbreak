-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE securityrule_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table if not exists securityrule
(
  id bigint default nextval('securityrule_id_seq'::regclass) not null
    constraint securityrule_pkey
      primary key,
  securitygroup_id bigint not null
    constraint fk_securitygroupidinsecurityrule
      references securitygroup,
  cidr varchar(255),
  ports text,
  protocol varchar(255),
  modifiable boolean
);

-- //@UNDO

DROP TABLE securityrule;

DROP SEQUENCE securityrule_id_seq;