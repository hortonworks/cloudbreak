package com.sequenceiq.environment.proxy.converter;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.workspace.model.CompactView;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Response;
import com.sequenceiq.environment.proxy.ProxyConfig;
import com.sequenceiq.secret.model.SecretResponse;

@Component
public class ProxyConfigToProxyV1ResponseConverter extends AbstractConversionServiceAwareConverter<ProxyConfig, ProxyV1Response> {

    @Override
    public ProxyV1Response convert(ProxyConfig source) {
        ProxyV1Response response = new ProxyV1Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setProtocol(source.getProtocol());
        response.setUserName(getConversionService().convert(source.getUserNameSecret(), SecretResponse.class));
        response.setPassword(getConversionService().convert(source.getPasswordSecret(), SecretResponse.class));
        response.setHost(source.getServerHost());
        response.setPort(source.getServerPort());
        response.setEnvironments(source.getEnvironments().stream()
                .map(CompactView::getName).collect(Collectors.toSet()));
        return response;
    }
}
