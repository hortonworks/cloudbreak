package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

public class WasbConfig extends CloudStorageConfig {

    private final String storageContainer;

    private final String account;

    private final boolean secure;

    public WasbConfig(String folderPrefix, String storageContainer, String account, boolean secure) {
        super(folderPrefix);
        this.storageContainer = storageContainer;
        this.account = account;
        this.secure = secure;
    }

    public String getStorageContainer() {
        return storageContainer;
    }

    public String getAccount() {
        return account;
    }

    public boolean isSecure() {
        return secure;
    }
}
