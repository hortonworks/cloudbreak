package com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class AmbariViewV4Response implements JsonEntity {

    @ApiModelProperty(ClusterModelDescription.CLUSTER_DEFINITION)
    private ClusterDefinitionV4ViewResponse clusterDefinition;

    @ApiModelProperty(StackModelDescription.AMBARI_IP)
    private String serverIp;

    public ClusterDefinitionV4ViewResponse getClusterDefinition() {
        return clusterDefinition;
    }

    public void setClusterDefinition(ClusterDefinitionV4ViewResponse clusterDefinition) {
        this.clusterDefinition = clusterDefinition;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }
}
