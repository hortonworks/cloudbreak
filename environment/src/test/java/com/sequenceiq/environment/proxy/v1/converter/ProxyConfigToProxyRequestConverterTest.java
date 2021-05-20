package com.sequenceiq.environment.proxy.v1.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.proxy.v1.ProxyTestSource;

class ProxyConfigToProxyRequestConverterTest extends ProxyConfigToProxyRequestConverter {

    @Test
    void testConvertCredentialAreMasked() {
        ProxyRequest result = convertProxyConfig();
        Assertions.assertEquals(FAKE_USERNAME, result.getUserName());
        Assertions.assertEquals(FAKE_PASSWORD, result.getPassword());
    }

    @Test
    void testConvertName() {
        Assertions.assertEquals(ProxyTestSource.NAME, convertProxyConfig().getName());
    }

    @Test
    void testConvertDescription() {
        Assertions.assertEquals(ProxyTestSource.DESCRIPTION, convertProxyConfig().getDescription());
    }

    @Test
    void testConvertHost() {
        Assertions.assertEquals(ProxyTestSource.SERVER_HOST, convertProxyConfig().getHost());
    }

    @Test
    void testConvertProtocol() {
        Assertions.assertEquals(ProxyTestSource.PROTOCOL, convertProxyConfig().getProtocol());
    }

    @Test
    void testConvertPort() {
        Assertions.assertEquals(ProxyTestSource.SERVER_PORT, convertProxyConfig().getPort());
    }

    @Test
    void testConvertNoProxyHosts() {
        Assertions.assertEquals(ProxyTestSource.NO_PROXY_HOSTS, convertProxyConfig().getNoProxyHosts());
    }

    private ProxyRequest convertProxyConfig() {
        return convert(ProxyTestSource.getProxyConfig());
    }
}
