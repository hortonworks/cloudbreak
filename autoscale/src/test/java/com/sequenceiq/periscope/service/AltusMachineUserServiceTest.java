package com.sequenceiq.periscope.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.google.common.collect.LinkedHashMultimap;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.monitor.handler.FreeIpaCommunicator;

class AltusMachineUserServiceTest {

    @InjectMocks
    private AltusMachineUserService underTest;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private ClusterService clusterService;

    @Mock
    private RoleCrnGenerator roleCrnGenerator;

    @Mock
    private FreeIpaCommunicator freeIpaCommunicator;

    @Mock
    private CloudbreakVersionService cloudbreakVersionService;

    private String testEnvironmentCrn;

    private String testClusterCrn;

    private String testAccountId;

    private String autoscaleMachineUserName;

    private String autoscaleMachineUserCrn;

    private String environmentRoleCrn;

    private String testOperationId;

    private String testCbSaltVersion;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(underTest, "cloudbreakVersionThreshold", "2.70.0");
        testEnvironmentCrn = "crn:cdp:environments:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:environment:1584cdfa-ad2f-45ff-b3d9-414b5b013001";
        testClusterCrn = "crn:cdp:datahub:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:cluster:77a21ecf-ee77-4098-aa41-503ea75a7b8e";
        testAccountId = "testTenant";
        autoscaleMachineUserName = "as5fca7da5aef81a50a8d49732";
        autoscaleMachineUserCrn = "testMachineUserCrn";
        environmentRoleCrn = "environmentuserRoleCrn";
        testOperationId = "testOperationId";
        testCbSaltVersion = "2.69.0";
    }

    @Test
    void testInitializeMachineUserForEnvironment() {
        Cluster cluster = getACluster();
        MachineUser machineUser = MachineUser.newBuilder().setCrn(autoscaleMachineUserCrn).build();

        when(grpcUmsClient.getOrCreateMachineUserWithoutAccessKey(autoscaleMachineUserName, testAccountId))
                .thenReturn(machineUser);
        when(grpcUmsClient.listAssignedResourceRoles(anyString())).thenReturn(LinkedHashMultimap.create());
        when(roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn(anyString())).thenReturn(environmentRoleCrn);
        when(cloudbreakVersionService.getCloudbreakSaltStateVersionByStackCrn(anyString())).thenReturn(testCbSaltVersion);
        when(freeIpaCommunicator.synchronizeAllUsers(any(SynchronizeAllUsersRequest.class))).thenReturn(getSyncOpStatus(SynchronizationStatus.COMPLETED));

        underTest.initializeMachineUserForEnvironment(cluster);

        verify(grpcUmsClient, times(1)).assignResourceRole(eq(autoscaleMachineUserCrn), eq(testEnvironmentCrn), eq(environmentRoleCrn));
        verify(clusterService, times(1)).setMachineUserCrn(cluster.getId(), autoscaleMachineUserCrn);

        ArgumentCaptor<SynchronizeAllUsersRequest> synchronizeUserCaptor = ArgumentCaptor.forClass(SynchronizeAllUsersRequest.class);
        verify(freeIpaCommunicator, times(1)).synchronizeAllUsers(synchronizeUserCaptor.capture());

        SynchronizeAllUsersRequest synchronizeAllUsersRequest = synchronizeUserCaptor.getValue();
        assertEquals(synchronizeAllUsersRequest.getEnvironments(), Set.of(testEnvironmentCrn), "Environment Crn Should match");
        assertEquals(synchronizeAllUsersRequest.getMachineUsers(), Set.of(autoscaleMachineUserCrn), "Machine User Crn Should match");
        assertEquals(synchronizeAllUsersRequest.getAccountId(), cluster.getClusterPertain().getTenant(), "Account Id Should match");
    }

    @Test
    void testinitializeMachineUserForEnvironmentWhenRoleAlreadyAssigned() {
        Cluster cluster = getACluster();
        MachineUser machineUser = MachineUser.newBuilder().setCrn(autoscaleMachineUserCrn).build();

        LinkedHashMultimap rolesMap = LinkedHashMultimap.create();
        rolesMap.put(cluster.getEnvironmentCrn(), environmentRoleCrn);

        when(grpcUmsClient.getOrCreateMachineUserWithoutAccessKey(autoscaleMachineUserName, testAccountId))
                .thenReturn(machineUser);
        when(grpcUmsClient.listAssignedResourceRoles(anyString())).thenReturn(rolesMap);
        when(roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn(anyString())).thenReturn(environmentRoleCrn);
        when(cloudbreakVersionService.getCloudbreakSaltStateVersionByStackCrn(anyString())).thenReturn(testCbSaltVersion);
        when(freeIpaCommunicator.synchronizeAllUsers(any(SynchronizeAllUsersRequest.class))).thenReturn(getSyncOpStatus(SynchronizationStatus.COMPLETED));

        underTest.initializeMachineUserForEnvironment(cluster);

        verify(grpcUmsClient, times(0)).assignResourceRole(eq(autoscaleMachineUserCrn), eq(testEnvironmentCrn), eq(environmentRoleCrn));
        verify(clusterService, times(1)).setMachineUserCrn(cluster.getId(), autoscaleMachineUserCrn);

        ArgumentCaptor<SynchronizeAllUsersRequest> synchronizeUserCaptor = ArgumentCaptor.forClass(SynchronizeAllUsersRequest.class);
        verify(freeIpaCommunicator, times(1)).synchronizeAllUsers(synchronizeUserCaptor.capture());

        SynchronizeAllUsersRequest synchronizeAllUsersRequest = synchronizeUserCaptor.getValue();
        assertEquals(synchronizeAllUsersRequest.getEnvironments(), Set.of(testEnvironmentCrn), "Environment Crn Should match");
        assertEquals(synchronizeAllUsersRequest.getMachineUsers(), Set.of(autoscaleMachineUserCrn), "Machine User Crn Should match");
        assertEquals(synchronizeAllUsersRequest.getAccountId(), cluster.getClusterPertain().getTenant(), "Account Id Should match");
    }

    @Test
    void testInitializeMachineUserForClusterWithCBVersionGreaterThanCbThreshold() {
        Cluster cluster = getACluster();
        MachineUser machineUser = mock(MachineUser.class);

        when(machineUser.getCrn()).thenReturn(autoscaleMachineUserCrn);
        when(grpcUmsClient.getOrCreateMachineUserWithoutAccessKey(autoscaleMachineUserName, testAccountId))
                .thenReturn(machineUser);
        when(cloudbreakVersionService.getCloudbreakSaltStateVersionByStackCrn(anyString())).thenReturn("2.71.0");

        underTest.initializeMachineUserForEnvironment(cluster);

        verify(clusterService, times(1)).setMachineUserCrn(cluster.getId(), autoscaleMachineUserCrn);
        verify(grpcUmsClient, never()).assignResourceRole(anyString(), anyString(), anyString());
        verify(grpcUmsClient, never()).listAssignedResourceRoles(anyString());
        verifyNoInteractions(freeIpaCommunicator);
    }

    @Test
    void testDeleteMachineUserForEnvironment() {
        MachineUser machineUser = MachineUser.newBuilder().setCrn(autoscaleMachineUserCrn).setWorkloadUsername("workloadUserName").build();
        when(roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn(anyString())).thenReturn(environmentRoleCrn);
        when(grpcUmsClient.getOrCreateMachineUserWithoutAccessKey(eq(autoscaleMachineUserName),
                eq("testTenant"))).thenReturn(machineUser);
        when(freeIpaCommunicator.synchronizeAllUsers(any(SynchronizeAllUsersRequest.class))).thenReturn(getSyncOpStatus(SynchronizationStatus.COMPLETED));

        underTest.deleteMachineUserForEnvironment(testAccountId, autoscaleMachineUserCrn, testEnvironmentCrn);

        verify(grpcUmsClient, times(1)).deleteMachineUser(eq(autoscaleMachineUserCrn), eq(testAccountId));
        ArgumentCaptor<SynchronizeAllUsersRequest> synchronizeUserCaptor = ArgumentCaptor.forClass(SynchronizeAllUsersRequest.class);
        verify(freeIpaCommunicator, times(1)).synchronizeAllUsers(synchronizeUserCaptor.capture());
        SynchronizeAllUsersRequest synchronizeAllUsersRequest = synchronizeUserCaptor.getValue();
        assertEquals(synchronizeAllUsersRequest.getDeletedWorkloadUsers(), Set.of("workloadUserName"), "WorkloadUserName Should match");
        assertEquals(synchronizeAllUsersRequest.getEnvironments(), Set.of(testEnvironmentCrn), "Environment Crn Should match");
        assertEquals(synchronizeAllUsersRequest.getMachineUsers(), Set.of(autoscaleMachineUserCrn), "Machine User Crn Should match");
        assertEquals(synchronizeAllUsersRequest.getAccountId(), testAccountId, "Account Id Should match");
    }

    protected Cluster getACluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(testClusterCrn);
        cluster.setEnvironmentCrn(testEnvironmentCrn);
        cluster.setId(10);

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(testAccountId);
        cluster.setClusterPertain(clusterPertain);
        return cluster;
    }

    private SyncOperationStatus getSyncOpStatus(SynchronizationStatus status) {
        return new SyncOperationStatus(testOperationId,
                SyncOperationType.USER_SYNC, status, List.of(),
                List.of(),
                "",
                System.currentTimeMillis(), null);
    }
}
