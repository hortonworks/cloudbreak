package com.sequenceiq.cloudbreak.dto;

import org.junit.Assert;
import org.junit.Test;

public class ProxyAuthenticationTest {
    @Test
    public void testBuilderBuildProperProxyAuthentication() {
        ProxyAuthentication proxyAuthentication = ProxyAuthentication.builder()
                .withUserName("user")
                .withPassword("pwd")
                .build();
        Assert.assertEquals("user", proxyAuthentication.getUserName());
        Assert.assertEquals("pwd", proxyAuthentication.getPassword());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderShouldThrowIllegalArgumentExcepitonWhenPasswordNotProvided() {
        ProxyAuthentication.builder()
                .withUserName("user")
                .withPassword("")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderShouldThrowIllegalArgumentExcepitonWhenUserNameNotProvided() {
        ProxyAuthentication.builder()
                .withUserName(null)
                .withPassword("pwd")
                .build();
    }
}
