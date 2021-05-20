package com.sequenceiq.environment.proxy.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

@Component
public class ProxyConfigToProxyRequestConverter
        extends AbstractConversionServiceAwareConverter<ProxyConfig, ProxyRequest> {

    public static final String FAKE_PASSWORD = "fake-password";

    public static final String FAKE_USERNAME = "fake-username";

    @Override
    public ProxyRequest convert(ProxyConfig source) {
        ProxyRequest request = new ProxyRequest();
        request.setName(source.getName());
        request.setDescription(source.getDescription());
        request.setPassword(FAKE_PASSWORD);
        request.setUserName(FAKE_USERNAME);
        request.setHost(source.getServerHost());
        request.setPort(source.getServerPort());
        request.setProtocol(source.getProtocol());
        request.setNoProxyHosts(source.getNoProxyHosts());
        return request;
    }

}
