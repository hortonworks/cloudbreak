package com.sequenceiq.environment.proxy.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;

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

    private static Stream<Arguments> invalidUserPasswords() {
        return Stream.of(
                Arguments.of("   "),
                Arguments.of("\t"),
                Arguments.of(""),
                Arguments.of(" \t "),
                Arguments.of(new Object[] { null }));
    }

    @Test
    void testConvertAccount() {
        assertEquals(null, getConvertProxyConfig().getAccountId());
    }

    @Test
    void testConvertName() {
        assertEquals(ProxyTestSource.NAME, getConvertProxyConfig().getName());
    }

    @Test
    void testConvertPassword() {
        assertEquals(ProxyTestSource.PASSWORD, getConvertProxyConfig().getPassword());
    }

    @Test
    void testConvertUsername() {
        assertEquals(ProxyTestSource.USERNAME, getConvertProxyConfig().getUserName());
    }

    @Test
    void testConvertDescription() {
        assertEquals(ProxyTestSource.DESCRIPTION, getConvertProxyConfig().getDescription());
    }

    @Test
    void testConvertPort() {
        assertEquals(ProxyTestSource.SERVER_PORT, getConvertProxyConfig().getServerPort());
    }

    @Test
    void testConvertHost() {
        assertEquals(ProxyTestSource.SERVER_HOST, getConvertProxyConfig().getServerHost());
    }

    @Test
    void testConvertProtocoll() {
        assertEquals(ProxyTestSource.PROTOCOL, getConvertProxyConfig().getProtocol());
    }

    @Test
    void testConvertNoProxyHosts() {
        assertEquals(ProxyTestSource.NO_PROXY_HOSTS, getConvertProxyConfig().getNoProxyHosts());
    }

    @Test
    void testConvertInboundProxyCidr() {
        assertEquals(ProxyTestSource.INBOUND_PROXY_CIDR, getConvertProxyConfig().getInboundProxyCidr());
    }

    @ParameterizedTest
    @MethodSource("invalidUserPasswords")
    void testConvertEmptyUser(String user) {
        ProxyRequest request = ProxyTestSource.getProxyRequest();
        request.setUserName(user);
        assertNull(convert(request).getUserName());
    }

    @ParameterizedTest
    @MethodSource("invalidUserPasswords")
    void testConvertEmptyPassword(String password) {
        ProxyRequest request = ProxyTestSource.getProxyRequest();
        request.setPassword(password);
        assertNull(convert(request).getPassword());
    }

    private ProxyConfig getConvertProxyConfig() {
        return convert(ProxyTestSource.getProxyRequest());
    }
}
