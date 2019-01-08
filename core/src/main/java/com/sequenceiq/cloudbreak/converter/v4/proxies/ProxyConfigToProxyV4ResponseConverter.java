package com.sequenceiq.cloudbreak.converter.v4.proxies;

import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class ProxyConfigToProxyV4ResponseConverter extends AbstractConversionServiceAwareConverter<ProxyConfig, ProxyV4Response> {

    @Inject
    private ConversionService conversionService;

    @Override
    public ProxyV4Response convert(ProxyConfig source) {
        ProxyV4Response response = new ProxyV4Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        response.setDescription(source.getDescription());
        response.setProtocol(source.getProtocol());
        response.setUserName(conversionService.convert(source.getUserNameSecret(), SecretResponse.class));
        response.setPassword(conversionService.convert(source.getPasswordSecret(), SecretResponse.class));
        response.setHost(source.getServerHost());
        response.setPort(source.getServerPort());
        response.setEnvironments(source.getEnvironments().stream()
                .map(CompactView::getName).collect(Collectors.toSet()));
        return response;
    }
}
