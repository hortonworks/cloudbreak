package com.sequenceiq.cloudbreak.blueprint.template;

public enum HandleBarModelKey {

    COMPONENTS("components"),
    LDAP("ldap"),
    GATEWAY("gateway"),
    RDS("rds"),
    FILESYSTEMCONFIGS("fileSystemConfigs"),
    SHAREDSERVICE("sharedService"),
    DATALAKE("datalake"),
    BLUEPRINT("blueprint"),
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
}
