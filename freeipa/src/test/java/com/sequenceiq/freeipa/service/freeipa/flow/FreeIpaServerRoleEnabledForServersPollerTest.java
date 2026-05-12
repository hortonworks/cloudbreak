package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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

    private static final String DNS_ROLE = "DNS server";

    private static final String SERVER = "ipa.server.com";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    void testConstructorShouldThrowNPEWhenServersIsNull() {
        assertThrows(NullPointerException.class, () -> new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, Set.of(ROLE), null));
    }

    @Test
    void testConstructorShouldThrowNPEWhenRolesIsNull() {
        assertThrows(NullPointerException.class, () -> new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, null, Set.of(SERVER)));
    }

    @Test
    void testProcessWhenRoleEnabledForAllServersShouldFinish() throws Exception {
        // GIVEN
        ServerRole caRole = serverRole(SERVER, ROLE, "enabled");
        ServerRole dnsRole = serverRole(SERVER, DNS_ROLE, "enabled");
        when(freeIpaClient.findServerRoles(isNull(), any(), any())).thenReturn(List.of(caRole, dnsRole));
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, Set.of(ROLE, DNS_ROLE), Set.of(SERVER));
        // WHEN
        AttemptResult<Void> result = underTest.process();
        // THEN
        assertEquals(AttemptState.FINISH, result.getState());
    }

    @Test
    void testProcessWhenOneRoleNotEnabledForAllServersShouldContinue() throws Exception {
        // GIVEN
        ServerRole caRole = serverRole(SERVER, ROLE, "disabled");
        ServerRole dnsRole = serverRole(SERVER, DNS_ROLE, "enabled");
        when(freeIpaClient.findServerRoles(isNull(), any(), any())).thenReturn(List.of(caRole, dnsRole));
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, Set.of(ROLE, DNS_ROLE), Set.of(SERVER));
        // WHEN
        AttemptResult<Void> result = underTest.process();
        // THEN
        assertEquals(AttemptState.CONTINUE, result.getState());
    }

    @Test
    void testProcessWhenBothRolesNotEnabledShouldContinueWithAggregatedError() throws Exception {
        // GIVEN
        when(freeIpaClient.findServerRoles(isNull(), any(), any())).thenReturn(List.of());
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, Set.of(ROLE, DNS_ROLE), Set.of(SERVER));
        // WHEN
        AttemptResult<Void> result = underTest.process();
        // THEN
        assertEquals(AttemptState.CONTINUE, result.getState());
        assertNotNull(result.getCause());
        String message = result.getCause().getMessage();
        assertTrue(message.contains(ROLE), "Error should mention CA server role");
        assertTrue(message.contains(DNS_ROLE), "Error should mention DNS server role");
        assertTrue(message.contains(SERVER), "Error should mention missing server");
    }

    @Test
    void testProcessWhenRoleNotEnabledForDifferentServerShouldContinue() throws Exception {
        // GIVEN
        ServerRole caRole = serverRole("another.server.com", ROLE, "enabled");
        ServerRole dnsRole = serverRole("another.server.com", DNS_ROLE, "enabled");
        when(freeIpaClient.findServerRoles(isNull(), any(), any())).thenReturn(List.of(caRole, dnsRole));
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, Set.of(ROLE, DNS_ROLE), Set.of(SERVER));
        // WHEN
        AttemptResult<Void> result = underTest.process();
        // THEN
        assertEquals(AttemptState.CONTINUE, result.getState());
    }

    @Test
    void testProcessWhenClientThrowsExceptionShouldBreak() throws Exception {
        // GIVEN
        FreeIpaClientException exception = new FreeIpaClientException("error");
        when(freeIpaClient.findServerRoles(isNull(), any(), any())).thenThrow(exception);
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, Set.of(ROLE, DNS_ROLE), Set.of(SERVER));
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
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, Set.of(ROLE, DNS_ROLE), Set.of());
        // WHEN
        AttemptResult<Void> result = underTest.process();
        // THEN
        assertEquals(AttemptState.FINISH, result.getState());
    }

    @Test
    void testProcessWhenClientReturnsEmptyListShouldContinue() throws Exception {
        // GIVEN
        when(freeIpaClient.findServerRoles(isNull(), any(), any())).thenReturn(List.of());
        FreeIpaServerRoleEnabledForServersPoller underTest = new FreeIpaServerRoleEnabledForServersPoller(freeIpaClient, Set.of(ROLE, DNS_ROLE), Set.of(SERVER));
        // WHEN
        AttemptResult<Void> result = underTest.process();
        // THEN
        assertEquals(AttemptState.CONTINUE, result.getState());
    }

    private ServerRole serverRole(String fqdn, String role, String status) {
        ServerRole sr = new ServerRole();
        sr.setServerFqdn(fqdn);
        sr.setRole(role);
        sr.setStatus(status);
        return sr;
    }
}
