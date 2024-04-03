package com.sequenceiq.remotecluster.client;

import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalGrpc;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.altus.CallingServiceNameInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;

@Service
public class StubProvider {

    public RemoteClusterInternalGrpc.RemoteClusterInternalBlockingStub newInternalStub(
            ManagedChannel channel,
            String requestId,
            Long timeout,
            String internalCrn,
            String callingServiceName) {
        checkNotNull(requestId, "requestId should not be null.");

        return RemoteClusterInternalGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTimeoutInterceptor(timeout),
                        new AltusMetadataInterceptor(requestId, internalCrn),
                        new CallingServiceNameInterceptor(callingServiceName));
    }
}
