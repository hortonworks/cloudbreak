package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class DashFileSystemConfiguration extends FileSystemConfiguration {

    @NotNull
    @Pattern(regexp = "^[a-z0-9]{3,24}$",
            message = "Must contain only numbers and lowercase letters and must be between 3 and 24 characters long.")
    private String accountName;
    @NotNull
    @Pattern(regexp = "^([A-Za-z0-9+/]{4}){21}([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$",
            message = "Must be the base64 encoded representation of 64 random bytes.")
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
