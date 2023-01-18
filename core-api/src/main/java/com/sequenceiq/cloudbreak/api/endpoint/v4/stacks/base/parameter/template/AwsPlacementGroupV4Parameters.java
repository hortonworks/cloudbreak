package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;
import com.sequenceiq.common.api.placement.AwsPlacementGroupStrategy;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsPlacementGroupV4Parameters implements JsonEntity {

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
