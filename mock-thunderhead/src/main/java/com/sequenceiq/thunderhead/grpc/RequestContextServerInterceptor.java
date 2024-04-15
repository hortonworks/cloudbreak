package com.sequenceiq.thunderhead.grpc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.logger.MdcContext;

import io.grpc.Context;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class RequestContextServerInterceptor implements ServerInterceptor {

    private static final Key<String> REQUEST_ID_METADATA_KEY = Key.of("requestId", Metadata.ASCII_STRING_MARSHALLER);

    private static final Key<String> ACTOR_CRN_METADATA_KEY = Key.of("actorCrn", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <R, S> Listener<R> interceptCall(
            ServerCall<R, S> serverCall,
            Metadata metadata,
            ServerCallHandler<R, S> serverCallHandler) {

        String fullMethodName = serverCall.getMethodDescriptor().getFullMethodName();
        if (!"usermanagement.UserManagement/VerifyInteractiveUserSessionToken".equals(fullMethodName)
                && !"usermanagement.UserManagement/Authenticate".equals(fullMethodName)) {
            String requestId = metadata.get(REQUEST_ID_METADATA_KEY);
            // Temporary solution. Liftie does not send request ID for this UMS call, tracking jira: COMPX-16621
            if ("usermanagement.UserManagement/GetWorkloadAuthConfiguration".equals(fullMethodName)) {
                requestId = UUID.randomUUID().toString();
            }
            checkNotNull(requestId, "requestId should not be null.");
            String actorCrn = metadata.get(ACTOR_CRN_METADATA_KEY);
            if ("usermanagement.UserManagement/GetWorkloadAuthConfiguration".equals(fullMethodName)) {
                actorCrn = "unknown actor";
            }
            checkNotNull(actorCrn, "actorCrn should not be null.");

            GrpcRequestContext requestContext = new GrpcRequestContext(requestId);
            GrpcActorContext actorContext = new GrpcActorContext(actorCrn);
            Context context = Context.current().withValues(
                    GrpcRequestContext.REQUEST_CONTEXT,
                    requestContext,
                    GrpcActorContext.ACTOR_CONTEXT,
                    actorContext);
            Context previous = context.attach();
            try {
                return new ContextualizedServerCallListener<>(serverCallHandler.startCall(serverCall, metadata), context);
            } finally {
                context.detach(previous);
            }
        } else {
            String requestId = metadata.get(REQUEST_ID_METADATA_KEY);
            checkNotNull(requestId, "requestId should not be null.");
            GrpcRequestContext requestContext =
                    new GrpcRequestContext(requestId);
            Context context = Context.current().withValue(GrpcRequestContext.REQUEST_CONTEXT, requestContext);
            return new ContextualizedServerCallListener<>(
                    serverCallHandler.startCall(serverCall, metadata),
                    context);
        }
    }

    private static class ContextualizedServerCallListener<R> extends SimpleForwardingServerCallListener<R> {

        private final Context context;

        ContextualizedServerCallListener(
                Listener<R> delegate,
                Context context) {
            super(delegate);
            this.context = checkNotNull(context);
        }

        @Override
        protected Listener<R> delegate() {
            GrpcRequestContext grpcRequestContext = GrpcRequestContext.REQUEST_CONTEXT.get(context);
            if (grpcRequestContext != null) {
                MdcContext.builder().requestId(grpcRequestContext.getRequestId()).buildMdc();
            }
            return super.delegate();
        }

        @Override
        public void onMessage(R message) {
            Context previous = context.attach();
            try {
                super.onMessage(message);
            } finally {
                context.detach(previous);
                MDC.clear();
            }
        }

        @Override
        public void onHalfClose() {
            Context previous = context.attach();
            try {
                super.onHalfClose();
            } finally {
                context.detach(previous);
                MDC.clear();
            }
        }

        @Override
        public void onCancel() {
            Context previous = context.attach();
            try {
                super.onCancel();
            } finally {
                context.detach(previous);
                MDC.clear();
            }
        }
    }
}
