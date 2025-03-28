package com.sequenceiq.freeipa.service.freeipa.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AzureCloudIdentityName;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CloudIdentity;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CloudIdentityName;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ServicePrincipalCloudIdentities;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.polling.usersync.CloudIdSyncPollerObject;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncState;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;
import com.sequenceiq.sdx.api.model.SetRangerCloudIdentityMappingRequest;

@ExtendWith(MockitoExtension.class)
class CloudIdentitySyncServiceTest {

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private StackUserSyncView stack;

    @Mock
    private PollingService<CloudIdSyncPollerObject> cloudIdSyncPollingService;

    @InjectMocks
    private CloudIdentitySyncService cloudIdentitySyncService;

    private CloudIdentity newAzureObjectId(String azureObjectId) {
        AzureCloudIdentityName azureCloudIdentityName = AzureCloudIdentityName.newBuilder()
                .setObjectId(azureObjectId)
                .build();
        return CloudIdentity.newBuilder()
                .setCloudIdentityName(CloudIdentityName.newBuilder()
                        .setAzureCloudIdentityName(azureCloudIdentityName)
                        .build())
                .build();
    }

    private ServicePrincipalCloudIdentities newServicePrincipalAzureObjectId(String servicePrincipal, String azureObjectId) {
        return ServicePrincipalCloudIdentities.newBuilder()
                .setServicePrincipal(servicePrincipal)
                .addCloudIdentities(newAzureObjectId(azureObjectId))
                .build();
    }

    private void testSyncAzureIdentitiesWithStatus(RangerCloudIdentitySyncStatus status) {
        when(stack.cloudPlatform()).thenReturn(CloudPlatform.AZURE.toString());
        when(stack.environmentCrn()).thenReturn("envcrn");
        when(sdxEndpoint.setRangerCloudIdentityMapping(eq("envcrn"), any())).thenReturn(status);
        UmsUsersState.Builder umsUsersStateBuilder = UmsUsersState.newBuilder()
                .addUserCloudIdentities("user1", List.of(newAzureObjectId("user-oid-1")))
                .addUserCloudIdentities("user2", List.of(newAzureObjectId("user-oid-2")))
                .setUsersState(UsersState.newBuilder().build());

        List<ServicePrincipalCloudIdentities> spCloudIds = new ArrayList<>();
        spCloudIds.add(newServicePrincipalAzureObjectId("sp01", "sp-oid-1"));
        spCloudIds.add(newServicePrincipalAzureObjectId("sp02", "sp-oid-2"));
        umsUsersStateBuilder.addServicePrincipalCloudIdentities(spCloudIds);

        cloudIdentitySyncService.syncCloudIdentities(stack, umsUsersStateBuilder.build(), mock(BiConsumer.class));

        SetRangerCloudIdentityMappingRequest expectedRequest = new SetRangerCloudIdentityMappingRequest();
        expectedRequest.setAzureUserMapping(Map.of("user1", "user-oid-1", "user2", "user-oid-2", "sp01", "sp-oid-1", "sp02", "sp-oid-2"));
        verify(sdxEndpoint, times(1)).setRangerCloudIdentityMapping(eq("envcrn"), eq(expectedRequest));
    }

    @Test
    void testSyncAzureIdentitiesSuccess() {
        RangerCloudIdentitySyncStatus status = new RangerCloudIdentitySyncStatus();
        status.setState(RangerCloudIdentitySyncState.SUCCESS);
        testSyncAzureIdentitiesWithStatus(status);
        verify(cloudIdSyncPollingService, never()).pollWithAbsoluteTimeout(any(), any(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void testSyncAzureIdentitiesActive() {
        RangerCloudIdentitySyncStatus status = new RangerCloudIdentitySyncStatus();
        status.setState(RangerCloudIdentitySyncState.ACTIVE);
        status.setCommandId(1L);
        CloudIdSyncPollerObject expectedPollerObject = new CloudIdSyncPollerObject("envcrn", List.of(1L));
        testSyncAzureIdentitiesWithStatus(status);
        verify(cloudIdSyncPollingService, never()).pollWithAbsoluteTimeout(any(), eq(expectedPollerObject), anyLong(), anyLong(), anyInt());
    }

    @Test
    void testSyncAzureIdentitiesFailed() {
        RangerCloudIdentitySyncStatus status = new RangerCloudIdentitySyncStatus();
        status.setState(RangerCloudIdentitySyncState.FAILED);
        testSyncAzureIdentitiesWithStatus(status);
        verify(cloudIdSyncPollingService, never()).pollWithAbsoluteTimeout(any(), any(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void testSyncAzureIdentitiesNotApplicable() {
        RangerCloudIdentitySyncStatus status = new RangerCloudIdentitySyncStatus();
        status.setState(RangerCloudIdentitySyncState.NOT_APPLICABLE);
        testSyncAzureIdentitiesWithStatus(status);
        verify(cloudIdSyncPollingService, never()).pollWithAbsoluteTimeout(any(), any(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void testSyncAwsIdentities() {
        // aws cloud identites is unsupported, should no-op
        when(stack.cloudPlatform()).thenReturn(CloudPlatform.AWS.toString());
        cloudIdentitySyncService.syncCloudIdentities(stack, mock(UmsUsersState.class), mock(BiConsumer.class));
        verify(sdxEndpoint, never()).setRangerCloudIdentityMapping(any(), any());
    }

}