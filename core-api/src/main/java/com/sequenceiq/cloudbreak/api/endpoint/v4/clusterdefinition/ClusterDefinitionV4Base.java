package com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterDefinitionModelDescription;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public abstract class ClusterDefinitionV4Base implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    @NotNull
    @Size(max = 100, min = 1, message = "The length of the cluster definition's name has to be in range of 1 to 100 and should not contain semicolon "
            + "and percentage character.")
    @Pattern(regexp = "^[^;\\/%]*$")
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(ClusterDefinitionModelDescription.CLUSTER_DEFINITION)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String clusterDefinition;

    @ApiModelProperty(ClusterDefinitionModelDescription.TAGS)
    private Map<String, Object> tags = new HashMap<>();

    public Map<String, Object> getTags() {
        return tags;
    }

    public void setTags(Map<String, Object> tags) {
        this.tags = tags;
    }

    public String getClusterDefinition() {
        return clusterDefinition;
    }

    public void setClusterDefinition(String clusterDefinition) {
        this.clusterDefinition = clusterDefinition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
