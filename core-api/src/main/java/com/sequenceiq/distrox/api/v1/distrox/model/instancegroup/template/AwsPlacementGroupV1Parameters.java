package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;
import com.sequenceiq.common.api.placement.AwsPlacementGroupStrategy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsPlacementGroupV1Parameters implements Serializable {

    @Valid
    @ApiModelProperty(value = TemplateModelDescription.AWS_PLACEMENT_GROUP_STRATEGY, allowableValues = "NONE, PARTITION, SPREAD, CLUSTER")
    private AwsPlacementGroupStrategy strategy;

    public AwsPlacementGroupStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(AwsPlacementGroupStrategy strategy) {
        this.strategy = strategy;
    }
}
