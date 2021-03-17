package com.sequenceiq.cloudbreak.auth.altus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.thunderhead.service.common.paging.PagingProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ServicePrincipalCloudIdentities;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;

@RunWith(MockitoJUnitRunner.class)
public class GrpcUmsClientTest {

    private static final String USER_CRN = Crn.builder(CrnResourceDescriptor.USER)
            .setResource("user")
            .setAccountId("acc")
            .build().toString();

    @Mock
    private ManagedChannelWrapper channelWrapper;

    @Mock
    private UmsClientConfig umsClientConfig;

    @Mock
    private Tracer tracer;

    @InjectMocks
    private GrpcUmsClient underTest;

    private GrpcUmsClient underTestWithMockUmsClient;

    @Mock
    private UmsClient umsClient;

    @Mock
    private ManagedChannel managedChannel;

    @Before
    public void setUp() {
        underTestWithMockUmsClient = spy(underTest);
        doReturn(managedChannel).when(channelWrapper).getChannel();
        doReturn(umsClient).when(underTestWithMockUmsClient).makeClient(any(ManagedChannel.class), anyString());
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

        List<String> wags = underTestWithMockUmsClient.listWorkloadAdministrationGroupsForMember("actor-crn", memberCrn, Optional.empty());

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

        List<ServicePrincipalCloudIdentities> spCloudIds = underTestWithMockUmsClient.listServicePrincipalCloudIdentities("actor-crn", accountId, envCrn,
                Optional.empty());

        verify(umsClient, times(2)).listServicePrincipalCloudIdentities(anyString(), eq(accountId), eq(envCrn), any(Optional.class));
        assertTrue(spCloudIds.containsAll(spIdsList01));
        assertTrue(spCloudIds.containsAll(spIdsList02));
    }

    @Test
    public void testCheckRightWithInvalidCrn() {
        assertEquals(
                assertThrows(IllegalArgumentException.class, () -> underTest.checkResourceRight(USER_CRN, USER_CRN,
                "environments/describeEnvironment", "invalidCrn", Optional.empty())).getMessage(),
                "Provided resource [invalidCrn] is not in CRN format");
    }

    @Test
    public void testHasRightsWithInvalidCrn() {
        assertEquals(
                assertThrows(IllegalArgumentException.class, () -> underTest.hasRights(USER_CRN, USER_CRN,
                List.of("invalidCrn", "*"), "environments/describeEnvironment", Optional.empty())).getMessage(),
                "Following resources are not provided in CRN format: invalidCrn.");
        assertEquals(
                assertThrows(IllegalArgumentException.class, () -> underTest.hasRightsOnResources(USER_CRN, USER_CRN,
                List.of("invalidCrn", "*"), "environments/describeEnvironment", Optional.empty())).getMessage(),
                "Following resources are not provided in CRN format: invalidCrn.");
    }
}
