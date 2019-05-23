-- // CB-1466 create kerberos table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS kerberosconfig (
	id bigserial NOT NULL,
    resourcecrn varchar(255) NOT NULL,
    "name" varchar(255) NOT NULL,
    description text NULL,
    accountid varchar(255) NOT NULL,
    environmentid varchar(255) NOT NULL,
    archived bool NULL DEFAULT false,
    deletiontimestamp int8 NULL DEFAULT '-1'::integer,
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
    CONSTRAINT kerberosconfig_pkey PRIMARY KEY (id),
    CONSTRAINT uk_kerberosconfig_accountid_environmentid_resourcecrn_deletiondate UNIQUE (accountid, environmentid, resourcecrn, deletiontimestamp)
);
CREATE UNIQUE INDEX IF NOT EXISTS kerberosconfig_id_idx ON kerberosconfig USING btree (id);
CREATE INDEX IF NOT EXISTS idx_kerberosconfig_accountid_environmentid ON kerberosconfig USING btree (accountid, environmentid);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS kerberosconfig;
