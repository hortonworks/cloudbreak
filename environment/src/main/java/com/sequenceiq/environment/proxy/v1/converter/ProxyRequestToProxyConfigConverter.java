package com.sequenceiq.environment.proxy.v1.converter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

@Component
public class ProxyRequestToProxyConfigConverter extends AbstractConversionServiceAwareConverter<ProxyRequest, ProxyConfig> {

    @Override
    public ProxyConfig convert(ProxyRequest source) {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName(source.getName());
        proxyConfig.setDescription(source.getDescription());
        proxyConfig.setProtocol(source.getProtocol());
        proxyConfig.setServerHost(source.getHost());
        proxyConfig.setServerPort(source.getPort());
        if (StringUtils.isNoneBlank(source.getUserName(), source.getPassword())) {
            proxyConfig.setUserName(source.getUserName());
            proxyConfig.setPassword(source.getPassword());
        }
        return proxyConfig;
    }
}
