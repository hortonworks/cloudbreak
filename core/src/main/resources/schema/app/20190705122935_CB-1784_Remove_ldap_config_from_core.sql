-- // CB-1784 Remove ldap config from core
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS ldapconfig_id;
ALTER TABLE datalakeresources DROP COLUMN IF EXISTS ldapconfig_id;

DROP TABLE IF EXISTS ldapconfig;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE IF NOT EXISTS ldapconfig (
	id bigserial NOT NULL,
	"name" varchar(255) NOT NULL,
	description text NULL,
	account varchar(255) NULL,
	"owner" varchar(255) NULL,
	publicinaccount bool NOT NULL DEFAULT false,
	serverhost varchar(255) NOT NULL,
	serverport int4 NOT NULL,
	binddn varchar(255) NOT NULL,
	bindpassword varchar(255) NOT NULL,
	usersearchbase varchar(255) NOT NULL,
	groupsearchbase varchar(255) NULL,
	usernameattribute varchar(255) NULL,
	"domain" varchar(255) NULL,
	protocol varchar(255) NOT NULL,
	directorytype varchar(63) NULL,
	userobjectclass text NULL,
	groupobjectclass text NULL,
	groupnameattribute text NULL,
	groupmemberattribute text NULL,
	admingroup text NULL,
	userdnpattern varchar(255) NULL,
	workspace_id int8 NULL,
	certificate text NULL,
	archived bool NULL DEFAULT false,
	deletiontimestamp int8 NULL DEFAULT '-1'::integer,
	CONSTRAINT ldapconfig_pkey PRIMARY KEY (id),
	CONSTRAINT uk_ldapconfig_deletiondate_workspace UNIQUE (name, deletiontimestamp, workspace_id),
	CONSTRAINT fk_ldapconfig_organization FOREIGN KEY (workspace_id) REFERENCES workspace(id)
);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS ldapconfig_id BIGINT REFERENCES ldapconfig(id);
ALTER TABLE datalakeresources ADD COLUMN IF NOT EXISTS ldapconfig_id BIGINT REFERENCES ldapconfig(id);

CREATE INDEX IF NOT EXISTS idx_ldapconfig_workspace_id_name ON ldapconfig USING btree (workspace_id, name);
CREATE UNIQUE INDEX IF NOT EXISTS ldapconfig_id_idx ON ldapconfig USING btree (id);
CREATE INDEX IF NOT EXISTS ldapconfig_name_idx ON ldapconfig USING btree (name);
CREATE INDEX IF NOT EXISTS ldapconfig_org_id_idx ON ldapconfig USING btree (workspace_id);
