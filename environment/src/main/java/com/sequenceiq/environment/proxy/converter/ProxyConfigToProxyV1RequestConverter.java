package com.sequenceiq.environment.proxy.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.proxy.model.request.ProxyV1Request;
import com.sequenceiq.environment.proxy.ProxyConfig;

@Component
public class ProxyConfigToProxyV1RequestConverter
        extends AbstractConversionServiceAwareConverter<ProxyConfig, ProxyV1Request> {

    @Override
    public ProxyV1Request convert(ProxyConfig source) {
        ProxyV1Request request = new ProxyV1Request();
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
