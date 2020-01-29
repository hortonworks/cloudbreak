-- // CB-5122 Create child_environment table

CREATE SEQUENCE IF NOT EXISTS childenvironment_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE IF NOT EXISTS childenvironment
(
  id bigint default nextval('childenvironment_id_seq'::regclass) not null
    constraint childenvironment_pkey
      primary key,
  stack_id bigint not null
    constraint fk_childenvironment_stack_id
      references stack,
  environmentcrn varchar(255) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS child_environment_crn_idx
  ON childenvironment (environmentcrn);

-- //@UNDO

DROP TABLE IF EXISTS childenvironment;

DROP SEQUENCE IF EXISTS childenvironment_id_seq;
