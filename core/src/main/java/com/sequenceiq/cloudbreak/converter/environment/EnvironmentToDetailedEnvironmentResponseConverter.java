package com.sequenceiq.cloudbreak.converter.environment;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;

@Component
public class EnvironmentToDetailedEnvironmentResponseConverter extends AbstractConversionServiceAwareConverter<Environment, DetailedEnvironmentResponse> {

    @Inject
    private RegionConverter regionConverter;

    @Override
    public DetailedEnvironmentResponse convert(Environment source) {
        DetailedEnvironmentResponse response = new DetailedEnvironmentResponse();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setCredentialName(source.getCredential().getName());
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        response.setLdapConfigs(source.getLdapConfigs().stream().map(LdapConfig::getName).collect(Collectors.toSet()));
        response.setProxyConfigs(source.getProxyConfigs().stream().map(ProxyConfig::getName).collect(Collectors.toSet()));
        response.setRdsConfigs(source.getRdsConfigs().stream().map(RDSConfig::getName).collect(Collectors.toSet()));
        if (!CollectionUtils.isEmpty(source.getWorkloadClusters())) {
            response.setWorkloadClusters(source.getWorkloadClusters().stream().map(StackApiView::getName).collect(Collectors.toSet()));
        }
        return response;
    }
}
