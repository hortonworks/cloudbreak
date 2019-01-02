package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4ParameterBase;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class MockInstanceTemplateV4Parameters extends InstanceTemplateV4ParameterBase {

}
