package com.sequenceiq.cloudbreak.api.model.filesystem;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class WasbFileSystemParameters implements FileSystemParameters {

    public static final String ACCOUNT_KEY = "accountKey";

    public static final String ACCOUNT_NAME = "accountName";

    @ApiModelProperty
    private String accountKey;

    @ApiModelProperty
    private String accountName;

    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public FileSystemType getType() {
        return FileSystemType.WASB;
    }

    @Override
    public Map<String, String> getAsMap() {
        Map<String, String> params = new LinkedHashMap<>(2);
        params.put(ACCOUNT_KEY, accountKey);
        params.put(ACCOUNT_NAME, accountName);
        return params;
    }
}
