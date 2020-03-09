package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

public class AdlsGen2BackupConfig extends CloudBackupStorageConfig {

    private final String fileSystem;

    private final String account;

    public AdlsGen2BackupConfig(String folderPrefix, String fileSystem, String account) {
        super(folderPrefix);
        this.fileSystem = fileSystem;
        this.account = account;
    }

    public String getFileSystem() {
        return fileSystem;
    }

    public String getAccount() {
        return account;
    }
}
