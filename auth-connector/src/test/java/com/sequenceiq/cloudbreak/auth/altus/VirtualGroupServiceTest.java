package com.sequenceiq.cloudbreak.auth.altus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class VirtualGroupServiceTest {

    private static final String MOCK_VIRTUAL_GROUP = "mock_virtual_group";

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

    @InjectMocks
    private VirtualGroupService virtualGroupService;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Captor
    private final ArgumentCaptor<String> rightCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    public void testCreateVirtualGroupsWithNonExistingGroups() {
        doThrow(new StatusRuntimeException(Status.NOT_FOUND))
                .when(grpcUmsClient).getWorkloadAdministrationGroupName(eq(ACCOUNT_ID), any(), anyString(), eq(ENV_CRN), any());
        when(grpcUmsClient.setWorkloadAdministrationGroupName(eq(ACCOUNT_ID),
                any(), anyString(), eq(ENV_CRN), any())).thenReturn(MOCK_VIRTUAL_GROUP);

        Map<UmsVirtualGroupRight, String> result = virtualGroupService.createVirtualGroups(ACCOUNT_ID, ENV_CRN);

        assertEquals(UmsVirtualGroupRight.values().length, result.size());
        verify(grpcUmsClient, times(UmsVirtualGroupRight.values().length)).setWorkloadAdministrationGroupName(eq(ACCOUNT_ID),
                any(), rightCaptor.capture(), eq(ENV_CRN), any());
        for (UmsVirtualGroupRight umsRight : UmsVirtualGroupRight.values()) {
            assertEquals(MOCK_VIRTUAL_GROUP, result.get(umsRight));
            assertTrue(rightCaptor.getAllValues().contains(umsRight.getRight()));
        }
    }

    @Test
    public void testCreateVirtualGroupsWithExistingGroups() {
        when(grpcUmsClient.getWorkloadAdministrationGroupName(eq(ACCOUNT_ID),
                any(), anyString(), eq(ENV_CRN), any())).thenReturn(MOCK_VIRTUAL_GROUP);

        Map<UmsVirtualGroupRight, String> result = virtualGroupService.createVirtualGroups(ACCOUNT_ID, ENV_CRN);

        assertEquals(UmsVirtualGroupRight.values().length, result.size());
        verify(grpcUmsClient, times(0)).setWorkloadAdministrationGroupName(eq(ACCOUNT_ID),
                any(), anyString(), eq(ENV_CRN), any());
        for (UmsVirtualGroupRight umsRight : UmsVirtualGroupRight.values()) {
            assertEquals(MOCK_VIRTUAL_GROUP, result.get(umsRight));
        }
    }

    @Test
    public void testGetVirtualGroupWithNoAdminGroupProvidedAndNewGroupNeedsToBeCreated() {
        doThrow(new StatusRuntimeException(Status.NOT_FOUND))
                .when(grpcUmsClient).getWorkloadAdministrationGroupName(eq(ACCOUNT_ID), any(), anyString(), eq(ENV_CRN), any());
        when(grpcUmsClient.setWorkloadAdministrationGroupName(eq(ACCOUNT_ID),
                any(), anyString(), eq(ENV_CRN), any())).thenReturn(MOCK_VIRTUAL_GROUP);

        VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(ENV_CRN, null);
        String result = virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.ENVIRONMENT_ACCESS.getRight());

        verify(grpcUmsClient, times(1)).setWorkloadAdministrationGroupName(eq(ACCOUNT_ID),
                any(), rightCaptor.capture(), eq(ENV_CRN), any());
        assertEquals(MOCK_VIRTUAL_GROUP, result);
        assertTrue(rightCaptor.getAllValues().contains(UmsVirtualGroupRight.ENVIRONMENT_ACCESS.getRight()));
    }

    @Test
    public void testGetVirtualGroupWithEmptyGroupProvidedAndNewGroupNeedsToBeCreated() {
        doThrow(new StatusRuntimeException(Status.NOT_FOUND))
                .when(grpcUmsClient).getWorkloadAdministrationGroupName(eq(ACCOUNT_ID), any(), anyString(), eq(ENV_CRN), any());
        when(grpcUmsClient.setWorkloadAdministrationGroupName(eq(ACCOUNT_ID),
                any(), anyString(), eq(ENV_CRN), any())).thenReturn(MOCK_VIRTUAL_GROUP);

        VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(ENV_CRN, "");
        String result = virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.ENVIRONMENT_ACCESS.getRight());

        verify(grpcUmsClient, times(1)).setWorkloadAdministrationGroupName(eq(ACCOUNT_ID),
                any(), rightCaptor.capture(), eq(ENV_CRN), any());
        assertEquals(MOCK_VIRTUAL_GROUP, result);
        assertTrue(rightCaptor.getAllValues().contains(UmsVirtualGroupRight.ENVIRONMENT_ACCESS.getRight()));
    }

    @Test
    public void testGetVirtualGroupWenAdminGroupIsProvided() {
        VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(ENV_CRN, "mockgroup");
        String result = virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.ENVIRONMENT_ACCESS.getRight());

        verify(grpcUmsClient, times(0)).getWorkloadAdministrationGroupName(eq(ACCOUNT_ID),
                eq(Optional.empty()), anyString(), eq(ENV_CRN), any());
        verify(grpcUmsClient, times(0)).setWorkloadAdministrationGroupName(eq(ACCOUNT_ID),
                any(), anyString(), eq(ENV_CRN), any());
        assertEquals("mockgroup", result);
    }

    @Test
    public void testCleanupVirtualGroups() {
        virtualGroupService.cleanupVirtualGroups(ACCOUNT_ID, ENV_CRN);

        verify(grpcUmsClient, times(UmsVirtualGroupRight.values().length)).deleteWorkloadAdministrationGroupName(eq(ACCOUNT_ID),
                any(), rightCaptor.capture(), eq(ENV_CRN), any());
        for (UmsVirtualGroupRight right : UmsVirtualGroupRight.values()) {
            assertTrue(rightCaptor.getAllValues().contains(right.getRight()));
        }
    }
}
