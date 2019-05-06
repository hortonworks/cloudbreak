-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE instancegroup_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table if not exists instancegroup
(
  id bigint default nextval('instancegroup_id_seq'::regclass) not null
    constraint instancegroup_pkey
      primary key,
  groupname varchar(255),
  instancegrouptype varchar(255),
  nodecount integer,
  stack_id bigint not null
    constraint fk_instancegroup_stack_id
      references stack
      on delete cascade,
  template_id bigint
    constraint fk_instancegroup_template_id
      references template,
  securitygroup_id bigint
    constraint fk_securitygroupidininstancegroup
      references securitygroup,
  attributes text
);

create index if not exists instancegroup_stack_id
  on instancegroup (stack_id);

-- //@UNDO

DROP TABLE instancegroup;

DROP SEQUENCE instancegroup_id_seq;