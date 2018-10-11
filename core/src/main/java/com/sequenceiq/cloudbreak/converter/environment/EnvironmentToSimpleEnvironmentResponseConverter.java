package com.sequenceiq.cloudbreak.converter.environment;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.environment.response.SimpleEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class EnvironmentToSimpleEnvironmentResponseConverter extends AbstractConversionServiceAwareConverter<Environment, SimpleEnvironmentResponse> {

    @Override
    public SimpleEnvironmentResponse convert(Environment source) {
        SimpleEnvironmentResponse response = new SimpleEnvironmentResponse();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(source.getRegionsSet());
        response.setCloudPlatform(source.getCloudPlatform());
        response.setCredentialName(source.getCredential().getName());
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        response.setLdapConfigs(source.getLdapConfigs().stream().map(LdapConfig::getName).collect(Collectors.toSet()));
        response.setProxyConfigs(source.getProxyConfigs().stream().map(ProxyConfig::getName).collect(Collectors.toSet()));
        response.setRdsConfigs(source.getRdsConfigs().stream().map(RDSConfig::getName).collect(Collectors.toSet()));
        return response;
    }
}
