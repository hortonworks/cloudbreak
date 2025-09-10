package com.sequenceiq.remoteenvironment.remotecluster.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalGrpc.RemoteClusterInternalBlockingStub;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.PvcControlPlaneConfiguration;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceConfig;
import com.sequenceiq.remotecluster.client.StubProvider;

@ExtendWith(MockitoExtension.class)
class RemoteClusterServiceClientTest {

    @Mock
    private ManagedChannelWrapper channelWrapper;

    @Mock
    private StubProvider stubProvider;

    @Mock
    private RemoteClusterServiceConfig remoteClusterConfig;

    @InjectMocks
    private RemoteClusterServiceClient remoteClusterServiceClient;

    @Test
    public void testListAllPrivateControlPlanes() {
        RemoteClusterInternalBlockingStub remoteClusterInternalBlockingStub = mock(RemoteClusterInternalBlockingStub.class);
        ListAllPvcControlPlanesResponse response1 = createMockResponse(true, "token1", "name1");
        ListAllPvcControlPlanesResponse response2 = createMockResponse(false, null, "name2");

        List<PvcControlPlaneConfiguration> items = new ArrayList<>();
        items.addAll(response1.getControlPlaneConfigurationsList());
        items.addAll(response2.getControlPlaneConfigurationsList());

        when(remoteClusterConfig.getGrpcTimeoutSec()).thenReturn(1L);
        when(remoteClusterConfig.getCallingServiceName()).thenReturn("caller");
        when(remoteClusterInternalBlockingStub.listAllPvcControlPlanes(any()))
                .thenReturn(response1)
                .thenReturn(response2);
        when(stubProvider.newRemoteClusterInternalStub(any(), any(), any(), any(), any())).thenReturn(remoteClusterInternalBlockingStub);

        List<PvcControlPlaneConfiguration> result = remoteClusterServiceClient.listAllPrivateControlPlanes();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    private ListAllPvcControlPlanesResponse createMockResponse(boolean hasNextPage, String nextPageToken, String name) {
        PvcControlPlaneConfiguration pc = PvcControlPlaneConfiguration.newBuilder()
                .setName(name)
                .build();

        List<PvcControlPlaneConfiguration> configurations = List.of(pc);
        return ListAllPvcControlPlanesResponse.newBuilder()
                .addAllControlPlaneConfigurations(configurations)
                .setNextPageToken(hasNextPage ? nextPageToken : "")
                .build();
    }

}