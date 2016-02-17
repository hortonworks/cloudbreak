-- // CLOUD-48264 More SSSD options
-- Migration SQL that makes the change goes here.

ALTER TABLE sssdconfig
    ADD COLUMN tlsreqcert character varying(255),
    ADD COLUMN adserver character varying(255),
    ADD COLUMN kerberosserver character varying(255),
    ADD COLUMN kerberosrealm character varying(255),
    ADD COLUMN configuration text,
    ALTER COLUMN providertype SET NOT NULL;

UPDATE sssdconfig SET tlsreqcert = 'NEVER';

ALTER TABLE sssdconfig
    ALTER COLUMN tlsreqcert SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sssdconfig
    DROP COLUMN reqcert,
    DROP COLUMN adserver,
    DROP COLUMN kerberosserver,
    DROP COLUMN kerberosrealm,
    DROP COLUMN sssdconfig_id;
