package com.sequenceiq.environment.proxy.v1.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyViewResponse;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.domain.ProxyConfigBase;

@Component
public class ProxyConfigToProxyResponseConverter {

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    public ProxyResponse convert(ProxyConfig source) {
        ProxyResponse response = new ProxyResponse();
        response.setCrn(source.getResourceCrn());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setProtocol(source.getProtocol());
        response.setUserName(stringToSecretResponseConverter.convert(source.getUserNameSecret()));
        response.setPassword(stringToSecretResponseConverter.convert(source.getPasswordSecret()));
        response.setCreator(source.getCreator());
        response.setHost(source.getServerHost());
        response.setPort(source.getServerPort());
        response.setNoProxyHosts(source.getNoProxyHosts());
        return response;
    }

    public ProxyViewResponse convertToView(ProxyConfigBase source) {
        ProxyViewResponse response = new ProxyViewResponse();
        response.setCrn(source.getResourceCrn());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setProtocol(source.getProtocol());
        response.setCreator(source.getCreator());
        response.setHost(source.getServerHost());
        response.setPort(source.getServerPort());
        response.setNoProxyHosts(source.getNoProxyHosts());
        return response;
    }
}
