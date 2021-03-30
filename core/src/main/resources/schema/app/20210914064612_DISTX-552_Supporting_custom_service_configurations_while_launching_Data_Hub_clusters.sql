-- // DISTX-552 Supporting custom service configurations while launching Data Hub clusters
-- Migration SQL that makes the change goes here.
CREATE SEQUENCE IF NOT EXISTS custom_configurations_id_seq;

CREATE TABLE IF NOT EXISTS customconfigurations
(
    id                bigint default nextval('custom_configurations_id_seq'::regclass)            NOT NULL
                      CONSTRAINT customconfigurations_pkey
                      PRIMARY KEY,
    name              varchar(255)                                                                NOT NULL,
    crn               varchar(255)                                                                NOT NULL,
    created           bigint default (date_part('epoch'::text, now()) * (1000)::double precision) NOT NULL,
    runtimeversion    varchar(255),
    account           varchar(255)                                                                NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_customconfigs_crn
    ON customconfigurations (crn);

CREATE UNIQUE INDEX IF NOT EXISTS idx_customconfigs_name_account
    ON customconfigurations (name, account);

CREATE SEQUENCE IF NOT EXISTS custom_configuration_property_id_seq;

CREATE TABLE IF NOT EXISTS customconfigurations_properties
(
    id                      bigint default nextval('custom_configuration_property_id_seq'::regclass) NOT NULL
                            CONSTRAINT customconfigurationproperty_pkey
                            PRIMARY KEY,
    name                    text                                                                     NOT NULL,
    value                   text                                                                     NOT NULL,
    roletype                text,
    servicetype             text                                                                     NOT NULL,
    customconfigurations_id bigint
                            CONSTRAINT fk_customconfiguration_property_customconfigurations_id
                            REFERENCES customconfigurations
                            ON UPDATE CASCADE ON DELETE CASCADE
);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS customconfigurations_id bigint;
ALTER TABLE cluster ADD CONSTRAINT fk_cluster_customconfigurations_id FOREIGN KEY (customconfigurations_id) REFERENCES customconfigurations;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE cluster DROP CONSTRAINT IF EXISTS fk_cluster_customconfigurations_id;
ALTER TABLE cluster DROP COLUMN IF EXISTS customconfigurations_id;
DROP TABLE IF EXISTS customconfigurations_properties;
DROP TABLE IF EXISTS customconfigurations;
DROP SEQUENCE IF EXISTS custom_configurations_id_seq;
DROP SEQUENCE IF EXISTS custom_configuration_property_id_seq;

