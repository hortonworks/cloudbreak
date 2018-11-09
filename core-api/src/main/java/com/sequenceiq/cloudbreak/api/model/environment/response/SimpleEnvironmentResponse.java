package com.sequenceiq.cloudbreak.api.model.environment.response;

import java.util.Set;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentResponseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class SimpleEnvironmentResponse extends EnvironmentBaseResponse {

    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_CLUSTER_NAMES)
    private Set<String> datalakeClusterNames;

    @ApiModelProperty(EnvironmentResponseModelDescription.WORKLOAD_CLUSTER_NAMES)
    private Set<String> workloadClusterNames;

    public Set<String> getDatalakeClusterNames() {
        return datalakeClusterNames;
    }

    public void setDatalakeClusterNames(Set<String> datalakeClusterNames) {
        this.datalakeClusterNames = datalakeClusterNames;
    }

    public Set<String> getWorkloadClusterNames() {
        return workloadClusterNames;
    }

    public void setWorkloadClusterNames(Set<String> workloadClusterNames) {
        this.workloadClusterNames = workloadClusterNames;
    }
}
