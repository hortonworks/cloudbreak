package com.sequenceiq.environment.proxy.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

@Component
public class ProxyConfigToProxyV1RequestConverter
        extends AbstractConversionServiceAwareConverter<ProxyConfig, ProxyRequest> {

    @Override
    public ProxyRequest convert(ProxyConfig source) {
        ProxyRequest request = new ProxyRequest();
        request.setName(source.getName());
        request.setDescription(source.getDescription());
        request.setPassword("fake-password");
        request.setUserName("fake-username");
        request.setHost(source.getServerHost());
        request.setPort(source.getServerPort());
        request.setProtocol(source.getProtocol());
        return request;
    }

}
