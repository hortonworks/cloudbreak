package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentResponseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetailedEnvironmentV4Response extends EnvironmentV4BaseResponse {
    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_RESOURCES)
    private Set<DatalakeResourcesV4Response> datalakeResourcesResponses;

    @ApiModelProperty(EnvironmentResponseModelDescription.WORKLOAD_CLUSTERS)
    private Set<StackViewV4Response> workloadClusters = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_CLUSTERS)
    private Set<StackViewV4Response> datalakeClusters = new HashSet<>();

    public Set<DatalakeResourcesV4Response> getDatalakeResources() {
        return datalakeResourcesResponses;
    }

    public void setDatalakeResources(Set<DatalakeResourcesV4Response> datalakeResources) {
        datalakeResourcesResponses = datalakeResources;
    }

    public Set<StackViewV4Response> getWorkloadClusters() {
        return workloadClusters;
    }

    public void setWorkloadClusters(Set<StackViewV4Response> workloadClusters) {
        this.workloadClusters = workloadClusters;
    }

    public Set<StackViewV4Response> getDatalakeClusters() {
        return datalakeClusters;
    }

    public void setDatalakeClusters(Set<StackViewV4Response> datalakeClusters) {
        this.datalakeClusters = datalakeClusters;
    }

}
