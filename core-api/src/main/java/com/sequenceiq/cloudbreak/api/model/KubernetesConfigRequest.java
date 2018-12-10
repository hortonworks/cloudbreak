package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.KubernetesConfig;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesConfigRequest extends KubernetesConfigBase {

    @ApiModelProperty(KubernetesConfig.CONFIG)
    @NotNull
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String config;

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
