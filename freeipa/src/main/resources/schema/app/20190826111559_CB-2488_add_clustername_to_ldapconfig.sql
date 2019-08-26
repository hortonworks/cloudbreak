-- // CB-2488_add_clustername_to_ldapconfig

ALTER TABLE ldapconfig
    ADD COLUMN clustername VARCHAR(255);

ALTER TABLE ldapconfig DROP CONSTRAINT IF EXISTS uk_ldapconfig_accountid_environmentid_resourcecrn_deletiondate;

ALTER TABLE ldapconfig ADD CONSTRAINT uk_ldapconfig_accid_envcrn_resourcecrn_deletiontimestamp_clustername
        unique (accountid, environmentcrn, resourcecrn, deletiontimestamp,  clustername);

 -- //@UNDO

 ALTER TABLE ldapconfig
    DROP COLUMN IF EXISTS clustername;

ALTER TABLE ldapconfig DROP CONSTRAINT IF EXISTS uk_ldapconfig_accid_envcrn_resourcecrn_deletiontimestamp_clustername;

ALTER TABLE ldapconfig ADD CONSTRAINT uk_ldapconfig_accountid_environmentid_resourcecrn_deletiondate
    unique (accountid, environmentcrn, resourcecrn, deletiontimestamp);
