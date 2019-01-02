package com.sequenceiq.cloudbreak.converter.v4.credentials;

import static com.sequenceiq.cloudbreak.converter.v4.credentials.ParameterMapToClassConverterUtil.exec;

import java.security.InvalidParameterException;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsSelectorType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.KeyBasedCredentialParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.RoleBasedCredentialParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class MapToAwsCredentialV4ParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, AwsCredentialV4Parameters> {

    private static final String UNABLE_TO_CHOOSE_MESSAGE = "Unable to decide between Key and Role base credential properties because of missing "
            + "required parameters";

    @Override
    public AwsCredentialV4Parameters convert(Map<String, Object> source) {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        parameters.setGovCloud((Boolean) source.get("govCloud"));
        String selector = (String) source.get("selector");
        if (selector != null) {
            if (AwsSelectorType.KEY_BASED.getName().equals(selector)) {
                parameters.setKeyBasedCredentialParameters(exec(() -> new Json(source).get(KeyBasedCredentialParameters.class),
                        KeyBasedCredentialParameters.class));
            } else if (AwsSelectorType.ROLE_BASED.getName().equals(selector)) {
                parameters.setRoleBasedCredentialParameters(exec(() -> new Json(source).get(RoleBasedCredentialParameters.class),
                        RoleBasedCredentialParameters.class));
            } else {
                throw new InvalidParameterException(UNABLE_TO_CHOOSE_MESSAGE);
            }
        } else {
            throw new InvalidParameterException(UNABLE_TO_CHOOSE_MESSAGE);
        }
        return parameters;
    }

}
