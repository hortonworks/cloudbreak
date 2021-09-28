package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RefreshClusterRecipeV4Base;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachRecipeV4Response extends RefreshClusterRecipeV4Base {

    @Override
    public String toString() {
        return "AttachRecipeV4Response{} " + super.toString();
    }
}
