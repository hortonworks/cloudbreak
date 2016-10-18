package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("HostGroupResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroupResponse extends HostGroupBase {
    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;
    @ApiModelProperty(value = HostGroupModelDescription.METADATA)
    private Set<HostMetadataResponse> metadata = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<HostMetadataResponse> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<HostMetadataResponse> metadata) {
        this.metadata = metadata;
    }
}
