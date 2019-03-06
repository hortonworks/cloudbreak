package com.sequenceiq.cloudbreak.api.model.stack.cluster;


import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription.METADATA;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.ID;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.NAME;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class HostGroupViewResponse implements JsonEntity {
    @ApiModelProperty(ID)
    private Long id;

    @ApiModelProperty(value = NAME, required = true)
    private String name;

    @ApiModelProperty(METADATA)
    private Set<HostMetadataViewResponse> metadata = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<HostMetadataViewResponse> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<HostMetadataViewResponse> metadata) {
        this.metadata = metadata;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
