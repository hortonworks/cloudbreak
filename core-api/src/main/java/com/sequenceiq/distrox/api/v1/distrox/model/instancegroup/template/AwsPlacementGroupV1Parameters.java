package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;
import com.sequenceiq.common.api.placement.AwsPlacementGroupStrategy;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsPlacementGroupV1Parameters implements Serializable {

    @Valid
    @Schema(description = TemplateModelDescription.AWS_PLACEMENT_GROUP_STRATEGY)
    private AwsPlacementGroupStrategy strategy;

    public AwsPlacementGroupStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(AwsPlacementGroupStrategy strategy) {
        this.strategy = strategy;
    }
}
