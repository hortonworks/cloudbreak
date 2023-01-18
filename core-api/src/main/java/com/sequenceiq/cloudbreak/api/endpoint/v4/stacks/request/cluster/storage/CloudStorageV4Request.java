package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.common.api.cloudstorage.CloudStorageV1Base;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CloudStorageV4Request extends CloudStorageV1Base {

    @Valid
    @Schema(description = ClusterModelDescription.LOCATIONS)
    private Set<StorageLocationV4Request> locations = new HashSet<>();

    public Set<StorageLocationV4Request> getLocations() {
        return locations;
    }

    public void setLocations(Set<StorageLocationV4Request> locations) {
        this.locations = locations;
    }
}
