package com.sequenceiq.cloudbreak.api.model.stack.cluster;


import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.ID;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.NAME;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class HostGroupViewResponse implements JsonEntity {
    @ApiModelProperty(ID)
    private Long id;

    @ApiModelProperty(value = NAME, required = true)
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
