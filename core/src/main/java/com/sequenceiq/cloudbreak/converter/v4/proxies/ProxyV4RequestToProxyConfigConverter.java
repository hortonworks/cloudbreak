package com.sequenceiq.cloudbreak.converter.v4.proxies;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.requests.ProxyV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;

@Component
public class ProxyV4RequestToProxyConfigConverter extends AbstractConversionServiceAwareConverter<ProxyV4Request, ProxyConfig> {

    @Override
    public ProxyConfig convert(ProxyV4Request source) {
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
