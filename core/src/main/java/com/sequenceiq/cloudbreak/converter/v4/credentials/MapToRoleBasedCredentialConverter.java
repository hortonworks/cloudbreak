package com.sequenceiq.cloudbreak.converter.v4.credentials;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.RoleBasedCredentialParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

public class MapToRoleBasedCredentialConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, RoleBasedCredentialParameters> {

    @Override
    public RoleBasedCredentialParameters convert(Map<String, Object> source) {
        RoleBasedCredentialParameters roleBasedCredentialParameters = new RoleBasedCredentialParameters();
        roleBasedCredentialParameters.setRoleArn((String) source.get("roleArn"));
        return roleBasedCredentialParameters;
    }
}
