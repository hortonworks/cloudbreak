package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CloudStorageV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.location.StorageLocationV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CloudStorageV4Response extends CloudStorageV4Base {

    @NotNull
    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @Valid
    @ApiModelProperty(ClusterModelDescription.LOCATIONS)
    private Set<StorageLocationV4Response> locations = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<StorageLocationV4Response> getLocations() {
        return locations;
    }

    public void setLocations(Set<StorageLocationV4Response> locations) {
        this.locations = locations;
    }
}
