-- // CB-1784 Remove kerberos config from core
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS kerberosconfig_id;
ALTER TABLE datalakeresources DROP COLUMN IF EXISTS kerberosconfig_id;

DROP TABLE IF EXISTS kerberosconfig;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE IF NOT EXISTS kerberosconfig (
	id bigserial NOT NULL,
	kerberosadmin varchar(255) NULL,
	kerberospassword varchar(255) NULL,
	kerberosurl varchar(255) NULL,
	kerberosrealm varchar(255) NULL,
	kerberosprincipal varchar(255) NULL,
	kerberosldapurl varchar(255) NULL,
	kerberoscontainerdn varchar(255) NULL,
	kerberostcpallowed bool NULL DEFAULT false,
	kerberosdescriptor text NULL,
	krb5conf text NULL,
	kdcadminurl varchar(255) NULL,
	"type" varchar(255) NULL,
	verifykdctrust bool NULL DEFAULT true,
	"domain" varchar(255) NULL,
	nameservers varchar(255) NULL,
	workspace_id int8 NOT NULL,
	"name" varchar(255) NOT NULL,
	description text NULL,
	archived bool NULL DEFAULT false,
	deletiontimestamp int8 NULL DEFAULT '-1'::integer,
	CONSTRAINT kerberosconfig_pkey PRIMARY KEY (id),
	CONSTRAINT uk_kerberosconfig_deletiondate_workspace UNIQUE (name, deletiontimestamp, workspace_id),
	CONSTRAINT fk_kerberosconfig_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id)
);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS kerberosconfig_id BIGINT REFERENCES kerberosconfig(id);
ALTER TABLE datalakeresources ADD COLUMN IF NOT EXISTS kerberosconfig_id BIGINT REFERENCES kerberosconfig(id);

CREATE INDEX IF NOT EXISTS kerberosconfig_workspace_id_idx ON kerberosconfig USING btree (workspace_id);
