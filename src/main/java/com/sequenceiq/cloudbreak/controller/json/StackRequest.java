package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.controller.validation.ValidStackRequest;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidStackRequest
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackRequest extends StackBase {
    @ApiModelProperty(StackModelDescription.CONSUL_SERVER_COUNT_BY_USER)
    private Integer consulServerCount;

    public Integer getConsulServerCount() {
        return consulServerCount;
    }

    public void setConsulServerCount(Integer consulServerCount) {
        this.consulServerCount = consulServerCount;
    }
}
