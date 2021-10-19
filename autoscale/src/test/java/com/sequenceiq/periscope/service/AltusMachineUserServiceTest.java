package com.sequenceiq.periscope.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.google.common.collect.LinkedHashMultimap;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;

public class AltusMachineUserServiceTest {

    @InjectMocks
    AltusMachineUserService underTest;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private UserV1Endpoint userV1Endpoint;

    @Mock
    private ClusterService clusterService;

    @Mock
    private RoleCrnGenerator roleCrnGenerator;

    private String testEnvironmentCrn;

    private String testAccountId;

    private String autoscaleMachineUserName;

    private String autoscaleMachineUserCrn;

    private String environmentRoleCrn;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        testEnvironmentCrn = "crn:cdp:environments:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:environment:1584cdfa-ad2f-45ff-b3d9-414b5b013001";
        testAccountId = "testTenant";
        autoscaleMachineUserName = "datahub-autoscale-metrics-1584cdfa-ad2f-45ff-b3d9-414b5b013001";
        autoscaleMachineUserCrn = "testMachineUserCrn";
        environmentRoleCrn = "environmentuserRoleCrn";
    }

    @Test
    public void testInitializeMachineUserForEnvironment() {
        Cluster cluster = getACluster();
        MachineUser machineUser = mock(MachineUser.class);

        when(machineUser.getCrn()).thenReturn(autoscaleMachineUserCrn);
        when(grpcUmsClient.getOrCreateMachineUserWithoutAccessKey(autoscaleMachineUserName, testAccountId, MDCUtils.getRequestId()))
                .thenReturn(machineUser);
        when(grpcUmsClient.listAssignedResourceRoles(anyString(), any(Optional.class)))
                .thenReturn(LinkedHashMultimap.create());
        when(roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn()).thenReturn(environmentRoleCrn);

        underTest.initializeMachineUserForEnvironment(cluster);

        verify(grpcUmsClient, times(1)).assignResourceRole(
                eq(autoscaleMachineUserCrn), eq(testEnvironmentCrn), eq(environmentRoleCrn), any(Optional.class));
        verify(clusterService, times(1)).setMachineUserCrn(cluster.getId(), autoscaleMachineUserCrn);

        ArgumentCaptor<SynchronizeAllUsersRequest> synchronizeUserCaptor = ArgumentCaptor.forClass(SynchronizeAllUsersRequest.class);
        verify(userV1Endpoint, times(1)).synchronizeAllUsers(synchronizeUserCaptor.capture());

        SynchronizeAllUsersRequest synchronizeAllUsersRequest = synchronizeUserCaptor.getValue();
        assertEquals("Environment Crn Should match", synchronizeAllUsersRequest.getEnvironments(), Set.of(testEnvironmentCrn));
        assertEquals("Machine User Crn Should match", synchronizeAllUsersRequest.getMachineUsers(), Set.of(autoscaleMachineUserCrn));
        assertEquals("Account Id Should match", synchronizeAllUsersRequest.getAccountId(), cluster.getClusterPertain().getTenant());
    }

    @Test
    public void testinitializeMachineUserForEnvironmentWhenRoleAlreadyAssigned() {
        Cluster cluster = getACluster();
        MachineUser machineUser = mock(MachineUser.class);

        LinkedHashMultimap rolesMap = LinkedHashMultimap.create();
        rolesMap.put(cluster.getEnvironmentCrn(), environmentRoleCrn);

        when(grpcUmsClient.getOrCreateMachineUserWithoutAccessKey(autoscaleMachineUserName, testAccountId, MDCUtils.getRequestId()))
                .thenReturn(machineUser);
        when(machineUser.getCrn()).thenReturn(autoscaleMachineUserCrn);
        when(grpcUmsClient.listAssignedResourceRoles(anyString(), any(Optional.class))).thenReturn(rolesMap);
        when(roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn()).thenReturn(environmentRoleCrn);

        underTest.initializeMachineUserForEnvironment(cluster);

        verify(grpcUmsClient, times(0)).assignResourceRole(
                eq(autoscaleMachineUserCrn), eq(testEnvironmentCrn), eq(environmentRoleCrn), any(Optional.class));
        verify(clusterService, times(1)).setMachineUserCrn(cluster.getId(), autoscaleMachineUserCrn);

        ArgumentCaptor<SynchronizeAllUsersRequest> synchronizeUserCaptor = ArgumentCaptor.forClass(SynchronizeAllUsersRequest.class);
        verify(userV1Endpoint, times(1)).synchronizeAllUsers(synchronizeUserCaptor.capture());

        SynchronizeAllUsersRequest synchronizeAllUsersRequest = synchronizeUserCaptor.getValue();
        assertEquals("Environment Crn Should match", synchronizeAllUsersRequest.getEnvironments(), Set.of(testEnvironmentCrn));
        assertEquals("Machine User Crn Should match", synchronizeAllUsersRequest.getMachineUsers(), Set.of(autoscaleMachineUserCrn));
        assertEquals("Account Id Should match", synchronizeAllUsersRequest.getAccountId(), cluster.getClusterPertain().getTenant());
    }

    @Test
    public void testDeleteMachineUserForEnvironment() {
        MachineUser machineUserMock = mock(MachineUser.class);
        when(machineUserMock.getCrn()).thenReturn(autoscaleMachineUserCrn);
        when(machineUserMock.getWorkloadUsername()).thenReturn("workloadUserName");
        when(roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn()).thenReturn(environmentRoleCrn);
        when(grpcUmsClient.getOrCreateMachineUserWithoutAccessKey(eq(autoscaleMachineUserName),
                eq("testTenant"), any(Optional.class))).thenReturn(machineUserMock);

        underTest.deleteMachineUserForEnvironment(testAccountId, autoscaleMachineUserCrn, testEnvironmentCrn);

        verify(grpcUmsClient, times(1)).deleteMachineUser(
                eq(autoscaleMachineUserCrn), eq(ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN), eq(testAccountId), any(Optional.class));
        ArgumentCaptor<SynchronizeAllUsersRequest> synchronizeUserCaptor = ArgumentCaptor.forClass(SynchronizeAllUsersRequest.class);
        verify(userV1Endpoint, times(1)).synchronizeAllUsers(synchronizeUserCaptor.capture());
        SynchronizeAllUsersRequest synchronizeAllUsersRequest = synchronizeUserCaptor.getValue();
        assertEquals("WorkloadUserName Should match", synchronizeAllUsersRequest.getDeletedWorkloadUsers(), Set.of("workloadUserName"));
        assertEquals("Environment Crn Should match", synchronizeAllUsersRequest.getEnvironments(), Set.of(testEnvironmentCrn));
        assertEquals("Machine User Crn Should match", synchronizeAllUsersRequest.getMachineUsers(), Set.of(autoscaleMachineUserCrn));
        assertEquals("Account Id Should match", synchronizeAllUsersRequest.getAccountId(), testAccountId);
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
}
