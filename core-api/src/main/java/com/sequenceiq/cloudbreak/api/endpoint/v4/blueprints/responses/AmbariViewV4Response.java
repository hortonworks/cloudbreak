package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class AmbariViewV4Response implements JsonEntity {

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT)
    private BlueprintV4ViewResponse blueprint;

    @ApiModelProperty(StackModelDescription.AMBARI_IP)
    private String serverIp;

    public BlueprintV4ViewResponse getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintV4ViewResponse blueprint) {
        this.blueprint = blueprint;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }
}
