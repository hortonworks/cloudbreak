package com.sequenceiq.environment.proxy.v1.converter;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
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

    @Test
    void testConvertNoProxyHosts() {
        Assertions.assertEquals(ProxyTestSource.NO_PROXY_HOSTS, getConvertProxyConfig().getNoProxyHosts());
    }

    @ParameterizedTest
    @MethodSource("invalidUserPasswords")
    void testConvertEmptyUser(String user) {
        ProxyRequest request = ProxyTestSource.getProxyRequest();
        request.setUserName(user);
        Assertions.assertNull(convert(request).getUserName());
    }

    @ParameterizedTest
    @MethodSource("invalidUserPasswords")
    void testConvertEmptyPassword(String password) {
        ProxyRequest request = ProxyTestSource.getProxyRequest();
        request.setPassword(password);
        Assertions.assertNull(convert(request).getPassword());
    }

    private ProxyConfig getConvertProxyConfig() {
        return convert(ProxyTestSource.getProxyRequest());
    }

    private static Stream<Arguments> invalidUserPasswords() {
        return Stream.of(
                Arguments.of("   "),
                Arguments.of("\t"),
                Arguments.of(""),
                Arguments.of(" \t "),
                Arguments.of(new Object[] { null }));
    }
}
