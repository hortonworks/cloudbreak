package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.authdistributor.GrpcAuthDistributorClient;
import com.sequenceiq.freeipa.converter.freeipa.user.UmsUsersStateToAuthDistributorUserStateConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

@ExtendWith(MockitoExtension.class)
class AuthDistributorServiceTest {

    @Spy
    private UmsUsersStateToAuthDistributorUserStateConverter umsUsersStateToAuthDistributorUserStateConverter;

    @Mock
    private GrpcAuthDistributorClient grpcAuthDistributorClient;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private AuthDistributorService underTest;

    @Test
    public void testUpdateAuthViewForEnvironment() {
        when(entitlementService.isSdxSaasIntegrationEnabled("accountId")).thenReturn(Boolean.TRUE);
        UmsUsersState umsUsersState = UmsUsersState.newBuilder().setUsersState(UsersState.newBuilder().build()).build();
        underTest.updateAuthViewForEnvironment("envCrn", umsUsersState, "accountId", "operationId");

        verify(umsUsersStateToAuthDistributorUserStateConverter).convert(eq(umsUsersState));
        verify(grpcAuthDistributorClient).updateAuthViewForEnvironment(eq("envCrn"), any());
    }

    @Test
    public void testUpdateAuthViewForEnvironmentWhenConverterFails() {
        when(entitlementService.isSdxSaasIntegrationEnabled("accountId")).thenReturn(Boolean.TRUE);
        doThrow(new RuntimeException("error")).when(umsUsersStateToAuthDistributorUserStateConverter).convert(any());
        UmsUsersState umsUsersState = UmsUsersState.newBuilder().setUsersState(UsersState.newBuilder().build()).build();
        assertDoesNotThrow(() -> underTest.updateAuthViewForEnvironment("envCrn", umsUsersState, "accountId", "operationId"));

        verify(grpcAuthDistributorClient, never()).updateAuthViewForEnvironment(eq("envCrn"), any());
    }

    @Test
    public void testUpdateAuthViewForEnvironmentWhenGrpcFails() {
        when(entitlementService.isSdxSaasIntegrationEnabled("accountId")).thenReturn(Boolean.TRUE);
        doThrow(new RuntimeException("error")).when(grpcAuthDistributorClient).updateAuthViewForEnvironment(eq("envCrn"), any());
        UmsUsersState umsUsersState = UmsUsersState.newBuilder().setUsersState(UsersState.newBuilder().build()).build();
        assertDoesNotThrow(() -> underTest.updateAuthViewForEnvironment("envCrn", umsUsersState, "accountId", "operationId"));

        verify(umsUsersStateToAuthDistributorUserStateConverter).convert(eq(umsUsersState));
    }

    @Test
    public void testUpdateAuthViewForEnvironmentWhenCdpSaasEntitlementDisabled() {
        when(entitlementService.isSdxSaasIntegrationEnabled("accountId")).thenReturn(Boolean.FALSE);
        UmsUsersState umsUsersState = UmsUsersState.newBuilder().setUsersState(UsersState.newBuilder().build()).build();
        underTest.updateAuthViewForEnvironment("envCrn", umsUsersState, "accountId", "operationId");

        verify(umsUsersStateToAuthDistributorUserStateConverter, never()).convert(eq(umsUsersState));
        verify(grpcAuthDistributorClient, never()).updateAuthViewForEnvironment(eq("envCrn"), any());
    }

    @Test
    public void testRemoveAuthViewForEnvironment() {
        when(entitlementService.isSdxSaasIntegrationEnabled("accountId")).thenReturn(Boolean.TRUE);
        underTest.removeAuthViewForEnvironment("envCrn", "accountId");
        verify(grpcAuthDistributorClient).removeAuthViewForEnvironment(eq("envCrn"));
    }

    @Test
    public void testRemoveAuthViewForEnvironmentWhenGrpcFails() {
        when(entitlementService.isSdxSaasIntegrationEnabled("accountId")).thenReturn(Boolean.TRUE);
        doThrow(new RuntimeException("error")).when(grpcAuthDistributorClient).removeAuthViewForEnvironment(eq("envCrn"));
        assertDoesNotThrow(() -> underTest.removeAuthViewForEnvironment("envCrn", "accountId"));
    }

    @Test
    public void testRemoveAuthViewForEnvironmentWhenCdpSaasEntitlementDisabled() {
        when(entitlementService.isSdxSaasIntegrationEnabled("accountId")).thenReturn(Boolean.FALSE);
        underTest.removeAuthViewForEnvironment("envCrn", "accountId");
        verify(grpcAuthDistributorClient, never()).removeAuthViewForEnvironment(eq("envCrn"));
    }

}