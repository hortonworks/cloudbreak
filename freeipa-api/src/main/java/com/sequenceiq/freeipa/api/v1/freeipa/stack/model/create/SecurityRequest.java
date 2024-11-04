package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateFreeIpaV1SecurityRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityRequest extends BaseSecurity {

    @Override
    public String toString() {
        return "CreateFreeIpaRequest.Security{" + super.toString() + '}';
    }
}
