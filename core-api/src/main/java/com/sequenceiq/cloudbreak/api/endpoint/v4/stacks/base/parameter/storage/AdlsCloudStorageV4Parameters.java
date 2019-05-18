package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidAdlsCloudStorageParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidAdlsCloudStorageParameters
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AdlsCloudStorageV4Parameters implements CloudStorageV4Parameters {

    @ApiModelProperty
    @NotNull
    private String accountName;

    @ApiModelProperty
    @NotNull
    private String clientId;

    @ApiModelProperty
    @NotNull
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

    @ApiModelProperty(hidden = true)
    @Override
    public FileSystemType getType() {
        return FileSystemType.ADLS;
    }

}