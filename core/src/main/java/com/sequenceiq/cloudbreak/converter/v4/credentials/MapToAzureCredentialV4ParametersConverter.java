package com.sequenceiq.cloudbreak.converter.v4.credentials;

import static java.util.Objects.isNull;

import java.security.InvalidParameterException;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AppBased;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.RoleBased;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class MapToAzureCredentialV4ParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, AzureCredentialV4Parameters> {

    private static final String INVALID_MESSAGE_FORMAT = "Unable to set which based is the actual Azure credential parameter, since %s";

    @Override
    public AzureCredentialV4Parameters convert(Map<String, Object> source) {
        AzureCredentialV4Parameters parameters = new AzureCredentialV4Parameters();
        parameters.setTenantId((String) source.get("tenantId"));
        parameters.setSubscriptionId((String) source.get("subscriptionId"));
        if (!isNull(source.get("accessKey")) && !isNull(source.get("roleName"))) {
            throw new InvalidParameterException(String.format(INVALID_MESSAGE_FORMAT, "both of them are set"));
        }
        if (!isNull(source.get("accessKey"))) {
            AppBased appBased = new AppBased();
            appBased.setAccessKey((String) source.get("accessKey"));
            appBased.setSecretKey((String) source.get("secretKey"));
            parameters.setAppBased(appBased);
        } else if (!isNull(source.get("roleName"))) {
            RoleBased roleBased = new RoleBased();
            roleBased.setRoleName((String) source.get("roleName"));
            parameters.setRoleBased(roleBased);
        } else {
            throw new InvalidParameterException(String.format(INVALID_MESSAGE_FORMAT, "none of them are set"));
        }
        return parameters;
    }

}
