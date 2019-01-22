package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateParameterV4Base;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class OpenStackInstanceTemplateParametersV4 extends InstanceTemplateParameterV4Base {

    @Override
    public Map<String, Object> asMap() {
        setPlatformType(CloudPlatform.OPENSTACK);
        return super.asMap();
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        setPlatformType(getPlatformType(parameters));
    }
}
