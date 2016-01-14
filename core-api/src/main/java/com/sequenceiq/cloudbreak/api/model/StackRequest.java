package com.sequenceiq.cloudbreak.api.model;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackRequest extends StackBase {
    @ApiModelProperty(StackModelDescription.CONSUL_SERVER_COUNT_BY_USER)
    private Integer consulServerCount;
    @Valid
    @ApiModelProperty(StackModelDescription.ORCHESTRATOR)
    private OrchestratorRequest orchestrator;


    public Integer getConsulServerCount() {
        return consulServerCount;
    }

    public void setConsulServerCount(Integer consulServerCount) {
        this.consulServerCount = consulServerCount;
    }

    public OrchestratorRequest getOrchestrator() {
        return orchestrator;
    }

    public void setOrchestrator(OrchestratorRequest orchestrator) {
        this.orchestrator = orchestrator;
    }

}
