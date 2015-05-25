-- // CLOUD-620 change gcc to gcp
-- Migration SQL that makes the change goes here.

UPDATE credential SET dtype='GcpCredential' WHERE dtype='GccCredential';

ALTER TABLE template RENAME COLUMN gccinstancetype TO gcpinstancetype;
ALTER TABLE template RENAME COLUMN gccrawdisktype TO gcprawdisktype;
UPDATE template SET dtype='GcpTemplate' WHERE dtype='GccTemplate';

UPDATE cloudbreakevent SET cloud='GCP' WHERE cloud='GCC';
UPDATE cloudbreakusage SET provider='GCP' WHERE provider='GCC';

UPDATE resource SET resourcetype='GCP_DISK' WHERE resourcetype='GCC_DISK';
UPDATE resource SET resourcetype='GCP_ATTACHED_DISK' WHERE resourcetype='GCC_ATTACHED_DISK';
UPDATE resource SET resourcetype='GCP_RESERVED_IP' WHERE resourcetype='GCC_RESERVED_IP';
UPDATE resource SET resourcetype='GCP_NETWORK' WHERE resourcetype='GCC_NETWORK';
UPDATE resource SET resourcetype='GCP_FIREWALL_IN' WHERE resourcetype='GCC_FIREWALL_IN';
UPDATE resource SET resourcetype='GCP_FIREWALL_INTERNAL' WHERE resourcetype='GCC_FIREWALL_INTERNAL';
UPDATE resource SET resourcetype='GCP_ROUTE' WHERE resourcetype='GCC_ROUTE';
UPDATE resource SET resourcetype='GCP_INSTANCE' WHERE resourcetype='GCC_INSTANCE';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE credential SET dtype='GccCredential' WHERE dtype='GcpCredential';

ALTER TABLE template RENAME COLUMN gcpinstancetype TO gccinstancetype;
ALTER TABLE template RENAME COLUMN gcprawdisktype TO gccrawdisktype;
UPDATE template SET dtype='GccTemplate' WHERE dtype='GcpTemplate';

UPDATE cloudbreakevent SET cloud='GCC' WHERE cloud='GCP';
UPDATE cloudbreakusage SET provider='GCC' WHERE provider='GCP';

UPDATE resource SET resourcetype='GCC_DISK' WHERE resourcetype='GCP_DISK';
UPDATE resource SET resourcetype='GCC_ATTACHED_DISK' WHERE resourcetype='GCP_ATTACHED_DISK';
UPDATE resource SET resourcetype='GCC_RESERVED_IP' WHERE resourcetype='GCP_RESERVED_IP';
UPDATE resource SET resourcetype='GCC_NETWORK' WHERE resourcetype='GCP_NETWORK';
UPDATE resource SET resourcetype='GCC_FIREWALL_IN' WHERE resourcetype='GCP_FIREWALL_IN';
UPDATE resource SET resourcetype='GCC_FIREWALL_INTERNAL' WHERE resourcetype='GCP_FIREWALL_INTERNAL';
UPDATE resource SET resourcetype='GCC_ROUTE' WHERE resourcetype='GCP_ROUTE';
UPDATE resource SET resourcetype='GCC_INSTANCE' WHERE resourcetype='GCP_INSTANCE';


