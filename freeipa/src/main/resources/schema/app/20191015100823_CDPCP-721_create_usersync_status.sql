-- // CDPCP-721 Track usersync status

CREATE SEQUENCE IF NOT EXISTS usersyncstatus_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE IF NOT EXISTS usersyncstatus
(
  id bigint default nextval('usersyncstatus_id_seq'::regclass) not null
    constraint usersyncstatus_pkey
      primary key,
  stack_id bigint not null
    constraint fk_usersyncstatus_id
      references stack,
  umseventgenerationids text
);

UPDATE usersyncstatus SET umseventgenerationids='{}';

CREATE UNIQUE INDEX IF NOT EXISTS usersyncstatus_id_idx
  on usersyncstatus (id);

CREATE INDEX IF NOT EXISTS usersyncstatus_freeipaid_idx
  on usersyncstatus (stack_id);

-- //@UNDO

DROP TABLE usersyncstatus;

DROP SEQUENCE usersyncstatus_id_seq;
