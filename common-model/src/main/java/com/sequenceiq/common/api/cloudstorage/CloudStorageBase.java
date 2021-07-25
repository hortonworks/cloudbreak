package com.sequenceiq.common.api.cloudstorage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.cloudstorage.doc.CloudStorageModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CloudStorageBase implements Serializable {

    private AwsStorageParameters aws;

    private List<StorageLocationBase> locations = new ArrayList<>();

    private List<@Valid StorageIdentityBase> identities = new ArrayList<>();

    @ApiModelProperty(CloudStorageModelDescription.ACCOUNT_MAPPING)
    private AccountMappingBase accountMapping;

    public AwsStorageParameters getAws() {
        return aws;
    }

    public void setAws(AwsStorageParameters aws) {
        this.aws = aws;
    }

    public List<StorageLocationBase> getLocations() {
        return locations;
    }

    public void setLocations(List<StorageLocationBase> locations) {
        this.locations = locations;
    }

    public List<StorageIdentityBase> getIdentities() {
        return identities;
    }

    public void setIdentities(List<StorageIdentityBase> identities) {
        this.identities = identities;
    }

    public AccountMappingBase getAccountMapping() {
        return accountMapping;
    }

    public void setAccountMapping(AccountMappingBase accountMapping) {
        this.accountMapping = accountMapping;

    }

    public CloudStorageBase copy(CloudStorageBase other) {
        this.aws = other.aws;
        this.locations = other.locations;
        this.identities = other.identities;
        this.accountMapping = other.accountMapping;
        return this;
    }
}