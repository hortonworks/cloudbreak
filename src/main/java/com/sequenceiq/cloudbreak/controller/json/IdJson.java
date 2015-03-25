package com.sequenceiq.cloudbreak.controller.json;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

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
