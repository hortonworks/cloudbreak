package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostMetadataModelDescription.STATE;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostMetadataModelDescription.STATUS_REASON;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.ID;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.NAME;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HostMetadataViewV4Response implements JsonEntity {

    @Schema(description = NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = ID)
    private Long id;

    @Schema(description = STATE)
    private String state;

    @Schema(description = STATUS_REASON)
    private String statusReason;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }
}
