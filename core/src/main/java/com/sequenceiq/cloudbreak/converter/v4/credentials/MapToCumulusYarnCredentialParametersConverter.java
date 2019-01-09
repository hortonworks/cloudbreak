package com.sequenceiq.cloudbreak.converter.v4.credentials;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.cumulus.CumulusYarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class MapToCumulusYarnCredentialParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, CumulusYarnCredentialV4Parameters> {

    @Override
    public CumulusYarnCredentialV4Parameters convert(Map<String, Object> source) {
        CumulusYarnCredentialV4Parameters parameters = new CumulusYarnCredentialV4Parameters();
        parameters.setAmbariPassword((String) source.get("cumulusAmbariPassword"));
        parameters.setAmbariUrl((String) source.get("cumulusAmbariUrl"));
        parameters.setAmbariUser((String) source.get("cumulusAmbariUser"));
        return parameters;
    }

}
