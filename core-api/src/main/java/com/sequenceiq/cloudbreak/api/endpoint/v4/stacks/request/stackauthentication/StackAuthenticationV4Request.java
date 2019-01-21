package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.stackauthentication;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackAuthenticationV4Base;

import io.swagger.annotations.ApiModel;

@ApiModel("StackAuthentication")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StackAuthenticationV4Request extends StackAuthenticationV4Base {
}
