package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class StatusCrnsV4Request {

    @ApiModelProperty(ModelDescriptions.CRNS)
    private List<String> crns = new ArrayList<>();

    public List<String> getCrns() {
        return crns;
    }

    public void setCrns(List<String> crns) {
        this.crns = crns;
    }
}
