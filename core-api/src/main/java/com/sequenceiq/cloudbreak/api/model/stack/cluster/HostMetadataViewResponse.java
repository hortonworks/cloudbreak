package com.sequenceiq.cloudbreak.api.model.stack.cluster;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostMetadataModelDescription.STATE;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.ID;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.NAME;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class HostMetadataViewResponse implements JsonEntity {

    @ApiModelProperty(value = NAME, required = true)
    private String name;

    @ApiModelProperty(ID)
    private Long id;

    @ApiModelProperty(STATE)
    private String state;

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
}
