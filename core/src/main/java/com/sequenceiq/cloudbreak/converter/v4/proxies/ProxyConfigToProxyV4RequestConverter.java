package com.sequenceiq.cloudbreak.converter.v4.proxies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.requests.ProxyV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;

@Component
public class ProxyConfigToProxyV4RequestConverter
        extends AbstractConversionServiceAwareConverter<ProxyConfig, ProxyV4Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigToProxyV4RequestConverter.class);

    @Override
    public ProxyV4Request convert(ProxyConfig source) {
        ProxyV4Request request = new ProxyV4Request();
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
