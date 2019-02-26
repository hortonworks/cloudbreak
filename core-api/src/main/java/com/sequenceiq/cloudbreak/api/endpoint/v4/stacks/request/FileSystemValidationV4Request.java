package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsGen2CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.GcsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.S3CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.WasbCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FileSystem;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class FileSystemValidationV4Request implements JsonEntity {

    @Valid
    @ApiModelProperty(FileSystem.LOCATIONS)
    private Set<StorageLocationV4Request> locations = new HashSet<>();

    @NotNull
    @ApiModelProperty(value = FileSystem.NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = FileSystem.TYPE, required = true)
    private String type;

    @ApiModelProperty(FileSystem.DEFAULT)
    private boolean defaultFs;

    @Valid
    @ApiModelProperty
    private AdlsCloudStorageV4Parameters adls;

    @Valid
    @ApiModelProperty
    private WasbCloudStorageV4Parameters wasb;

    @Valid
    @ApiModelProperty
    private GcsCloudStorageV4Parameters gcs;

    @Valid
    @ApiModelProperty
    private S3CloudStorageV4Parameters s3;

    @Valid
    @ApiModelProperty
    private AdlsGen2CloudStorageV4Parameters adlsGen2;

    public Set<StorageLocationV4Request> getLocations() {
        return locations;
    }

    public void setLocations(Set<StorageLocationV4Request> locations) {
        this.locations = locations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDefaultFs() {
        return defaultFs;
    }

    public void setDefaultFs(boolean defaultFs) {
        this.defaultFs = defaultFs;
    }

    public AdlsCloudStorageV4Parameters getAdls() {
        return adls;
    }

    public void setAdls(AdlsCloudStorageV4Parameters adls) {
        this.adls = adls;
    }

    public WasbCloudStorageV4Parameters getWasb() {
        return wasb;
    }

    public void setWasb(WasbCloudStorageV4Parameters wasb) {
        this.wasb = wasb;
    }

    public GcsCloudStorageV4Parameters getGcs() {
        return gcs;
    }

    public void setGcs(GcsCloudStorageV4Parameters gcs) {
        this.gcs = gcs;
    }

    public S3CloudStorageV4Parameters getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageV4Parameters s3) {
        this.s3 = s3;
    }

    public AdlsGen2CloudStorageV4Parameters getAdlsGen2() {
        return adlsGen2;
    }

    public void setAdlsGen2(AdlsGen2CloudStorageV4Parameters adlsGen2) {
        this.adlsGen2 = adlsGen2;
    }
}
