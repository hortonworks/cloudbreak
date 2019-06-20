package com.sequenceiq.environment.proxy.v1.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.proxy.v1.ProxyTestSource;

public class ProxyConfigToProxyRequestConverterTest extends ProxyConfigToProxyRequestConverter {
    @Test
    public void testConvertCredentialAreMasked() {
        ProxyRequest result = converProxyConfig();
        Assertions.assertEquals(FAKE_USERNAME, result.getUserName());
        Assertions.assertEquals(FAKE_PASSWORD, result.getPassword());
    }

    @Test
    public void testConvertName() {
        Assertions.assertEquals(ProxyTestSource.NAME, converProxyConfig().getName());
    }

    @Test
    public void testConvertDescription() {
        Assertions.assertEquals(ProxyTestSource.DESCRIPTION, converProxyConfig().getDescription());
    }

    @Test
    public void testConvertHost() {
        Assertions.assertEquals(ProxyTestSource.SERVER_HOST, converProxyConfig().getHost());
    }

    @Test
    public void testConvertProtocol() {
        Assertions.assertEquals(ProxyTestSource.PROTOCOL, converProxyConfig().getProtocol());
    }

    @Test
    public void testConvertPort() {
        Assertions.assertEquals(ProxyTestSource.SERVER_PORT, converProxyConfig().getPort());
    }

    private ProxyRequest converProxyConfig() {
        return convert(ProxyTestSource.getProxyConfig());
    }
}
