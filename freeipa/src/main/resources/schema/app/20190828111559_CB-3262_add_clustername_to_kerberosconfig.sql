-- // CB-3262_add_clustername_to_kerberosconfig

ALTER TABLE kerberosconfig
    ADD COLUMN clustername VARCHAR(255);

ALTER TABLE kerberosconfig DROP CONSTRAINT IF EXISTS uk_kerberosconfig_accountid_environmentid_resourcecrn_deletiond;

ALTER TABLE kerberosconfig ADD CONSTRAINT uk_kerberosconfig_accid_envcrn_resourcecrn_deletiontimestamp_clustername
        unique (accountid, environmentcrn, resourcecrn, deletiontimestamp,  clustername);

 -- //@UNDO

 ALTER TABLE kerberosconfig
    DROP COLUMN IF EXISTS clustername;

ALTER TABLE kerberosconfig DROP CONSTRAINT IF EXISTS uk_kerberosconfig_accid_envcrn_resourcecrn_deletiontimestamp_clustername;

ALTER TABLE kerberosconfig ADD CONSTRAINT uk_kerberosconfig_accountid_environmentid_resourcecrn_deletiond
    unique (accountid, environmentcrn, resourcecrn, deletiontimestamp);
