package com.sequenceiq.cloudbreak.cloud.model.filesystem;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({ @JsonSubTypes.Type(value = CloudAdlsGen2View.class, name = "cloudAdlsGen2View"),
        @JsonSubTypes.Type(value = CloudAdlsView.class, name = "cloudAdlsView"),
        @JsonSubTypes.Type(value = CloudEfsView.class, name = "cloudEfsView"),
        @JsonSubTypes.Type(value = CloudGcsView.class, name = "cloudGcsView"),
        @JsonSubTypes.Type(value = CloudS3View.class, name = "cloudS3View"),
        @JsonSubTypes.Type(value = CloudWasbView.class, name = "cloudWasbView") })
public abstract class CloudFileSystemView {

    private final CloudIdentityType cloudIdentityType;

    private AccountMappingBase accountMapping;

    private List<StorageLocationBase> locations;

    @JsonCreator
    protected CloudFileSystemView(@JsonProperty("cloudIdentityType") CloudIdentityType cloudIdentityType) {
        this.cloudIdentityType = cloudIdentityType;
    }

    public CloudIdentityType getCloudIdentityType() {
        return cloudIdentityType;
    }

    public void setAccountMapping(AccountMappingBase accountMapping) {
        this.accountMapping = accountMapping;
    }

    public AccountMappingBase getAccountMapping() {
        return accountMapping;
    }

    public void setLocations(List<StorageLocationBase> locations) {
        this.locations = locations;
    }

    public List<StorageLocationBase> getLocations() {
        return locations;
    }
}
