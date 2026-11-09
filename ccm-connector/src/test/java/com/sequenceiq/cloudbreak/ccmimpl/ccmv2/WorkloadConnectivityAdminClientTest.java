package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiGrpc;
import com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest;
import com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse;
import com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityProto.TunnelStatus;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.config.GrpcWorkloadConnectivityAdminConfig;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;

@ExtendWith(MockitoExtension.class)
class WorkloadConnectivityAdminClientTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:envId";

    private static final String INTERNAL_CRN = "crn:altus:iam:us-west-1:altus:user:__internal__actor__";

    private static final Metadata.Key<String> ACTOR_CRN_METADATA_KEY = Metadata.Key.of("actorCrn", Metadata.ASCII_STRING_MARSHALLER);

    private final AtomicReference<ListTunnelStatusRequest> receivedRequest = new AtomicReference<>();

    private final AtomicReference<String> receivedActorCrn = new AtomicReference<>();

    private final AtomicReference<Status> responseStatus = new AtomicReference<>(Status.OK);

    private Server server;

    private ManagedChannel channel;

    @Mock
    private ManagedChannelWrapper channelWrapper;

    @Mock
    private GrpcWorkloadConnectivityAdminConfig grpcWorkloadConnectivityAdminConfig;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private WorkloadConnectivityAdminClient underTest;

    @BeforeEach
    void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(new FakeWorkloadConnectivityAdminApi(), new ActorCrnCapturingInterceptor()))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

        when(channelWrapper.getChannel()).thenReturn(channel);
        when(grpcWorkloadConnectivityAdminConfig.getGrpcTimeoutSec()).thenReturn(10L);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_CRN);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testListTunnelStatus() {
        ListTunnelStatusResponse response = underTest.listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN);

        assertEquals(1, response.getTunnelsCount());
        assertEquals("tunnelId", response.getTunnels(0).getId());
        assertEquals(ACCOUNT_ID, receivedRequest.get().getAccountId());
        assertEquals(ENVIRONMENT_CRN, receivedRequest.get().getEnvironmentCrn());
        assertEquals(INTERNAL_CRN, receivedActorCrn.get());
    }

    @Test
    void testListTunnelStatusPropagatesGrpcError() {
        responseStatus.set(Status.UNIMPLEMENTED);

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> underTest.listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN));

        assertEquals(Status.Code.UNIMPLEMENTED, exception.getStatus().getCode());
    }

    private class FakeWorkloadConnectivityAdminApi extends WorkloadConnectivityAdminApiGrpc.WorkloadConnectivityAdminApiImplBase {

        @Override
        public void listTunnelStatus(ListTunnelStatusRequest request, StreamObserver<ListTunnelStatusResponse> responseObserver) {
            receivedRequest.set(request);
            if (responseStatus.get().isOk()) {
                responseObserver.onNext(ListTunnelStatusResponse.newBuilder()
                        .addTunnels(TunnelStatus.newBuilder().setId("tunnelId").build())
                        .build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(responseStatus.get().asRuntimeException());
            }
        }
    }

    private class ActorCrnCapturingInterceptor implements ServerInterceptor {

        @Override
        public <R, S> ServerCall.Listener<R> interceptCall(ServerCall<R, S> call, Metadata headers, ServerCallHandler<R, S> next) {
            receivedActorCrn.set(headers.get(ACTOR_CRN_METADATA_KEY));
            return next.startCall(call, headers);
        }
    }
}
