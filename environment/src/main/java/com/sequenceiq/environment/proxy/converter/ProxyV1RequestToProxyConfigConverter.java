package com.sequenceiq.environment.proxy.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.proxy.model.request.ProxyV1Request;
import com.sequenceiq.environment.proxy.ProxyConfig;

@Component
public class ProxyV1RequestToProxyConfigConverter extends AbstractConversionServiceAwareConverter<ProxyV1Request, ProxyConfig> {

    @Override
    public ProxyConfig convert(ProxyV1Request source) {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName(source.getName());
        proxyConfig.setPassword(source.getPassword());
        proxyConfig.setDescription(source.getDescription());
        proxyConfig.setProtocol(source.getProtocol());
        proxyConfig.setServerHost(source.getHost());
        proxyConfig.setServerPort(source.getPort());
        proxyConfig.setUserName(source.getUserName());
        return proxyConfig;
    }
}
