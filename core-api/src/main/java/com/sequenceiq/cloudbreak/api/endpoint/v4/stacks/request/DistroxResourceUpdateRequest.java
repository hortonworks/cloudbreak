package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistroxResourceUpdateRequest extends ResourceUpdateRequest {

    @ValidCrn(resource = CrnResourceDescriptor.DATAHUB)
    public String getCrn() {
        return super.getCrn();
    }
}
