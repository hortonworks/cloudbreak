package com.sequenceiq.cloudbreak.template;

import java.util.HashSet;
import java.util.Set;

public enum HandleBarModelKey {

    COMPONENTS("components"),
    LDAP("ldap"),
    KERBEROS("kerberos"),
    GATEWAY("gateway"),
    RDS("rds"),
    FILESYSTEMCONFIGS("fileSystemConfigs"),
    SHAREDSERVICE("sharedService"),
    DATALAKE("datalake"),
    CLUSTER_DEFINITION("clusterdefinition"),
    HDF("hdf"),
    GENERAL("general"),
    STACK_VERSION("stack_version");

    private final String modelKey;

    HandleBarModelKey(String modelKey) {
        this.modelKey = modelKey;
    }

    public String modelKey() {
        return modelKey;
    }

    public static Set<String> modelKeys() {
        Set<String> result = new HashSet<>();
        for (HandleBarModelKey handleBarModelKey : HandleBarModelKey.values()) {
            result.add(handleBarModelKey.modelKey());
        }
        return result;
    }

}
