package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("Id")
public class IdJson implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.ID, required = true)
    private Long id;

    public IdJson() {

    }

    public IdJson(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
