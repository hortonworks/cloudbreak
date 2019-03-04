package com.sequenceiq.cloudbreak.converter.v4.stacks.environment;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.EnvironmentSettingsV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;

@Component
public class EnvironmentViewToEnvironmentSettingsV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<EnvironmentView, EnvironmentSettingsV4Response> {

    @Override
    public EnvironmentSettingsV4Response convert(EnvironmentView source) {
        EnvironmentSettingsV4Response response = new EnvironmentSettingsV4Response();
        var credential = getConversionService().convert(source.getCredential(), CredentialV4Response.class);
        response.setCloudPlatform(credential.getCloudPlatform());
        response.setCredential(credential);
        response.setName(source.getName());
        return response;
    }

}
