package com.sequenceiq.freeipa.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

import io.opentracing.Tracer;

class FreeIpaClientTest {

    private FreeIpaClient underTest;

    @BeforeEach
    void setUp() {
        underTest = new FreeIpaClient(
                mock(JsonRpcHttpClient.class),
                "apiVersion",
                "apiAddress",
                "hostname",
                mock(Tracer.class));
    }

    @Test
    void deleteUserThrowsOnProtectedUser() {
        assertThrows(FreeIpaClientException.class, () ->
                underTest.deleteUser(FreeIpaChecks.IPA_PROTECTED_USERS.get(0))
        );
    }

    @Test
    void addUserThrowsOnProtectedUser() {
        assertThrows(FreeIpaClientException.class, () ->
                underTest.userAdd(FreeIpaChecks.IPA_PROTECTED_USERS.get(0), "first", "last")
        );
    }
}