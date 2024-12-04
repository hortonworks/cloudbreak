package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views;


import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription.METADATA;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.ID;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.NAME;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroupViewV4Response implements JsonEntity {
    @Schema(description = ID)
    private Long id;

    @Schema(description = NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = METADATA, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<HostMetadataViewV4Response> metadata = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<HostMetadataViewV4Response> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<HostMetadataViewV4Response> metadata) {
        this.metadata = metadata;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
