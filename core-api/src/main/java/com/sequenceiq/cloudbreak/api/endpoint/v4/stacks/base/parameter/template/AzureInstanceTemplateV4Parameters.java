package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4ParameterBase;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureInstanceTemplateV4Parameters extends InstanceTemplateV4ParameterBase {

    @ApiModelProperty(TemplateModelDescription.AZURE_PRIVATE_ID)
    private String privateId;

    public String getPrivateId() {
        return privateId;
    }

    public void setPrivateId(String privateId) {
        this.privateId = privateId;
    }

    @Override
    public Map<String, Object> asMap() {
        setPlatformType(CloudPlatform.AZURE);
        return super.asMap();
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        privateId = getParameterOrNull(parameters, "privateId");
        setPlatformType(getPlatformType(parameters));
    }
}
