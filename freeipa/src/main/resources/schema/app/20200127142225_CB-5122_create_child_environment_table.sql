-- // CB-5122 Create child_environment table

CREATE SEQUENCE IF NOT EXISTS child_environment_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE IF NOT EXISTS child_environment
(
  id bigint default nextval('child_environment_id_seq'::regclass) not null
    constraint child_environment_pkey
      primary key,
  parent_environment_crn varchar(255),
  child_environment_crn varchar(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS child_environment_crn_idx
  ON child_environment (child_environment_crn);

-- //@UNDO

DROP TABLE IF EXISTS child_environment;

DROP SEQUENCE IF EXISTS child_environment_id_seq;
