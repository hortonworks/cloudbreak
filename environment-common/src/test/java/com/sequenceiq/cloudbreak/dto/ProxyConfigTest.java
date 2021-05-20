package com.sequenceiq.cloudbreak.dto;

import org.junit.Assert;
import org.junit.Test;

public class ProxyConfigTest {

    private static final String PROXY_HOST = "10.0.0.5";

    private static final int PROXY_PORT = 3128;

    private static final String PROXY_AUTH = "squid";

    private static final String NO_PROXY_HOSTS = "noProxy.com";

    @Test
    public void testGetFullProxyUrl() {
        ProxyConfig proxy = ProxyConfig.builder()
                .withName("name")
                .withProtocol("http")
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withNoProxyHosts(NO_PROXY_HOSTS)
                .build();

        Assert.assertEquals("http://10.0.0.5:3128", proxy.getFullProxyUrl());
        Assert.assertEquals(NO_PROXY_HOSTS, proxy.getNoProxyHosts());
    }

    @Test
    public void testGetFullProxyUrlWithAuthentication() {
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

        Assert.assertEquals("http://squid:squid@10.0.0.5:3128", proxy.getFullProxyUrl());
    }

    @Test
    public void testGetFullProxyUrlWithAuthenticationAndHttps() {
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

        Assert.assertEquals("https://squid:squid@10.0.0.5:3128", proxy.getFullProxyUrl());
    }
}
