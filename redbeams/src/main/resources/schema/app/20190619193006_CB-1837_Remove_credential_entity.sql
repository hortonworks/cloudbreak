-- // CB-1837 Remove credential entity
-- Migration SQL that makes the change goes here.

DROP TABLE credential;

DROP SEQUENCE credential_id_seq;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE SEQUENCE credential_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table if not exists credential
(
  id bigint default nextval('credential_id_seq'::regclass) not null
    constraint credential_pkey
      primary key,
  name varchar(255),
  attributes text
);

create unique index if not exists credential_id_idx
  on credential (id);

