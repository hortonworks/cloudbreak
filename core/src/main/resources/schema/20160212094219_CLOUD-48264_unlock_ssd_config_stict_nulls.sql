-- // CLOUD-48264 Unlock ssd config stict nulls
-- Migration SQL that makes the change goes here.

ALTER TABLE sssdconfig
    ALTER COLUMN providertype DROP NOT NULL,
    ALTER COLUMN url DROP NOT NULL,
    ALTER COLUMN ldapschema DROP NOT NULL,
    ALTER COLUMN basesearch DROP NOT NULL,
    ALTER COLUMN tlsreqcert DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sssdconfig
    ALTER COLUMN providertype SET NOT NULL,
    ALTER COLUMN url SET NOT NULL,
    ALTER COLUMN ldapschema SET NOT NULL,
    ALTER COLUMN basesearch SET NOT NULL,
    ALTER COLUMN tlsreqcert SET NOT NULL;
