package com.sequenceiq.environment.proxy.v1.converter;

import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.DESCRIPTION;
import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.NAME;
import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.NO_PROXY_HOSTS;
import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.PROTOCOL;
import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.SERVER_HOST;
import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.SERVER_PORT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.v1.ProxyTestSource;

class ProxyConfigToProxyResponseConverterTest extends ProxyConfigToProxyResponseConverter {

    public ConversionService getConversionService() {
        ConversionService conversionService = mock(ConversionService.class);
        when(conversionService.convert(ProxyTestSource.USERNAME, SecretResponse.class)).thenReturn(ProxyTestSource.USERNAME_SECRET);
        when(conversionService.convert(ProxyTestSource.PASSWORD, SecretResponse.class)).thenReturn(ProxyTestSource.PASSWORD_SECRET);

        return conversionService;
    }

    @Test
    void testConvertCrn() {
        Assertions.assertEquals(ProxyTestSource.RESCRN, convertSourceProxyConfig().getCrn());
    }

    @Test
    void testConvertCreator() {
        Assertions.assertEquals(ProxyTestSource.CREATOR, convertSourceProxyConfig().getCreator());
    }

    @Test
    void testConvertDescription() {
        Assertions.assertEquals(DESCRIPTION, convertSourceProxyConfig().getDescription());
    }

    @Test
    void testConvertHost() {
        Assertions.assertEquals(SERVER_HOST, convertSourceProxyConfig().getHost());
    }

    @Test
    void testConvertProtocol() {
        Assertions.assertEquals(PROTOCOL, convertSourceProxyConfig().getProtocol());
    }

    @Test
    void testConvertName() {
        Assertions.assertEquals(NAME, convertSourceProxyConfig().getName());
    }

    @Test
    void testConvertPort() {
        Assertions.assertEquals(SERVER_PORT, convertSourceProxyConfig().getPort());
    }

    @Test
    void testConvertNoProxyHosts() {
        Assertions.assertEquals(NO_PROXY_HOSTS, convertSourceProxyConfig().getNoProxyHosts());
    }

    @Test
    void testConvertCredentials() {
        ProxyResponse result = convertEncodedProxyConfig();
        Assertions.assertEquals(ProxyTestSource.USERNAME, result.getUserName().getSecretPath());
        Assertions.assertEquals(ProxyTestSource.PASSWORD, result.getPassword().getSecretPath());
    }

    public ProxyResponse convertSourceProxyConfig() {
        ProxyConfig testSource = ProxyTestSource.getProxyConfig();

        return convert(testSource);
    }

    private ProxyResponse convertEncodedProxyConfig() {
        ProxyConfig source = mock(ProxyConfig.class);
        when(source.getPasswordSecret()).thenReturn(ProxyTestSource.PASSWORD);
        when(source.getUserNameSecret()).thenReturn(ProxyTestSource.USERNAME);

        return convert(source);
    }
}
