package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

public class AdlsGen2Config extends CloudStorageConfig {

    private final String fileSystem;

    private final String account;

    private final boolean secure;

    public AdlsGen2Config(String folderPrefix, String fileSystem, String account, boolean secure) {
        super(folderPrefix);
        this.fileSystem = fileSystem;
        this.account = account;
        this.secure = secure;
    }

    public String getFileSystem() {
        return fileSystem;
    }

    public String getAccount() {
        return account;
    }

    public boolean isSecure() {
        return secure;
    }
}
