package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class ProxyConfigToProxyConfigResponseConverter extends AbstractConversionServiceAwareConverter<ProxyConfig, ProxyConfigResponse> {

    @Inject
    private ConversionService conversionService;

    @Override
    public ProxyConfigResponse convert(ProxyConfig source) {
        ProxyConfigResponse response = new ProxyConfigResponse();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        response.setDescription(source.getDescription());
        response.setProtocol(source.getProtocol());
        response.setUserName(conversionService.convert(source.getUserNameSecret(), SecretResponse.class));
        response.setPassword(conversionService.convert(source.getPasswordSecret(), SecretResponse.class));
        response.setServerHost(source.getServerHost());
        response.setServerPort(source.getServerPort());
        response.setEnvironments(source.getEnvironments().stream()
                .map(CompactView::getName).collect(Collectors.toSet()));
        return response;
    }
}
