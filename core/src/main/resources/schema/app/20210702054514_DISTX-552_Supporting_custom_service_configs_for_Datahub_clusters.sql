-- // DISTX-552 Supporting custom service configs for Datahub clusters
-- Migration SQL that makes the change goes here.
--  Name: custom_configs_id_seq, Type: Sequence
CREATE SEQUENCE IF NOT EXISTS custom_configs_id_seq;

ALTER SEQUENCE custom_configs_id_seq OWNER TO postgres;

--  Name: customconfigs, Type: Table, Tablespace:
CREATE TABLE IF NOT EXISTS customconfigs
(
    id                bigint default nextval('custom_configs_id_seq'::regclass)                   not null
        constraint customconfigs_pk
            primary key,
    name varchar(255)                                                                not null,
    resourcecrn       varchar(255)                                                                not null,
    created           bigint default (date_part('epoch'::text, now()) * (1000)::double precision) not null,
    lastmodified      bigint,
    platformversion   text,
    account           text                                                                        not null
);

ALTER TABLE customconfigs
    OWNER TO postgres;

CREATE UNIQUE INDEX IF NOT EXISTS customconfigs_resourcecrn_uindex
    ON customconfigs (resourcecrn);

CREATE UNIQUE INDEX IF NOT EXISTS customconfigs_name_account_uindex
    ON customconfigs (name, account);

--  Name: custom_config_property_id_seq, Type: Sequence
CREATE SEQUENCE IF NOT EXISTS custom_config_property_id_seq;

ALTER SEQUENCE custom_config_property_id_seq OWNER TO postgres;

-- Name: customconfigs_properties, Type: Table, Tablespace:
CREATE TABLE IF NOT EXISTS customconfigs_properties
(
    id               bigint default nextval('custom_config_property_id_seq'::regclass) not null
        constraint customconfigsentity_pk
            primary key,
    configname       text                                                          not null,
    configvalue      text                                                          not null,
    roletype         text,
    servicetype      text                                                          not null,
    customconfigs_id bigint
        constraint customconfigs_entity_customconfigs_id_fk
            references customconfigs
            on update cascade on delete cascade
);

ALTER TABLE customconfigs_properties
    OWNER TO postgres;

--  Alter Cluster table to add customconfigs_id column as foreign key
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS customconfigs_id bigint;
ALTER TABLE cluster ADD CONSTRAINT fk_cluster_customconfigs_id FOREIGN KEY (customconfigs_id) REFERENCES customconfigs;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE cluster DROP CONSTRAINT fk_cluster_customconfigs_id;
ALTER TABLE cluster DROP COLUMN IF EXISTS customconfigs_id;
DROP TABLE IF EXISTS customconfigs_properties;
DROP TABLE IF EXISTS customconfigs;
DROP SEQUENCE IF EXISTS custom_configs_id_seq;
DROP SEQUENCE IF EXISTS custom_config_property_id_seq;

