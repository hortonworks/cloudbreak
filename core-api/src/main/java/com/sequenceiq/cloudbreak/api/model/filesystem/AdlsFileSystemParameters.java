package com.sequenceiq.cloudbreak.api.model.filesystem;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AdlsFileSystemParameters implements FileSystemParameters {

    public static final String ACCOUNT_NAME = "accountName";

    public static final String CLIENT_ID = "clientId";

    public static final String CREDENTIAL = "credential";

    public static final String TENANT_ID = "tenantId";

    private static final int PARAMETER_QUANTITY = 4;

    @ApiModelProperty
    private String accountName;

    @ApiModelProperty
    private String clientId;

    @ApiModelProperty
    private String credential;

    @ApiModelProperty
    private String tenantId;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public FileSystemType getType() {
        return FileSystemType.ADLS;
    }

    @Override
    public Map<String, String> getAsMap() {
        Map<String, String> params = new LinkedHashMap<>(PARAMETER_QUANTITY);
        params.put(ACCOUNT_NAME, accountName);
        params.put(CLIENT_ID, clientId);
        params.put(CREDENTIAL, credential);
        params.put(TENANT_ID, tenantId);
        return params;
    }
}
