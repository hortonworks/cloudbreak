-- // CLOUD-57789 added clustertemplate
-- Migration SQL that makes the change goes here.

CREATE TABLE clustertemplate (
    id bigint NOT NULL,
    account character varying(255),
    name character varying(255) NOT NULL,
    template text,
    owner character varying(255),
    type character varying(255),
    publicinaccount boolean NOT NULL
);

ALTER TABLE ONLY clustertemplate ADD CONSTRAINT clustertemplate_pkey PRIMARY KEY (id);


ALTER TABLE ONLY clustertemplate ADD CONSTRAINT uk_clustertemplate_account_name UNIQUE (account, name);

CREATE SEQUENCE clustertemplate_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE clustertemplate;
DROP SEQUENCE clustertemplate_id_seq;

