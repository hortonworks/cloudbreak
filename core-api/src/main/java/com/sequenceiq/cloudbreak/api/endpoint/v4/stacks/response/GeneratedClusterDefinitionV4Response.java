package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterDefinitionModelDescription;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class GeneratedClusterDefinitionV4Response implements JsonEntity {

    @ApiModelProperty(ClusterDefinitionModelDescription.CLUSTER_DEFINITION)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String clusterDefinitionText;

    public String getClusterDefinitionText() {
        return clusterDefinitionText;
    }

    public void setClusterDefinitionText(String clusterDefinitionText) {
        this.clusterDefinitionText = clusterDefinitionText;
    }
}
