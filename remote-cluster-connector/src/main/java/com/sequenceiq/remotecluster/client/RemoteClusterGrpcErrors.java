package com.sequenceiq.remotecluster.client;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public final class RemoteClusterGrpcErrors {

    private static final Metadata.Key<String> STATUS_CODE_EXTENSION_KEY =
            Metadata.Key.of("grpc-status-code-extension", Metadata.ASCII_STRING_MARSHALLER);

    // must match CodeExtension.CM_PROXY_CALL_FAILED in thunderhead
    private static final String CM_PROXY_CALL_FAILED_CODE_EXTENSION = "25";

    private RemoteClusterGrpcErrors() {
    }

    public static boolean isCmProxyCallFailure(StatusRuntimeException e) {
        Metadata trailers = e.getTrailers();
        return Status.Code.UNAVAILABLE == e.getStatus().getCode()
                && trailers != null
                && CM_PROXY_CALL_FAILED_CODE_EXTENSION.equals(trailers.get(STATUS_CODE_EXTENSION_KEY));
    }
}
