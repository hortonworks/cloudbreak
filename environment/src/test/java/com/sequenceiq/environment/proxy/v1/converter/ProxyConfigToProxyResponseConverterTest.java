package com.sequenceiq.environment.proxy.v1.converter;

import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.DESCRIPTION;
import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.NAME;
import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.NO_PROXY_HOSTS;
import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.PROTOCOL;
import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.SERVER_HOST;
import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.SERVER_PORT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.v1.ProxyTestSource;

@ExtendWith(MockitoExtension.class)
class ProxyConfigToProxyResponseConverterTest {

    @InjectMocks
    private ProxyConfigToProxyResponseConverter underTest;

    @Mock
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @Test
    void testConvertCrn() {
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.USERNAME_SECRET);
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.PASSWORD_SECRET);

        Assertions.assertEquals(ProxyTestSource.RESCRN, convertSourceProxyConfig().getCrn());
    }

    @Test
    void testConvertCreator() {
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.USERNAME_SECRET);
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.PASSWORD_SECRET);

        Assertions.assertEquals(ProxyTestSource.CREATOR, convertSourceProxyConfig().getCreator());
    }

    @Test
    void testConvertDescription() {
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.USERNAME_SECRET);
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.PASSWORD_SECRET);

        Assertions.assertEquals(DESCRIPTION, convertSourceProxyConfig().getDescription());
    }

    @Test
    void testConvertHost() {
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.USERNAME_SECRET);
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.PASSWORD_SECRET);

        Assertions.assertEquals(SERVER_HOST, convertSourceProxyConfig().getHost());
    }

    @Test
    void testConvertProtocol() {
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.USERNAME_SECRET);
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.PASSWORD_SECRET);

        Assertions.assertEquals(PROTOCOL, convertSourceProxyConfig().getProtocol());
    }

    @Test
    void testConvertName() {
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.USERNAME_SECRET);
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.PASSWORD_SECRET);

        Assertions.assertEquals(NAME, convertSourceProxyConfig().getName());
    }

    @Test
    void testConvertPort() {
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.USERNAME_SECRET);
        when(stringToSecretResponseConverter.convert(any())).thenReturn(ProxyTestSource.PASSWORD_SECRET);

        Assertions.assertEquals(SERVER_PORT, convertSourceProxyConfig().getPort());
    }

    @Test
    void testConvertNoProxyHosts() {
        Assertions.assertEquals(NO_PROXY_HOSTS, convertSourceProxyConfig().getNoProxyHosts());
    }

    @Test
    void testConvertCredentials() {
        when(stringToSecretResponseConverter.convert(ProxyTestSource.USERNAME)).thenReturn(ProxyTestSource.USERNAME_SECRET);
        when(stringToSecretResponseConverter.convert(ProxyTestSource.PASSWORD)).thenReturn(ProxyTestSource.PASSWORD_SECRET);
        ProxyResponse result = convertEncodedProxyConfig();
        Assertions.assertEquals(ProxyTestSource.USERNAME, result.getUserName().getSecretPath());
        Assertions.assertEquals(ProxyTestSource.PASSWORD, result.getPassword().getSecretPath());
    }

    public ProxyResponse convertSourceProxyConfig() {
        ProxyConfig testSource = ProxyTestSource.getProxyConfig();

        return underTest.convert(testSource);
    }

    private ProxyResponse convertEncodedProxyConfig() {
        ProxyConfig source = mock(ProxyConfig.class);
        when(source.getPasswordSecret()).thenReturn(ProxyTestSource.PASSWORD);
        when(source.getUserNameSecret()).thenReturn(ProxyTestSource.USERNAME);

        return underTest.convert(source);
    }
}
