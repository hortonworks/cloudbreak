package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.BaseSecurity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DescribeFreeIpaSecurityV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityResponse extends BaseSecurity {

    @Override
    public String toString() {
        return "DescribeFreeIpaRequest.DescribeFreeIpaSecurityV1Response{" + super.toString() + '}';
    }
}
