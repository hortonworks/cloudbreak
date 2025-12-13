package com.sequenceiq.cloudbreak.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProxyAuthenticationTest {
    @Test
    public void testBuilderBuildProperProxyAuthentication() {
        ProxyAuthentication proxyAuthentication = ProxyAuthentication.builder()
                .withUserName("user")
                .withPassword("pwd")
                .build();
        assertEquals("user", proxyAuthentication.getUserName());
        assertEquals("pwd", proxyAuthentication.getPassword());
    }

    @Test
    public void testBuilderShouldThrowIllegalArgumentExcepitonWhenPasswordNotProvided() {
        assertThrows(IllegalArgumentException.class, () -> ProxyAuthentication.builder()
                .withUserName("user")
                .withPassword("")
                .build());
    }

    @Test
    public void testBuilderShouldThrowIllegalArgumentExcepitonWhenUserNameNotProvided() {
        assertThrows(IllegalArgumentException.class, () -> ProxyAuthentication.builder()
                .withUserName(null)
                .withPassword("pwd")
                .build());
    }
}
