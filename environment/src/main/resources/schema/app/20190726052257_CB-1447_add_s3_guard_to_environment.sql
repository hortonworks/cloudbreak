-- // add s3 guard to environment
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE environment_parameters_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;


CREATE TABLE IF NOT EXISTS environment_parameters
(
    id                  bigint NOT NULL
        CONSTRAINT environment_parameters_pkey
          PRIMARY KEY,
    name                varchar(255) NOT NULL,
    parameters_platform varchar(255) NOT NULL,
    environment_id      bigint NOT NULL,
    accountid           varchar(255) NOT NULL,
    s3guard_dynamo_table_name varchar(255),
    CONSTRAINT fk_environment_parameters_environment FOREIGN KEY (environment_id) REFERENCES environment(id)
);

CREATE INDEX IF NOT EXISTS environment_parameters_accountid_name_idx ON environment_network (accountid, name);
CREATE INDEX IF NOT EXISTS environment_parameters_environment_id_idx ON environment_network (environment_id);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS environment_network_environment_id_idx;
DROP INDEX IF EXISTS environment_network_accountid_name_idx;

ALTER TABLE ONLY environment_network DROP CONSTRAINT IF EXISTS environment_parameters_pkey;

DROP TABLE IF EXISTS environment_parameters;
