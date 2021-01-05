package com.sequenceiq.freeipa.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.List;

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
                underTest.getDeleteUserFlagsAndParams(FreeIpaChecks.IPA_PROTECTED_USERS.get(0))
        );
    }

    @Test
    void addUserThrowsOnProtectedUser() {
        assertThrows(FreeIpaClientException.class, () ->
                underTest.getUserAddFlagsAndParams(FreeIpaChecks.IPA_PROTECTED_USERS.get(0), "first", "last")
        );
    }

    @Test
    void groupAddThrowsOnProtectedGroup() {
        assertThrows(FreeIpaClientException.class, () ->
                underTest.getGroupAddFlagsAndParams(FreeIpaChecks.IPA_PROTECTED_GROUPS.get(0))
        );
    }

    @Test
    void deleteGroupThrowsOnProtectedGroup() {
        assertThrows(FreeIpaClientException.class, () ->
                underTest.getDeleteGroupFlagsAndParams(FreeIpaChecks.IPA_PROTECTED_GROUPS.get(0))
        );
    }

    @Test
    void groupAddMembersThrowsOnUnmanagedGroup() {
        assertThrows(FreeIpaClientException.class, () ->
                underTest.getGroupAddMembersFlagsAndParams(FreeIpaChecks.IPA_UNMANAGED_GROUPS.get(0), List.of("harry", "sally"))
        );
    }

    @Test
    void groupRemoveMembersThrowsOnUnmanagedGroup() {
        assertThrows(FreeIpaClientException.class, () ->
                underTest.getGroupAddMembersFlagsAndParams(FreeIpaChecks.IPA_UNMANAGED_GROUPS.get(0), List.of("harry", "sally"))
        );
    }
}