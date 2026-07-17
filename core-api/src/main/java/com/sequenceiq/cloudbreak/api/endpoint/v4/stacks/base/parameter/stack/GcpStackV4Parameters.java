package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RAZ_AUTHENTICATION_TYPE;

import java.util.Map;

import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GcpStackV4Parameters extends StackV4ParameterBase {

    @Pattern(regexp = "^(CAB|HMAC)$",
            message = "The RAZ authentication type must be either 'CAB' or 'HMAC'.")
    @Schema(description = "RAZ authentication type for GCS (CAB or HMAC)")
    private String razAuthenticationType;

    public String getRazAuthenticationType() {
        return razAuthenticationType;
    }

    public void setRazAuthenticationType(String razAuthenticationType) {
        this.razAuthenticationType = razAuthenticationType;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, RAZ_AUTHENTICATION_TYPE, razAuthenticationType);
        return map;
    }

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        razAuthenticationType = getParameterOrNull(parameters, RAZ_AUTHENTICATION_TYPE);
    }
}
