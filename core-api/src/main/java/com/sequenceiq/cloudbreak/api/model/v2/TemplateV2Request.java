package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateV2Request implements JsonEntity {

    @ApiModelProperty(TemplateModelDescription.VOLUME_COUNT)
    private Integer volumeCount;

    @ApiModelProperty(TemplateModelDescription.VOLUME_SIZE)
    private Integer volumeSize;

    @ApiModelProperty(TemplateModelDescription.PARAMETERS)
    private Map<String, Object> parameters = new HashMap<>();

    @ApiModelProperty(TemplateModelDescription.VOLUME_TYPE)
    private String volumeType;

    @NotNull
    @ApiModelProperty(value = TemplateModelDescription.INSTANCE_TYPE, required = true)
    private String instanceType;

    public Integer getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(Integer volumeSize) {
        this.volumeSize = volumeSize;
    }

    public Integer getVolumeCount() {
        return volumeCount;
    }

    public void setVolumeCount(Integer volumeCount) {
        this.volumeCount = volumeCount;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }



}
