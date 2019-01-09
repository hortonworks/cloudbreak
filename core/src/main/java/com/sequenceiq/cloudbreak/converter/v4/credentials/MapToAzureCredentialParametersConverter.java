package com.sequenceiq.cloudbreak.converter.v4.credentials;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class MapToAzureCredentialParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, AzureCredentialV4Parameters> {

    @Override
    public AzureCredentialV4Parameters convert(Map<String, Object> source) {
        AzureCredentialV4Parameters parameters = new AzureCredentialV4Parameters();
        parameters.setTenantId((String) source.get("tenantId"));
        parameters.setSubscriptionId((String) source.get("subscriptionId"));
        parameters.setSecretKey((String) source.get("secretKey"));
        parameters.setAccessKey((String) source.get("accessKey"));
        return parameters;
    }

}
