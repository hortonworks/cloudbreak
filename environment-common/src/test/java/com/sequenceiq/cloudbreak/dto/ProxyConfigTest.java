package com.sequenceiq.cloudbreak.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProxyConfigTest {

    private static final String PROXY_HOST = "10.0.0.5";

    private static final int PROXY_PORT = 3128;

    private static final String PROXY_AUTH = "squid";

    private static final String NO_PROXY_HOSTS = "noProxy.com";

    @Test
    void testGetFullProxyUrl() {
        ProxyConfig proxy = ProxyConfig.builder()
                .withName("name")
                .withProtocol("http")
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withNoProxyHosts(NO_PROXY_HOSTS)
                .build();

        assertEquals("http://10.0.0.5:3128", proxy.getFullProxyUrl());
        assertEquals(NO_PROXY_HOSTS, proxy.getNoProxyHosts());
    }

    @Test
    void testGetFullProxyUrlWithAuthentication() {
        ProxyConfig proxy = ProxyConfig.builder()
                .withName("name")
                .withProtocol("http")
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withProxyAuthentication(ProxyAuthentication.builder()
                        .withUserName(PROXY_AUTH)
                        .withPassword(PROXY_AUTH)
                        .build())
                .build();

        assertEquals("http://squid:squid@10.0.0.5:3128", proxy.getFullProxyUrl());
    }

    @Test
    void testGetFullProxyUrlWithAuthenticationAndHttps() {
        ProxyConfig proxy = ProxyConfig.builder()
                .withName("name")
                .withProtocol("https")
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withProxyAuthentication(ProxyAuthentication.builder()
                        .withUserName(PROXY_AUTH)
                        .withPassword(PROXY_AUTH)
                        .build())
                .build();

        assertEquals("https://squid:squid@10.0.0.5:3128", proxy.getFullProxyUrl());
    }
}
