package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.BaseSecurityV4;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SecurityV4Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityV4Request extends BaseSecurityV4 {

    @Override
    public String toString() {
        return "StackV4Request.SecurityV4Request{" +
                super.toString() +
                '}';
    }
}
