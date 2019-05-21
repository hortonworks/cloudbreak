package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UsersyncModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("GroupV1")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Group {

    @NotNull
    @ApiModelProperty(value = UsersyncModelDescriptions.GROUP_NAME, required = true)
    private String name;

    public String getName() {
        return name;
    }
}