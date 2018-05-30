package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("FileSystem")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class FileSystemRequest extends FileSystemBase {

    @ApiModelProperty(ModelDescriptions.FileSystem.LOCATIONS)
    private Set<StorageLocationRequest> locations = new HashSet<>();

    public Set<StorageLocationRequest> getLocations() {
        return locations;
    }

    public void setLocations(Set<StorageLocationRequest> locations) {
        this.locations = locations;
    }
}
