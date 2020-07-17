package com.sequenceiq.cloudbreak.auth.altus;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsConfig;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;

@RunWith(MockitoJUnitRunner.class)
public class GrpcUmsClientTest {

    private static final String ACCOUNT_ID = "altus";

    private static final String TEST_ROLE_1 = "TestRole1";

    private static final String TEST_ROLE_2 = "TestRole2";

    @Mock
    private UmsConfig umsConfig;

    @Mock
    private UmsClientConfig umsClientConfig;

    @Mock
    private UmsClient umsClient;

    @InjectMocks
    private GrpcUmsClient underTest;

    private GrpcUmsClient underTestWithMockUmsClient;

    @Before
    public void setUp() throws Exception {
        underTestWithMockUmsClient = spy(underTest);
        ManagedChannel managedChannel = mock(ManagedChannel.class);
        ManagedChannelWrapper managedChannelWrapper = mock(ManagedChannelWrapper.class);
        doReturn(managedChannel).when(managedChannelWrapper).getChannel();
        doReturn(managedChannelWrapper).when(underTestWithMockUmsClient).makeWrapper();
        doReturn(umsClient).when(underTestWithMockUmsClient).makeClient(any(ManagedChannel.class), anyString());
    }

    @Test
    public void testGetRoleCrn() {
        Crn testRole1 = underTest.getRoleCrn(TEST_ROLE_1);
        Crn testRole2 = underTest.getRoleCrn(TEST_ROLE_2);

        assertEquals(ACCOUNT_ID, testRole1.getAccountId());
        assertEquals(TEST_ROLE_1, testRole1.getResource());
        assertEquals(Crn.ResourceType.ROLE, testRole1.getResourceType());

        assertEquals(ACCOUNT_ID, testRole2.getAccountId());
        assertEquals(TEST_ROLE_2, testRole2.getResource());
        assertEquals(Crn.ResourceType.ROLE, testRole2.getResourceType());
    }

    @Test
    public void testGetResourceRoleCrn() {
        Crn testRole1 = underTest.getResourceRoleCrn(TEST_ROLE_1);
        Crn testRole2 = underTest.getResourceRoleCrn(TEST_ROLE_2);

        assertEquals(ACCOUNT_ID, testRole1.getAccountId());
        assertEquals(TEST_ROLE_1, testRole1.getResource());
        assertEquals(Crn.ResourceType.RESOURCE_ROLE, testRole1.getResourceType());

        assertEquals(ACCOUNT_ID, testRole2.getAccountId());
        assertEquals(TEST_ROLE_2, testRole2.getResource());
        assertEquals(Crn.ResourceType.RESOURCE_ROLE, testRole2.getResourceType());
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
}
