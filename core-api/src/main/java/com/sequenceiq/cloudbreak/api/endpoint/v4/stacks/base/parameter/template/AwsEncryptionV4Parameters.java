package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.EncryptionParametersV4Base;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsEncryptionV4Parameters extends EncryptionParametersV4Base {

}
