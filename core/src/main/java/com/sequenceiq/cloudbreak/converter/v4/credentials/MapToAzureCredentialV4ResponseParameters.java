package com.sequenceiq.cloudbreak.converter.v4.credentials;

import static java.util.Objects.isNull;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4ResponseParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.RoleBasedResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class MapToAzureCredentialV4ResponseParameters extends AbstractConversionServiceAwareConverter<Map<String, Object>,
        AzureCredentialV4ResponseParameters> {

    private static final String INVALID_MESSAGE_FORMAT = "Unable to set which based is the actual Azure credential parameter, since %s";

    @Override
    public AzureCredentialV4ResponseParameters convert(Map<String, Object> source) {
        AzureCredentialV4ResponseParameters parameters = new AzureCredentialV4ResponseParameters();
        parameters.setTenantId((String) source.get("tenantId"));
        parameters.setSubscriptionId((String) source.get("subscriptionId"));
        parameters.setAccessKey((String) source.get("accessKey"));
        convertRoleBasedIfSourceContainsRequiredParams(source, parameters);
        return parameters;
    }

    private void convertRoleBasedIfSourceContainsRequiredParams(Map<String, Object> source, AzureCredentialV4ResponseParameters parameters) {
        if (!isNull(source.get("appObjectId")) || !isNull(source.get("roleType"))) {
            RoleBasedResponse roleBased = new RoleBasedResponse();
            roleBased.setRoleName((String) source.get("roleType"));
            roleBased.setAppObjectId((String) source.get("appObjectId"));
            roleBased.setSpDisplayName((String) source.get("spDisplayName"));
            if (source.get("codeGrantFlow") != null && source.get("codeGrantFlow") instanceof Boolean) {
                roleBased.setCodeGrantFlow((Boolean) source.get("codeGrantFlow"));
            }
            parameters.setRoleBased(roleBased);
        }
    }

}
