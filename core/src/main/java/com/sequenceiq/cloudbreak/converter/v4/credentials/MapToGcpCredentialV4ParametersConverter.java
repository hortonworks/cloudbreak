package com.sequenceiq.cloudbreak.converter.v4.credentials;

import java.security.InvalidParameterException;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.GcpCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.JsonParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.P12Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class MapToGcpCredentialV4ParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, GcpCredentialV4Parameters> {

    @Override
    public GcpCredentialV4Parameters convert(Map<String, Object> source) {
        GcpCredentialV4Parameters parameters = new GcpCredentialV4Parameters();
        if (source.get("credentialJson") != null) {
            parameters.setJson(getConversionService().convert(source, JsonParameters.class));
        } else if (source.get("serviceAccountId") != null) {
            parameters.setP12(getConversionService().convert(source, P12Parameters.class));
        } else {
            throw new InvalidParameterException("Unable to convert to GcpCredentialParameters since not able to decide it's type (json or p12)");
        }
        return parameters;
    }

}
