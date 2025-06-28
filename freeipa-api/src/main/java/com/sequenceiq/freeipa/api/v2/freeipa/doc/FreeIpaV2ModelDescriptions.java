package com.sequenceiq.freeipa.api.v2.freeipa.doc;

public class FreeIpaV2ModelDescriptions {

    public static final String INSTANCE_TO_REPAIR_FQDN = "The FQDN of the instance which will be restored. It has to match the backup provided.";

    public static final String FULL_BACKUP_STORAGE_PATH = "Cloud storage path, where the 'header' file and 'ipa-full.tar' file is available " +
            "and can be downloaded from. Header file host filed must match the hostname provided in 'instanceToRestoreFqdn'.";

    public static final String DATA_BACKUP_STORAGE_PATH = "Cloud storage path, where the 'header' file and 'ipa-data.tar' file is available " +
            "and can be downloaded from. Header file host filed must match the hostname provided in 'instanceToRestoreFqdn'.";

    public static final String FQDN = "Fully qualified domain name of the Active Directory server.";

    public static final String IP = "IP address of the Active Directory server.";

    public static final String REALM = "Realm of the Active Directory server.";

    public static final String TRUST_SECRET = "The trust shared secret is a password-like key entered during trust configuration that is shared between " +
            "the IDM and AD domains to secure and validate the trust relationship.";

    private FreeIpaV2ModelDescriptions() {
    }
}
