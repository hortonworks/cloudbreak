package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostMetadataModelDescription.STATE;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostMetadataModelDescription.STATUS_REASON;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.ID;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.NAME;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class HostMetadataViewV4Response implements JsonEntity {

    @ApiModelProperty(value = NAME, required = true)
    private String name;

    @ApiModelProperty(ID)
    private Long id;

    @ApiModelProperty(STATE)
    private String state;

    @ApiModelProperty(STATUS_REASON)
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
