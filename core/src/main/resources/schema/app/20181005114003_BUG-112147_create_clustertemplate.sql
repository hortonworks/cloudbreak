-- // BUG-112147 create clustertemplate
-- Migration SQL that makes the change goes here.

DROP TABLE IF EXISTS clustertemplate;

CREATE SEQUENCE IF NOT EXISTS clustertemplate_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE  IF NOT EXISTS clustertemplate (
  id bigint PRIMARY KEY DEFAULT nextval('clustertemplate_id_seq'),
  name character varying(255) NOT NULL,
  description TEXT,
  template TEXT,
  workspace_id bigint,
  status character varying(255),
  cloudplatform character varying(255) NOT NULL
);

ALTER TABLE ONLY clustertemplate ADD CONSTRAINT uk_clustertemplate_workspace_id_name UNIQUE (workspace_id, name);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS clustertemplate;

DROP SEQUENCE IF EXISTS clustertemplate_id_seq;

-- we test rollback, so we must be backward compatible and this table already existed
CREATE TABLE clustertemplate
(
  id bigint NOT NULL,
  account character varying(255) NOT NULL,
  name character varying(255) NOT NULL,
  template text,
  owner character varying(255) NOT NULL,
  type character varying(255),
  publicinaccount boolean NOT NULL DEFAULT false,
  workspace_id bigint,
  CONSTRAINT clustertemplate_pkey PRIMARY KEY (id),
  CONSTRAINT clustertemplatename_in_org_unique UNIQUE (name, workspace_id),
  CONSTRAINT uk_clustertemplate_account_name UNIQUE (account, name),
  CONSTRAINT fk_clustertemplate_organization FOREIGN KEY (workspace_id)
  REFERENCES workspace (id) MATCH SIMPLE
  ON UPDATE NO ACTION
  ON DELETE NO ACTION
)