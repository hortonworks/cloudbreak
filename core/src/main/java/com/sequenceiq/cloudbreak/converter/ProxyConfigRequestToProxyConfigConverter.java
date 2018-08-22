package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;

@Component
public class ProxyConfigRequestToProxyConfigConverter extends AbstractConversionServiceAwareConverter<ProxyConfigRequest, ProxyConfig> {

    @Override
    public ProxyConfig convert(ProxyConfigRequest source) {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName(source.getName());
        proxyConfig.setPassword(source.getPassword());
        proxyConfig.setDescription(source.getDescription());
        proxyConfig.setProtocol(source.getProtocol());
        proxyConfig.setServerHost(source.getServerHost());
        proxyConfig.setServerPort(source.getServerPort());
        proxyConfig.setUserName(source.getUserName());
        return proxyConfig;
    }
}
