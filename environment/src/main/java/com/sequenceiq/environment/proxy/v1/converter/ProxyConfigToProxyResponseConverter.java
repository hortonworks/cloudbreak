package com.sequenceiq.environment.proxy.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyViewResponse;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

@Component
public class ProxyConfigToProxyResponseConverter extends AbstractConversionServiceAwareConverter<ProxyConfig, ProxyResponse> {

    @Override
    public ProxyResponse convert(ProxyConfig source) {
        ProxyResponse response = new ProxyResponse();
        response.setCrn(source.getResourceCrn());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setProtocol(source.getProtocol());
        response.setUserName(getConversionService().convert(source.getUserNameSecret(), SecretResponse.class));
        response.setPassword(getConversionService().convert(source.getPasswordSecret(), SecretResponse.class));
        response.setCreator(source.getCreator());
        response.setHost(source.getServerHost());
        response.setPort(source.getServerPort());

        return response;
    }

    public ProxyViewResponse convertToView(ProxyConfig source) {
        ProxyViewResponse response = new ProxyViewResponse();
        response.setCrn(source.getResourceCrn());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setProtocol(source.getProtocol());
        response.setCreator(source.getCreator());
        response.setHost(source.getServerHost());
        response.setPort(source.getServerPort());

        return response;
    }
}
