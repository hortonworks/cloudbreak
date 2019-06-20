package com.sequenceiq.environment.proxy.v1.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.v1.ProxyTestSource;

@Component
public class ProxyRequestToProxyConfigConverterTest extends ProxyRequestToProxyConfigConverter {

    @Test
    void testConvertAccount() {
        Assertions.assertEquals(null, getConvertProxyConfig().getAccountId());
    }

    @Test
    void testConvertName() {
        Assertions.assertEquals(ProxyTestSource.NAME, getConvertProxyConfig().getName());
    }

    @Test
    void testConvertPassword() {
        Assertions.assertEquals(ProxyTestSource.PASSWORD, getConvertProxyConfig().getPassword());
    }

    @Test
    void testConvertUsername() {
        Assertions.assertEquals(ProxyTestSource.USERNAME, getConvertProxyConfig().getUserName());
    }

    @Test
    void testConvertDescription() {
        Assertions.assertEquals(ProxyTestSource.DESCRIPTION, getConvertProxyConfig().getDescription());
    }

    @Test
    void testConvertPort() {
        Assertions.assertEquals(ProxyTestSource.SERVER_PORT, getConvertProxyConfig().getServerPort());
    }

    @Test
    void testConvertHost() {
        Assertions.assertEquals(ProxyTestSource.SERVER_HOST, getConvertProxyConfig().getServerHost());
    }

    @Test
    void testConvertProtocoll() {
        Assertions.assertEquals(ProxyTestSource.PROTOCOL, getConvertProxyConfig().getProtocol());
    }

    private ProxyConfig getConvertProxyConfig() {
        return convert(ProxyTestSource.getProxyRequest());
    }
}
