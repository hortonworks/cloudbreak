-- // CB-1467 Init create ldap table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS ldapconfig (
    id bigserial NOT NULL,
    resourcecrn varchar(255) NOT NULL,
    "name" varchar(255) NOT NULL,
    description text NULL,
    accountid varchar(255) NOT NULL,
    environmentid varchar(255) NOT NULL,
    archived bool NULL DEFAULT false,
    deletiontimestamp int8 NULL DEFAULT '-1'::integer,
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
    certificate text NULL,
    CONSTRAINT ldapconfig_pkey PRIMARY KEY (id),
    CONSTRAINT uk_ldapconfig_accountid_environmentid_resourcecrn_deletiondate UNIQUE (accountid, environmentid, resourcecrn, deletiontimestamp)
);

CREATE UNIQUE INDEX IF NOT EXISTS ldapconfig_id_idx ON ldapconfig USING btree (id);
CREATE INDEX IF NOT EXISTS idx_ldapconfig_accountid_environmentid ON ldapconfig USING btree (accountid, environmentid);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS ldapconfig;
