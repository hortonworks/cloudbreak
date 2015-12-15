package com.sequenceiq.cloudbreak.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("Id")
public class IdJson implements JsonEntity {

    @ApiModelProperty(required = true)
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
