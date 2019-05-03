-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE stack_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table stack
(
  id bigint default nextval('stack_id_seq'::regclass) not null
    constraint stack_pkey
      primary key,
  name varchar(255),
  region varchar(255),
  created bigint default (date_part('epoch'::text, now()) * (1000)::double precision),
  terminated bigint,
  platformvariant text,
  availabilityzone text,
  cloudplatform varchar(255),
  owner varchar(255),
  gatewayport integer default 9443,
  stackauthentication_id bigint
    constraint fk_stack_stackauthentication_id
      references stackauthentication,
  network_id bigint
    constraint fk_stack_network_id
      references network,
  credential_id bigint
    constraint fk_stack_credential_id
      references credential,
  securityconfig_id bigint
    constraint fk_stack_securityconfig_id
      references securityconfig,
  tags text,
  template text,
  stackstatus_id bigint
);

create unique index stack_id_idx
  on stack (id);

create index stack_name_idx
  on stack (name);

-- //@UNDO

DROP TABLE stack;

DROP SEQUENCE stack_id_seq;