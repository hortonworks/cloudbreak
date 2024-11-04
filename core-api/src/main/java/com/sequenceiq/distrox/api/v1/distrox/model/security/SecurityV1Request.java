package com.sequenceiq.distrox.api.v1.distrox.model.security;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SecurityV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityV1Request extends BaseSecurityV1 {

    @Override
    public String toString() {
        return "DistroXV1Request.SecurityV1Request{" +
                super.toString() +
                '}';
    }
}
