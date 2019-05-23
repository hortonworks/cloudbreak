package com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.location.StorageLocationV1Request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CloudStorageV1Request extends CloudStorageV1Base {

    @Valid
    @ApiModelProperty(ClusterModelDescription.LOCATIONS)
    private Set<StorageLocationV1Request> locations = new HashSet<>();

    public Set<StorageLocationV1Request> getLocations() {
        return locations;
    }

    public void setLocations(Set<StorageLocationV1Request> locations) {
        this.locations = locations;
    }
}
