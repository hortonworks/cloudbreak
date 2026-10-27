package com.sequenceiq.remoteenvironment.remotecluster.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.remotecluster.client.RemoteClusterGrpcErrors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

class RemoteClusterGrpcErrorsTest {

    private static final Metadata.Key<String> STATUS_CODE_EXTENSION_KEY =
            Metadata.Key.of("grpc-status-code-extension", Metadata.ASCII_STRING_MARSHALLER);

    @Test
    void unavailableWithCmProxyExtensionIsCmProxyCallFailure() {
        assertThat(RemoteClusterGrpcErrors.isCmProxyCallFailure(exception(Status.UNAVAILABLE, "25"))).isTrue();
    }

    @Test
    void unavailableWithoutExtensionIsNotCmProxyCallFailure() {
        assertThat(RemoteClusterGrpcErrors.isCmProxyCallFailure(Status.UNAVAILABLE.asRuntimeException())).isFalse();
    }

    @Test
    void unavailableWithOtherExtensionIsNotCmProxyCallFailure() {
        assertThat(RemoteClusterGrpcErrors.isCmProxyCallFailure(exception(Status.UNAVAILABLE, "99"))).isFalse();
    }

    @Test
    void internalWithCmProxyExtensionIsNotCmProxyCallFailure() {
        assertThat(RemoteClusterGrpcErrors.isCmProxyCallFailure(exception(Status.INTERNAL, "25"))).isFalse();
    }

    private StatusRuntimeException exception(Status status, String extension) {
        Metadata trailers = new Metadata();
        trailers.put(STATUS_CODE_EXTENSION_KEY, extension);
        return status.withDescription("Call to cluster proxy failed").asRuntimeException(trailers);
    }
}
