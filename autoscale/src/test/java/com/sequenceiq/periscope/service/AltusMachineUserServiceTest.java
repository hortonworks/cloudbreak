package com.sequenceiq.periscope.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.google.common.collect.LinkedHashMultimap;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.monitor.handler.FreeIpaCommunicator;

class AltusMachineUserServiceTest {

    @InjectMocks
    AltusMachineUserService underTest;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private ClusterService clusterService;

    @Mock
    private RoleCrnGenerator roleCrnGenerator;

    @Mock
    private FreeIpaCommunicator freeIpaCommunicator;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private String testEnvironmentCrn;

    private String testAccountId;

    private String autoscaleMachineUserName;

    private String autoscaleMachineUserCrn;

    private String environmentRoleCrn;

    private String testOperationId;

    private String internalActorCrn;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        testEnvironmentCrn = "crn:cdp:environments:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:environment:1584cdfa-ad2f-45ff-b3d9-414b5b013001";
        testAccountId = "testTenant";
        autoscaleMachineUserName = "datahub-autoscale-metrics-1584cdfa-ad2f-45ff-b3d9-414b5b013001";
        autoscaleMachineUserCrn = "testMachineUserCrn";
        environmentRoleCrn = "environmentuserRoleCrn";
        testOperationId = "testOperationId";
        internalActorCrn = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";
    }

    @Test
    void testInitializeMachineUserForEnvironment() {
        Cluster cluster = getACluster();
        MachineUser machineUser = mock(MachineUser.class);

        when(machineUser.getCrn()).thenReturn(autoscaleMachineUserCrn);
        when(grpcUmsClient.getOrCreateMachineUserWithoutAccessKey(autoscaleMachineUserName, testAccountId))
                .thenReturn(machineUser);
        when(grpcUmsClient.listAssignedResourceRoles(anyString(), any(RegionAwareInternalCrnGeneratorFactory.class)))
                .thenReturn(LinkedHashMultimap.create());
        when(roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn(anyString())).thenReturn(environmentRoleCrn);
        when(freeIpaCommunicator.synchronizeAllUsers(any(SynchronizeAllUsersRequest.class))).thenReturn(getSyncOpStatus(SynchronizationStatus.COMPLETED));

        underTest.initializeMachineUserForEnvironment(cluster);

        verify(grpcUmsClient, times(1)).assignResourceRole(
                eq(autoscaleMachineUserCrn), eq(testEnvironmentCrn), eq(environmentRoleCrn),
                any(RegionAwareInternalCrnGeneratorFactory.class));
        verify(clusterService, times(1)).setMachineUserCrn(cluster.getId(), autoscaleMachineUserCrn);

        ArgumentCaptor<SynchronizeAllUsersRequest> synchronizeUserCaptor = ArgumentCaptor.forClass(SynchronizeAllUsersRequest.class);
        verify(freeIpaCommunicator, times(1)).synchronizeAllUsers(synchronizeUserCaptor.capture());

        SynchronizeAllUsersRequest synchronizeAllUsersRequest = synchronizeUserCaptor.getValue();
        Assertions.assertEquals(synchronizeAllUsersRequest.getEnvironments(), Set.of(testEnvironmentCrn), "Environment Crn Should match");
        Assertions.assertEquals(synchronizeAllUsersRequest.getMachineUsers(), Set.of(autoscaleMachineUserCrn), "Machine User Crn Should match");
        Assertions.assertEquals(synchronizeAllUsersRequest.getAccountId(), cluster.getClusterPertain().getTenant(), "Account Id Should match");
    }

    @Test
    void testinitializeMachineUserForEnvironmentWhenRoleAlreadyAssigned() {
        Cluster cluster = getACluster();
        MachineUser machineUser = mock(MachineUser.class);

        LinkedHashMultimap rolesMap = LinkedHashMultimap.create();
        rolesMap.put(cluster.getEnvironmentCrn(), environmentRoleCrn);

        when(grpcUmsClient.getOrCreateMachineUserWithoutAccessKey(autoscaleMachineUserName, testAccountId))
                .thenReturn(machineUser);
        when(machineUser.getCrn()).thenReturn(autoscaleMachineUserCrn);
        when(grpcUmsClient.listAssignedResourceRoles(anyString(), any(RegionAwareInternalCrnGeneratorFactory.class))).thenReturn(rolesMap);
        when(roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn(anyString())).thenReturn(environmentRoleCrn);
        when(freeIpaCommunicator.synchronizeAllUsers(any(SynchronizeAllUsersRequest.class))).thenReturn(getSyncOpStatus(SynchronizationStatus.COMPLETED));

        underTest.initializeMachineUserForEnvironment(cluster);

        verify(grpcUmsClient, times(0)).assignResourceRole(
                eq(autoscaleMachineUserCrn), eq(testEnvironmentCrn), eq(environmentRoleCrn),
                any(RegionAwareInternalCrnGeneratorFactory.class));
        verify(clusterService, times(1)).setMachineUserCrn(cluster.getId(), autoscaleMachineUserCrn);

        ArgumentCaptor<SynchronizeAllUsersRequest> synchronizeUserCaptor = ArgumentCaptor.forClass(SynchronizeAllUsersRequest.class);
        verify(freeIpaCommunicator, times(1)).synchronizeAllUsers(synchronizeUserCaptor.capture());

        SynchronizeAllUsersRequest synchronizeAllUsersRequest = synchronizeUserCaptor.getValue();
        Assertions.assertEquals(synchronizeAllUsersRequest.getEnvironments(), Set.of(testEnvironmentCrn), "Environment Crn Should match");
        Assertions.assertEquals(synchronizeAllUsersRequest.getMachineUsers(), Set.of(autoscaleMachineUserCrn), "Machine User Crn Should match");
        Assertions.assertEquals(synchronizeAllUsersRequest.getAccountId(), cluster.getClusterPertain().getTenant(), "Account Id Should match");
    }

    @Test
    void testDeleteMachineUserForEnvironment() {
        MachineUser machineUserMock = mock(MachineUser.class);
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        when(machineUserMock.getCrn()).thenReturn(autoscaleMachineUserCrn);
        when(machineUserMock.getWorkloadUsername()).thenReturn("workloadUserName");
        when(roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn(anyString())).thenReturn(environmentRoleCrn);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(internalActorCrn);
        when(grpcUmsClient.getOrCreateMachineUserWithoutAccessKey(eq(autoscaleMachineUserName),
                eq("testTenant"))).thenReturn(machineUserMock);
        when(freeIpaCommunicator.synchronizeAllUsers(any(SynchronizeAllUsersRequest.class))).thenReturn(getSyncOpStatus(SynchronizationStatus.COMPLETED));

        underTest.deleteMachineUserForEnvironment(testAccountId, autoscaleMachineUserCrn, testEnvironmentCrn);

        verify(grpcUmsClient, times(1)).deleteMachineUser(
                eq(autoscaleMachineUserCrn), eq(internalActorCrn), eq(testAccountId), any(RegionAwareInternalCrnGeneratorFactory.class));
        ArgumentCaptor<SynchronizeAllUsersRequest> synchronizeUserCaptor = ArgumentCaptor.forClass(SynchronizeAllUsersRequest.class);
        verify(freeIpaCommunicator, times(1)).synchronizeAllUsers(synchronizeUserCaptor.capture());
        SynchronizeAllUsersRequest synchronizeAllUsersRequest = synchronizeUserCaptor.getValue();
        Assertions.assertEquals(synchronizeAllUsersRequest.getDeletedWorkloadUsers(), Set.of("workloadUserName"), "WorkloadUserName Should match");
        Assertions.assertEquals(synchronizeAllUsersRequest.getEnvironments(), Set.of(testEnvironmentCrn), "Environment Crn Should match");
        Assertions.assertEquals(synchronizeAllUsersRequest.getMachineUsers(), Set.of(autoscaleMachineUserCrn), "Machine User Crn Should match");
        Assertions.assertEquals(synchronizeAllUsersRequest.getAccountId(), testAccountId, "Account Id Should match");
    }

    protected Cluster getACluster() {
        Cluster cluster = new Cluster();
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
