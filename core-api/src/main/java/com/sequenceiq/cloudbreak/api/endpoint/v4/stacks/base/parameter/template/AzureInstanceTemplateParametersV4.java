package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateParameterV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureInstanceTemplateParametersV4 extends InstanceTemplateParameterV4Base {

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
    public <T> T toClass(Map<String, Object> parameters) {
        AzureInstanceTemplateParametersV4 ret = new AzureInstanceTemplateParametersV4();
        ret.privateId = getParameterOrNull(parameters, "privateId");
        ret.setPlatformType(getPlatformType(parameters));
        return (T) ret;
    }
}
