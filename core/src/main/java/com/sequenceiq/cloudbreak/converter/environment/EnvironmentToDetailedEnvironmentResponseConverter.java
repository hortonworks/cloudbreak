package com.sequenceiq.cloudbreak.converter.environment;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.environment.Environment;

@Component
public class EnvironmentToDetailedEnvironmentResponseConverter extends AbstractConversionServiceAwareConverter<Environment, DetailedEnvironmentResponse> {

    @Override
    public DetailedEnvironmentResponse convert(Environment source) {
        DetailedEnvironmentResponse response = new DetailedEnvironmentResponse();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(source.getRegionsSet());
        response.setCloudPlatform(source.getCloudPlatform());
        response.setCredential(getConversionService().convert(source.getCredential(), CredentialResponse.class));
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        response.setLdapConfigs(source.getLdapConfigs().stream()
                .map(ldap -> getConversionService().convert(ldap, LdapConfigResponse.class))
                .collect(Collectors.toSet()));
        response.setProxyConfigs(source.getProxyConfigs().stream()
                .map(proxy -> getConversionService().convert(proxy, ProxyConfigResponse.class))
                .collect(Collectors.toSet()));
        response.setRdsConfigs(source.getRdsConfigs().stream()
                .map(rds -> getConversionService().convert(rds, RDSConfigResponse.class))
                .collect(Collectors.toSet()));
        return response;
    }
}
