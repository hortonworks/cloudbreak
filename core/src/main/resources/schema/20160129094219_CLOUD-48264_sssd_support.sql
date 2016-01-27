-- // CLOUD-48264 Sssd suport
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE sssdconfig_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE sssdconfig (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    account character varying(255),
    owner character varying(255),
    publicinaccount boolean NOT NULL,
    providertype character varying(255),
    url character varying(255),
    ldapschema character varying(255),
    basesearch text
);

ALTER TABLE sssdconfig
    ADD CONSTRAINT PK_sssdconfig PRIMARY KEY (id),
    ADD CONSTRAINT uk_sssdconfig_account_name UNIQUE (account, name),
    ALTER COLUMN id SET DEFAULT nextval ('sssdconfig_id_seq');

ALTER TABLE cluster
    ADD COLUMN ldaprequired boolean,
    ADD COLUMN sssdconfig_id bigint,
    ADD CONSTRAINT fk_cluster_sssdconfig_id FOREIGN KEY (sssdconfig_id) REFERENCES sssdconfig (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster
    DROP COLUMN ldaprequired,
    DROP COLUMN sssdconfig_id;

DROP TABLE sssdconfig;

DROP SEQUENCE sssdconfig_id_seq;
