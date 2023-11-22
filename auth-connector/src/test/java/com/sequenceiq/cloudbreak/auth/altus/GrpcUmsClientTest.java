package com.sequenceiq.cloudbreak.auth.altus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.cloudera.thunderhead.service.common.paging.PagingProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyType.Value;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ServicePrincipalCloudIdentities;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.exception.UnauthorizedException;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@ExtendWith(MockitoExtension.class)
public class GrpcUmsClientTest {

    private static final String USER_CRN = CrnTestUtil.getUserCrnBuilder()
            .setResource("user")
            .setAccountId("acc")
            .build().toString();

    private static final String RESOURCE_CRN = CrnTestUtil.getEnvironmentCrnBuilder()
            .setResource(UUID.randomUUID().toString())
            .setAccountId("account")
            .build().toString();

    @Mock
    private ManagedChannelWrapper channelWrapper;

    @Mock
    private UmsClientConfig umsClientConfig;

    @InjectMocks
    private GrpcUmsClient rawGrpcUmsClient;

    private GrpcUmsClient underTest;

    @Mock
    private UmsClient umsClient;

    @Mock
    private AuthorizationClient authorizationClient;

    @Mock
    private ManagedChannel managedChannel;

    @Captor
    private ArgumentCaptor<Iterable<AuthorizationProto.RightCheck>> captor;

    @BeforeEach
    public void setUp() {
        underTest = spy(rawGrpcUmsClient);
        lenient().doReturn(managedChannel).when(channelWrapper).getChannel();
        lenient().doReturn(umsClient).when(underTest).makeClient();
        lenient().doReturn(authorizationClient).when(underTest).makeAuthorizationClient();
    }

    @Test
    public void testListWorkloadAdministrationGroupsForMember() {
        String memberCrn = "fake-crn";
        List<String> wags1 = List.of("wag1a", "wag1b");
        List<String> wags2 = List.of("wag2a", "wag2b");

        PagingProto.PageToken pageToken = PagingProto.PageToken.getDefaultInstance();
        UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse response1 =
                UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse.newBuilder()
                        .addAllWorkloadAdministrationGroupName(wags1)
                        .setNextPageToken(pageToken)
                        .build();
        UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse response2 =
                UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse.newBuilder()
                        .addAllWorkloadAdministrationGroupName(wags2)
                        .build();
        when(umsClient.listWorkloadAdministrationGroupsForMember(eq(memberCrn), eq(Optional.empty())))
                .thenReturn(response1);
        when(umsClient.listWorkloadAdministrationGroupsForMember(eq(memberCrn), eq(Optional.of(pageToken))))
                .thenReturn(response2);

        List<String> wags = underTest.listWorkloadAdministrationGroupsForMember(memberCrn);

        verify(umsClient, times(2)).listWorkloadAdministrationGroupsForMember(eq(memberCrn), any(Optional.class));
        assertTrue(wags.containsAll(wags1));
        assertTrue(wags.containsAll(wags2));
    }

    @Test
    public void testListServicePrincipalCloudIdentities() {
        String accountId = "accountId";
        String envCrn = "envCrn";
        ServicePrincipalCloudIdentities spIds01 = ServicePrincipalCloudIdentities.newBuilder().build();
        ServicePrincipalCloudIdentities spIds02 = ServicePrincipalCloudIdentities.newBuilder().build();
        ServicePrincipalCloudIdentities spIds03 = ServicePrincipalCloudIdentities.newBuilder().build();
        ServicePrincipalCloudIdentities spIds04 = ServicePrincipalCloudIdentities.newBuilder().build();

        List<ServicePrincipalCloudIdentities> spIdsList01 = List.of(spIds01, spIds02);
        List<ServicePrincipalCloudIdentities> spIdsList02 = List.of(spIds03, spIds04);

        PagingProto.PageToken pageToken = PagingProto.PageToken.getDefaultInstance();
        UserManagementProto.ListServicePrincipalCloudIdentitiesResponse response1 =
                UserManagementProto.ListServicePrincipalCloudIdentitiesResponse.newBuilder()
                        .addAllServicePrincipalCloudIdentities(spIdsList01)
                        .setNextPageToken(pageToken)
                        .build();
        UserManagementProto.ListServicePrincipalCloudIdentitiesResponse response2 =
                UserManagementProto.ListServicePrincipalCloudIdentitiesResponse.newBuilder()
                        .addAllServicePrincipalCloudIdentities(spIdsList02)
                        .build();
        when(umsClient.listServicePrincipalCloudIdentities(eq(accountId), eq(envCrn), eq(Optional.empty())))
                .thenReturn(response1);
        when(umsClient.listServicePrincipalCloudIdentities(eq(accountId), eq(envCrn), eq(Optional.of(pageToken))))
                .thenReturn(response2);

        List<ServicePrincipalCloudIdentities> spCloudIds = underTest.listServicePrincipalCloudIdentities(accountId, envCrn);

        verify(umsClient, times(2)).listServicePrincipalCloudIdentities(eq(accountId), eq(envCrn), any(Optional.class));
        assertTrue(spCloudIds.containsAll(spIdsList01));
        assertTrue(spCloudIds.containsAll(spIdsList02));
    }

    @Test
    public void testCheckRightWithInvalidCrn() {
        assertEquals(
                assertThrows(IllegalArgumentException.class, () -> underTest.checkResourceRight(USER_CRN,
                        "environments/describeEnvironment", "invalidCrn")).getMessage(),
                "Provided resource [invalidCrn] is not in CRN format");
    }

    @Test
    public void testCheckRightWhenNotFound() {
        doThrow(new StatusRuntimeException(Status.NOT_FOUND)).when(authorizationClient).checkRight(any(), any(), any());
        UnauthorizedException unauthorizedException = assertThrows(
                UnauthorizedException.class,
                () -> underTest.checkResourceRight(USER_CRN, "environments/describeEnvironment", RESOURCE_CRN));
        assertNotNull(unauthorizedException);
        assertEquals("Authorization failed for user: " + USER_CRN, unauthorizedException.getMessage());
    }

    @Test
    public void testHasRightsWithInvalidCrn() {
        assertEquals(
                assertThrows(IllegalArgumentException.class, () -> underTest.hasRights(USER_CRN,
                        List.of("invalidCrn", "*"), "environments/describeEnvironment")).getMessage(),
                "Following resources are not provided in CRN format: invalidCrn.");
        assertEquals(
                assertThrows(IllegalArgumentException.class, () -> underTest.hasRightsOnResources(USER_CRN,
                        List.of("invalidCrn", "*"), "environments/describeEnvironment")).getMessage(),
                "Following resources are not provided in CRN format: invalidCrn.");
    }

    @Test
    public void testHasRightsUsesCheckRightWhenRequestNumberIsLessThenThreshold() {
        String resourceCrn1 = CrnTestUtil.getDatahubCrnBuilder().setAccountId("a1").setResource("r1").build().toString();
        String resourceCrn2 = CrnTestUtil.getDatahubCrnBuilder().setAccountId("a1").setResource("r2").build().toString();
        doNothing().when(authorizationClient).checkRight(USER_CRN, "right", resourceCrn1);
        doThrow(new RuntimeException("Permission denied")).when(authorizationClient).checkRight(USER_CRN, "right", resourceCrn2);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> {
            underTest.hasRights(USER_CRN, List.of(resourceCrn1, resourceCrn2), "right");
        });

        assertEquals("Authorization failed due to user management service call failed with error.", exception.getMessage());
        InOrder inOrder = inOrder(authorizationClient);
        inOrder.verify(authorizationClient).checkRight(USER_CRN, "right", resourceCrn1);
        inOrder.verify(authorizationClient).checkRight(USER_CRN, "right", resourceCrn2);
    }

    @Test
    public void testHasRightsUsesHasRightsWhenRequestNumberIsGreaterThenThreshold() {
        String resourceCrn1 = CrnTestUtil.getDatahubCrnBuilder().setAccountId("a1").setResource("r1").build().toString();
        String resourceCrn2 = CrnTestUtil.getDatahubCrnBuilder().setAccountId("a1").setResource("r2").build().toString();
        String resourceCrn3 = CrnTestUtil.getDatahubCrnBuilder().setAccountId("a1").setResource("r3").build().toString();
        doAnswer(m -> Lists.newArrayList((Iterable<AuthorizationProto.RightCheck>) m.getArgument(1)).stream().map(i -> true).collect(Collectors.toList()))
                .when(authorizationClient).hasRights(anyString(), any());

        underTest.hasRights(USER_CRN, List.of(resourceCrn1, resourceCrn2, resourceCrn3), "right");

        verify(authorizationClient).hasRights(eq(USER_CRN), captor.capture());

        List<AuthorizationProto.RightCheck> rightChecks = Lists.newArrayList(captor.getValue());
        assertEquals(List.of(
                AuthorizationProto.RightCheck.newBuilder()
                        .setResource(resourceCrn1)
                        .setRight("right")
                        .build(),
                AuthorizationProto.RightCheck.newBuilder()
                        .setResource(resourceCrn2)
                        .setRight("right")
                        .build(),
                AuthorizationProto.RightCheck.newBuilder()
                        .setResource(resourceCrn3)
                        .setRight("right")
                        .build()),
                rightChecks);
    }

    @Test
    public void testAssignMachineUserResourceRole() {
        underTest.assignMachineUserResourceRole("accountId", "machineUserCrn", "resourceRoleCrn", "resourceCrn");

        verify(umsClient, times(1)).assignMachineUserResourceRole(eq("accountId"), eq("machineUserCrn"),
                eq("resourceRoleCrn"), eq("resourceCrn"));
    }

    @Test
    public void testCreateMachineUserAndGenerateKeysWithResourceRoles() {
        when(umsClient.createMachineUser(eq("accountId"), eq("machineUserName"))).thenReturn(Optional.of("machineUserCrn"));
        UserManagementProto.CreateAccessKeyResponse createAccessKeyResponse = UserManagementProto.CreateAccessKeyResponse.newBuilder()
                .setAccessKey(UserManagementProto.AccessKey.newBuilder().setAccessKeyId("accessKeyId").build())
                .setPrivateKey("privateKey")
                .build();
        when(umsClient.createAccessPrivateKeyPair(eq("accountId"), eq("machineUserCrn"), any())).thenReturn(createAccessKeyResponse);

        underTest.createMachineUserAndGenerateKeys("machineUserName", "userCrn", "accountId", "roleCrn",
                Map.of("resourceCrn", "resourceRoleCrn"));
        verify(umsClient, times(1)).createMachineUser(eq("accountId"), eq("machineUserName"));
        verify(umsClient, times(1)).assignMachineUserRole(eq("accountId"), eq("machineUserCrn"), eq("roleCrn"));
        verify(umsClient, times(1)).assignMachineUserResourceRole(eq("accountId"), eq("machineUserCrn"), eq("resourceRoleCrn"), eq("resourceCrn"));
    }

    @Test
    public void testCreateMachineUserAndGenerateKeysWithMultipleRoleCrns() {
        when(umsClient.createMachineUser(eq("accountId"), eq("machineUserName"))).thenReturn(Optional.of("machineUserCrn"));
        UserManagementProto.CreateAccessKeyResponse createAccessKeyResponse = UserManagementProto.CreateAccessKeyResponse.newBuilder()
                .setAccessKey(UserManagementProto.AccessKey.newBuilder().setAccessKeyId("accessKeyId").build())
                .setPrivateKey("privateKey")
                .build();
        when(umsClient.createAccessPrivateKeyPair(eq("accountId"), eq("machineUserCrn"), any())).thenReturn(createAccessKeyResponse);

        underTest.createMachineUserAndGenerateKeys("machineUserName", "userCrn", "accountId", Set.of("roleCrn1", "roleCrn2"),
                Map.of("resourceCrn", "resourceRoleCrn"), Value.UNSET);
        verify(umsClient, times(1)).createMachineUser(eq("accountId"), eq("machineUserName"));
        verify(umsClient, times(1)).assignMachineUserRole(eq("accountId"), eq("machineUserCrn"), eq("roleCrn1"));
        verify(umsClient, times(1)).assignMachineUserRole(eq("accountId"), eq("machineUserCrn"), eq("roleCrn2"));
        verify(umsClient, times(1)).assignMachineUserResourceRole(eq("accountId"), eq("machineUserCrn"), eq("resourceRoleCrn"),
                eq("resourceCrn"));
    }
}
