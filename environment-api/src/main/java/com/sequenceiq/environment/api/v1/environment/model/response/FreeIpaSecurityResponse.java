package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.environment.api.v1.environment.model.base.FeeIpaBaseSecurity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaSecurityV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaSecurityResponse extends FeeIpaBaseSecurity {

    @Override
    public String toString() {
        return "FreeIpaSecurityV1Response{" + super.toString() + '}';
    }
}
