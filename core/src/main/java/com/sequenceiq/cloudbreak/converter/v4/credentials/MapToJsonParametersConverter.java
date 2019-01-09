package com.sequenceiq.cloudbreak.converter.v4.credentials;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.JsonParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class MapToJsonParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, JsonParameters> {

    @Override
    public JsonParameters convert(Map<String, Object> source) {
        JsonParameters jsonParameters = new JsonParameters();
        jsonParameters.setCredentialJson((String) source.get("credentialJson"));
        return jsonParameters;
    }

}
