package com.sequenceiq.cloudbreak.converter.v4.proxies;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class ProxyConfigToProxyV4ResponseConverter extends AbstractConversionServiceAwareConverter<ProxyConfig, ProxyV4Response> {

    @Override
    public ProxyV4Response convert(ProxyConfig source) {
        ProxyV4Response response = new ProxyV4Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        response.setDescription(source.getDescription());
        response.setProtocol(source.getProtocol());
        response.setUserName(getConversionService().convert(source.getUserNameSecret(), SecretV4Response.class));
        response.setPassword(getConversionService().convert(source.getPasswordSecret(), SecretV4Response.class));
        response.setHost(source.getServerHost());
        response.setPort(source.getServerPort());
        response.setEnvironments(source.getEnvironments().stream()
                .map(CompactView::getName).collect(Collectors.toSet()));
        return response;
    }
}
