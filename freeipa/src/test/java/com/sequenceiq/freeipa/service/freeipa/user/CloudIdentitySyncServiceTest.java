package com.sequenceiq.freeipa.service.freeipa.user;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AzureCloudIdentityName;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CloudIdentity;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CloudIdentityName;
import com.google.common.collect.HashMultimap;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SetRangerCloudIdentityMappingRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
class CloudIdentitySyncServiceTest {

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private Stack stack;

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

    @Test
    void testSyncAzureIdentites() {
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.toString());
        when(stack.getEnvironmentCrn()).thenReturn("envcrn");
        UsersState usersState = new UsersState(
                Set.of(new FmsGroup().withName("group1")),
                Set.of(new FmsUser().withName("user1"), new FmsUser().withName("user2")),
                HashMultimap.create()
        );
        UmsUsersState umsUsersState = new UmsUsersState.Builder()
                .addUserCloudIdentities("user1", List.of(newAzureObjectId("object-id-1")))
                .addUserCloudIdentities("user2", List.of(newAzureObjectId("object-id-2")))
                .addUserCloudIdentities("user3", List.of(newAzureObjectId("object-id-3")))
                .addGroupCloudIdentities("group1", List.of(newAzureObjectId("object-id-4")))
                .setUsersState(usersState)
                .build();

        cloudIdentitySyncService.syncCloudIdentites(stack, umsUsersState, mock(BiConsumer.class));

        SetRangerCloudIdentityMappingRequest expectedRequest = new SetRangerCloudIdentityMappingRequest();
        expectedRequest.setAzureUserMapping(Map.of("user1", "object-id-1", "user2", "object-id-2"));
        expectedRequest.setAzureGroupMapping(Map.of("group1", "object-id-4"));
        verify(sdxEndpoint, times(1)).setRangerCloudIdentityMapping(eq("envcrn"), eq(expectedRequest));
    }

    @Test
    void testSyncAwsIdentites() {
        // aws cloud identites is unsupported, should no-op
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.toString());
        cloudIdentitySyncService.syncCloudIdentites(stack, mock(UmsUsersState.class), mock(BiConsumer.class));
        verify(sdxEndpoint, never()).setRangerCloudIdentityMapping(any(), any());
    }

}