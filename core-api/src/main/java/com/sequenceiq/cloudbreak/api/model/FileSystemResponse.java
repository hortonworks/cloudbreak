package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FileSystem;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class FileSystemResponse extends FileSystemBase {

    @NotNull
    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(FileSystem.LOCATIONS)
    private Set<StorageLocationResponse> locations = new HashSet<>();

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<StorageLocationResponse> getLocations() {
        return locations;
    }

    public void setLocations(Set<StorageLocationResponse> locations) {
        this.locations = locations;
    }
}
