package com.sequenceiq.freeipa.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

class FreeIpaClientTest {

    private FreeIpaClient underTest;

    @BeforeEach
    void setUp() {
        underTest = new FreeIpaClient(
                mock(JsonRpcHttpClient.class),
                "apiVersion",
                "apiAddress",
                "hostname"
        );
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

    @Test
    void groupAddThrowsOnProtectedGroup() {
        assertThrows(FreeIpaClientException.class, () ->
                underTest.groupAdd(FreeIpaChecks.IPA_PROTECTED_GROUPS.get(0))
        );
    }

    @Test
    void deleteGroupThrowsOnProtectedGroup() {
        assertThrows(FreeIpaClientException.class, () ->
                underTest.deleteGroup(FreeIpaChecks.IPA_PROTECTED_GROUPS.get(0))
        );
    }

    @Test
    void groupAddMembersThrowsOnUnmanagedGroup() {
        assertThrows(FreeIpaClientException.class, () ->
                underTest.groupAddMembers(FreeIpaChecks.IPA_UNMANAGED_GROUPS.get(0), List.of("harry", "sally"))
        );
    }

    @Test
    void groupRemoveMembersThrowsOnUnmanagedGroup() {
        assertThrows(FreeIpaClientException.class, () ->
                underTest.groupAddMembers(FreeIpaChecks.IPA_UNMANAGED_GROUPS.get(0), List.of("harry", "sally"))
        );
    }
}