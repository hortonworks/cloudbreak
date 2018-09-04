package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.WasbCloudStorageParameters;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CloudStorageRequest implements JsonEntity {

    @Valid
    @ApiModelProperty
    private AdlsCloudStorageParameters adls;

    @Valid
    @ApiModelProperty
    private WasbCloudStorageParameters wasb;

    @Valid
    @ApiModelProperty
    private GcsCloudStorageParameters gcs;

    @Valid
    @ApiModelProperty
    private S3CloudStorageParameters s3;

    @Valid
    @ApiModelProperty
    private AbfsCloudStorageParameters abfs;

    @Valid
    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.LOCATIONS)
    private Set<StorageLocationRequest> locations = new HashSet<>();

    public AdlsCloudStorageParameters getAdls() {
        return adls;
    }

    public void setAdls(AdlsCloudStorageParameters adls) {
        this.adls = adls;
    }

    public WasbCloudStorageParameters getWasb() {
        return wasb;
    }

    public void setWasb(WasbCloudStorageParameters wasb) {
        this.wasb = wasb;
    }

    public GcsCloudStorageParameters getGcs() {
        return gcs;
    }

    public void setGcs(GcsCloudStorageParameters gcs) {
        this.gcs = gcs;
    }

    public S3CloudStorageParameters getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageParameters s3) {
        this.s3 = s3;
    }

    public Set<StorageLocationRequest> getLocations() {
        return locations;
    }

    public void setLocations(Set<StorageLocationRequest> locations) {
        this.locations = locations;
    }

    public AbfsCloudStorageParameters getAbfs() {
        return abfs;
    }

    public void setAbfs(AbfsCloudStorageParameters abfs) {
        this.abfs = abfs;
    }
}
