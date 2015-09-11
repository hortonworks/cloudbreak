package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.dash;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfiguration;

public class DashFileSystemConfiguration extends FileSystemConfiguration {

    @NotNull
    private String accountName;
    @NotNull
    private String accountKey;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }
}
