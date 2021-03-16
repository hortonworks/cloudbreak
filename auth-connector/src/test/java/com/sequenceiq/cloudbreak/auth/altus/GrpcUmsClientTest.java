package com.sequenceiq.cloudbreak.auth.altus;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ServicePrincipalCloudIdentities;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;

@ExtendWith(MockitoExtension.class)
public class GrpcUmsClientTest {

    private static final String USER_CRN = Crn.builder(CrnResourceDescriptor.USER)
            .setResource("user")
            .setAccountId("acc")
            .build().toString();

    private static final Optional<String> REQUEST_ID = Optional.of("requeest-id");

    @Mock
    private ManagedChannelWrapper channelWrapper;

    @Mock
    private UmsClientConfig umsClientConfig;

    @Mock
    private Tracer tracer;

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
        lenient().doReturn(umsClient).when(underTest).makeClient(any(ManagedChannel.class), anyString());
        lenient().doReturn(authorizationClient).when(underTest).makeAuthorizationClient(anyString());
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
        when(umsClient.listWorkloadAdministrationGroupsForMember(anyString(), eq(memberCrn), eq(Optional.empty())))
                .thenReturn(response1);
        when(umsClient.listWorkloadAdministrationGroupsForMember(anyString(), eq(memberCrn), eq(Optional.of(pageToken))))
                .thenReturn(response2);

        List<String> wags = underTest.listWorkloadAdministrationGroupsForMember("actor-crn", memberCrn, REQUEST_ID);

        verify(umsClient, times(2)).listWorkloadAdministrationGroupsForMember(anyString(), eq(memberCrn), any(Optional.class));
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
        when(umsClient.listServicePrincipalCloudIdentities(anyString(), eq(accountId), eq(envCrn), eq(Optional.empty())))
                .thenReturn(response1);
        when(umsClient.listServicePrincipalCloudIdentities(anyString(), eq(accountId), eq(envCrn), eq(Optional.of(pageToken))))
                .thenReturn(response2);

        List<ServicePrincipalCloudIdentities> spCloudIds = underTest.listServicePrincipalCloudIdentities("actor-crn", accountId, envCrn,
                REQUEST_ID);

        verify(umsClient, times(2)).listServicePrincipalCloudIdentities(anyString(), eq(accountId), eq(envCrn), any(Optional.class));
        assertTrue(spCloudIds.containsAll(spIdsList01));
        assertTrue(spCloudIds.containsAll(spIdsList02));
    }

    @Test
    public void testCheckRightWithInvalidCrn() {
        assertEquals(
                assertThrows(IllegalArgumentException.class, () -> underTest.checkResourceRight(USER_CRN, USER_CRN,
                        "environments/describeEnvironment", "invalidCrn", REQUEST_ID)).getMessage(),
                "Provided resource [invalidCrn] is not in CRN format");
    }

    @Test
    public void testHasRightsWithInvalidCrn() {
        assertEquals(
                assertThrows(IllegalArgumentException.class, () -> underTest.hasRights(USER_CRN, USER_CRN,
                        List.of("invalidCrn", "*"), "environments/describeEnvironment", REQUEST_ID)).getMessage(),
                "Following resources are not provided in CRN format: invalidCrn.");
        assertEquals(
                assertThrows(IllegalArgumentException.class, () -> underTest.hasRightsOnResources(USER_CRN, USER_CRN,
                        List.of("invalidCrn", "*"), "environments/describeEnvironment", REQUEST_ID)).getMessage(),
                "Following resources are not provided in CRN format: invalidCrn.");
    }

    @Test
    public void testHasRightsUsesCheckRightWhenRequestNumberIsLessThenThreshold() {
        String resourceCrn1 = Crn.builder(CrnResourceDescriptor.DATAHUB).setAccountId("a1").setResource("r1").build().toString();
        String resourceCrn2 = Crn.builder(CrnResourceDescriptor.DATAHUB).setAccountId("a1").setResource("r2").build().toString();
        doNothing().when(authorizationClient).checkRight(REQUEST_ID.get(), USER_CRN, "right", resourceCrn1);
        doThrow(new RuntimeException("Permission denied")).when(authorizationClient).checkRight(REQUEST_ID.get(), USER_CRN, "right", resourceCrn2);

        Map<String, Boolean> result = underTest.hasRights(USER_CRN, USER_CRN, List.of(resourceCrn1, resourceCrn2), "right", REQUEST_ID);

        assertEquals(Map.of(resourceCrn1, true, resourceCrn2, false), result);
        InOrder inOrder = inOrder(authorizationClient);
        inOrder.verify(authorizationClient).checkRight(REQUEST_ID.get(), USER_CRN, "right", resourceCrn1);
        inOrder.verify(authorizationClient).checkRight(REQUEST_ID.get(), USER_CRN, "right", resourceCrn2);
    }

    @Test
    public void testHasRightsUsesHasRightsWhenRequestNumberIsGreaterThenThreshold() {
        String resourceCrn1 = Crn.builder(CrnResourceDescriptor.DATAHUB).setAccountId("a1").setResource("r1").build().toString();
        String resourceCrn2 = Crn.builder(CrnResourceDescriptor.DATAHUB).setAccountId("a1").setResource("r2").build().toString();
        String resourceCrn3 = Crn.builder(CrnResourceDescriptor.DATAHUB).setAccountId("a1").setResource("r3").build().toString();
        doAnswer(m -> Lists.newArrayList((Iterable<AuthorizationProto.RightCheck>) m.getArgument(2)).stream().map(i -> true).collect(Collectors.toList()))
                .when(authorizationClient).hasRights(any(), anyString(), any());

        underTest.hasRights(USER_CRN, USER_CRN, List.of(resourceCrn1, resourceCrn2, resourceCrn3), "right", REQUEST_ID);

        verify(authorizationClient).hasRights(eq(REQUEST_ID.get()), eq(USER_CRN), captor.capture());

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
}
