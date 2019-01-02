package com.sequenceiq.cloudbreak.converter.v4.credentials;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.P12Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class MapToP12ParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, P12Parameters> {

    @Override
    public P12Parameters convert(Map<String, Object> source) {
        P12Parameters p12Parameters = new P12Parameters();
        p12Parameters.setServiceAccountPrivateKey((String) source.get("serviceAccountPrivateKey"));
        p12Parameters.setServiceAccountId((String) source.get("serviceAccountId"));
        p12Parameters.setProjectId((String) source.get("projectId"));
        return p12Parameters;
    }

}
