package com.sequenceiq.cloudbreak.fluent.cloud;

public class WasbConfig extends CloudStorageConfig {

    private final String storageContainer;

    private final String account;

    public WasbConfig(String folderPrefix, String storageContainer, String account) {
        super(folderPrefix);
        this.storageContainer = storageContainer;
        this.account = account;
    }

    public String getStorageContainer() {
        return storageContainer;
    }

    public String getAccount() {
        return account;
    }
}
