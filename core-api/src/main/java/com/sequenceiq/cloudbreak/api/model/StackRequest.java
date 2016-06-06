package com.sequenceiq.cloudbreak.api.model;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackRequest extends StackBase {
    @Valid
    @ApiModelProperty(StackModelDescription.ORCHESTRATOR)
    private OrchestratorRequest orchestrator;
    @ApiModelProperty(value = StackModelDescription.AMBARI_VERSION)
    private String ambariVersion;
    @ApiModelProperty(value = StackModelDescription.HDP_VERSION)
    private String hdpVersion;

    public OrchestratorRequest getOrchestrator() {
        return orchestrator;
    }

    public void setOrchestrator(OrchestratorRequest orchestrator) {
        this.orchestrator = orchestrator;
    }

    public String getAmbariVersion() {
        return ambariVersion;
    }

    public void setAmbariVersion(String ambariVersion) {
        this.ambariVersion = ambariVersion;
    }

    public String getHdpVersion() {
        return hdpVersion;
    }

    public void setHdpVersion(String hdpVersion) {
        this.hdpVersion = hdpVersion;
    }
}
