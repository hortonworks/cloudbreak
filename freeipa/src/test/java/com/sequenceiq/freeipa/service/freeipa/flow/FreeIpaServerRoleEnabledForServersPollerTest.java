package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.ServerRole;

@ExtendWith(MockitoExtension.class)
class FreeIpaServerRoleEnabledForServersPollerTest {

    private static final String ROLE = "CA server";

    private static final String SERVER = "ipa.server.com";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    void testConstructorShouldThrowNPEWhenServersIsNull() {
        assertThrows(NullPointerException.class, () -> new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, ROLE, null));
    }

    @Test
    void testProcessWhenRoleEnabledForAllServersShouldFinish() throws Exception {
        // GIVEN
        ServerRole serverRole = new ServerRole();
        serverRole.setServerFqdn(SERVER);
        serverRole.setStatus("enabled");
        when(freeIpaClient.findServerRoles(anyString(), any(), any())).thenReturn(List.of(serverRole));
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, ROLE, Set.of(SERVER));
        // WHEN
        AttemptResult<Void> result = underTest.process();
        // THEN
        assertEquals(AttemptState.FINISH, result.getState());
    }

    @Test
    void testProcessWhenRoleNotEnabledForAllServersShouldContinue() throws Exception {
        // GIVEN
        ServerRole serverRole = new ServerRole();
        serverRole.setServerFqdn(SERVER);
        serverRole.setStatus("disabled");
        when(freeIpaClient.findServerRoles(anyString(), any(), any())).thenReturn(List.of(serverRole));
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, ROLE, Set.of(SERVER));
        // WHEN
        AttemptResult<Void> result = underTest.process();
        // THEN
        assertEquals(AttemptState.CONTINUE, result.getState());
    }

    @Test
    void testProcessWhenRoleNotEnabledForDifferentServerShouldContinue() throws Exception {
        // GIVEN
        ServerRole serverRole = new ServerRole();
        serverRole.setServerFqdn("another.server.com");
        serverRole.setStatus("enabled");
        when(freeIpaClient.findServerRoles(anyString(), any(), any())).thenReturn(List.of(serverRole));
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, ROLE, Set.of(SERVER));
        // WHEN
        AttemptResult<Void> result = underTest.process();
        // THEN
        assertEquals(AttemptState.CONTINUE, result.getState());
    }

    @Test
    void testProcessWhenClientThrowsExceptionShouldBreak() throws Exception {
        // GIVEN
        FreeIpaClientException exception = new FreeIpaClientException("error");
        when(freeIpaClient.findServerRoles(anyString(), any(), any())).thenThrow(exception);
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, ROLE, Set.of(SERVER));
        // WHEN
        AttemptResult<Void> result = underTest.process();
        // THEN
        assertEquals(AttemptState.BREAK, result.getState());
        assertNotNull(result.getCause());
        assertEquals(exception, result.getCause());
    }

    @Test
    void testProcessWithEmptyServerListShouldFinish() throws Exception {
        // GIVEN
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, ROLE, Set.of());
        // WHEN
        AttemptResult<Void> result = underTest.process();
        // THEN
        assertEquals(AttemptState.FINISH, result.getState());
    }

    @Test
    void testProcessWhenClientReturnsEmptyListShouldContinue() throws Exception {
        // GIVEN
        when(freeIpaClient.findServerRoles(anyString(), any(), any())).thenReturn(List.of());
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, ROLE, Set.of(SERVER));
        // WHEN
        AttemptResult<Void> result = underTest.process();
        // THEN
        assertEquals(AttemptState.CONTINUE, result.getState());
    }
}
