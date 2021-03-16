package com.sequenceiq.authorization.info.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CheckRightOnResourcesV4Request {

    @NotNull
    private RightV4 right;

    @NotNull
    private List<String> resourceCrns;

    public RightV4 getRight() {
        return right;
    }

    public void setRight(RightV4 right) {
        this.right = right;
    }

    public List<String> getResourceCrns() {
        return resourceCrns;
    }

    public void setResourceCrns(List<String> resourceCrns) {
        this.resourceCrns = resourceCrns;
    }
}
