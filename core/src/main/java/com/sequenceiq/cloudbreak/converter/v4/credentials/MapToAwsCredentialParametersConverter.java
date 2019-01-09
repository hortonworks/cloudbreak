package com.sequenceiq.cloudbreak.converter.v4.credentials;

import java.security.InvalidParameterException;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.KeyBasedCredentialParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.RoleBasedCredentialParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class MapToAwsCredentialParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, AwsCredentialV4Parameters> {

    @Override
    public AwsCredentialV4Parameters convert(Map<String, Object> source) {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        parameters.setGovCloud((Boolean) source.get("govCloud"));
        if (source.get("accessKey") != null) {
            parameters.setKeyBasedCredentialParameters(getConversionService().convert(source, KeyBasedCredentialParameters.class));
        } else if (source.get("roleArn") != null) {
            parameters.setRoleBasedCredentialParameters(getConversionService().convert(source, RoleBasedCredentialParameters.class));
        } else {
            throw new InvalidParameterException("Unable to decide between Key and Role base credential properties because of missing required parameters");
        }
        return parameters;
    }

}
