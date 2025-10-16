package com.cloudera.thunderhead.service.authdistributor;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class AuthDistributorGrpc {

  private AuthDistributorGrpc() {}

  public static final java.lang.String SERVICE_NAME = "authdistributor.AuthDistributor";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
      com.cloudera.thunderhead.service.common.version.Version.VersionResponse> getGetVersionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetVersion",
      requestType = com.cloudera.thunderhead.service.common.version.Version.VersionRequest.class,
      responseType = com.cloudera.thunderhead.service.common.version.Version.VersionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
      com.cloudera.thunderhead.service.common.version.Version.VersionResponse> getGetVersionMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse> getGetVersionMethod;
    if ((getGetVersionMethod = AuthDistributorGrpc.getGetVersionMethod) == null) {
      synchronized (AuthDistributorGrpc.class) {
        if ((getGetVersionMethod = AuthDistributorGrpc.getGetVersionMethod) == null) {
          AuthDistributorGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuthDistributorMethodDescriptorSupplier("GetVersion"))
              .build();
        }
      }
    }
    return getGetVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest,
      com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse> getFetchAuthViewForEnvironmentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FetchAuthViewForEnvironment",
      requestType = com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest.class,
      responseType = com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest,
      com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse> getFetchAuthViewForEnvironmentMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest, com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse> getFetchAuthViewForEnvironmentMethod;
    if ((getFetchAuthViewForEnvironmentMethod = AuthDistributorGrpc.getFetchAuthViewForEnvironmentMethod) == null) {
      synchronized (AuthDistributorGrpc.class) {
        if ((getFetchAuthViewForEnvironmentMethod = AuthDistributorGrpc.getFetchAuthViewForEnvironmentMethod) == null) {
          AuthDistributorGrpc.getFetchAuthViewForEnvironmentMethod = getFetchAuthViewForEnvironmentMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest, com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FetchAuthViewForEnvironment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuthDistributorMethodDescriptorSupplier("FetchAuthViewForEnvironment"))
              .build();
        }
      }
    }
    return getFetchAuthViewForEnvironmentMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest,
      com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse> getRemoveAuthViewForEnvironmentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RemoveAuthViewForEnvironment",
      requestType = com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest.class,
      responseType = com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest,
      com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse> getRemoveAuthViewForEnvironmentMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest, com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse> getRemoveAuthViewForEnvironmentMethod;
    if ((getRemoveAuthViewForEnvironmentMethod = AuthDistributorGrpc.getRemoveAuthViewForEnvironmentMethod) == null) {
      synchronized (AuthDistributorGrpc.class) {
        if ((getRemoveAuthViewForEnvironmentMethod = AuthDistributorGrpc.getRemoveAuthViewForEnvironmentMethod) == null) {
          AuthDistributorGrpc.getRemoveAuthViewForEnvironmentMethod = getRemoveAuthViewForEnvironmentMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest, com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RemoveAuthViewForEnvironment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuthDistributorMethodDescriptorSupplier("RemoveAuthViewForEnvironment"))
              .build();
        }
      }
    }
    return getRemoveAuthViewForEnvironmentMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest,
      com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse> getUpdateAuthViewForEnvironmentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateAuthViewForEnvironment",
      requestType = com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest.class,
      responseType = com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest,
      com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse> getUpdateAuthViewForEnvironmentMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest, com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse> getUpdateAuthViewForEnvironmentMethod;
    if ((getUpdateAuthViewForEnvironmentMethod = AuthDistributorGrpc.getUpdateAuthViewForEnvironmentMethod) == null) {
      synchronized (AuthDistributorGrpc.class) {
        if ((getUpdateAuthViewForEnvironmentMethod = AuthDistributorGrpc.getUpdateAuthViewForEnvironmentMethod) == null) {
          AuthDistributorGrpc.getUpdateAuthViewForEnvironmentMethod = getUpdateAuthViewForEnvironmentMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest, com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateAuthViewForEnvironment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuthDistributorMethodDescriptorSupplier("UpdateAuthViewForEnvironment"))
              .build();
        }
      }
    }
    return getUpdateAuthViewForEnvironmentMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AuthDistributorStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthDistributorStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthDistributorStub>() {
        @java.lang.Override
        public AuthDistributorStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthDistributorStub(channel, callOptions);
        }
      };
    return AuthDistributorStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static AuthDistributorBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthDistributorBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthDistributorBlockingV2Stub>() {
        @java.lang.Override
        public AuthDistributorBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthDistributorBlockingV2Stub(channel, callOptions);
        }
      };
    return AuthDistributorBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AuthDistributorBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthDistributorBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthDistributorBlockingStub>() {
        @java.lang.Override
        public AuthDistributorBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthDistributorBlockingStub(channel, callOptions);
        }
      };
    return AuthDistributorBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AuthDistributorFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthDistributorFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthDistributorFutureStub>() {
        @java.lang.Override
        public AuthDistributorFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthDistributorFutureStub(channel, callOptions);
        }
      };
    return AuthDistributorFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Get the service version.
     * </pre>
     */
    default void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetVersionMethod(), responseObserver);
    }

    /**
     */
    default void fetchAuthViewForEnvironment(com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFetchAuthViewForEnvironmentMethod(), responseObserver);
    }

    /**
     */
    default void removeAuthViewForEnvironment(com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRemoveAuthViewForEnvironmentMethod(), responseObserver);
    }

    /**
     */
    default void updateAuthViewForEnvironment(com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateAuthViewForEnvironmentMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service AuthDistributor.
   */
  public static abstract class AuthDistributorImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AuthDistributorGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service AuthDistributor.
   */
  public static final class AuthDistributorStub
      extends io.grpc.stub.AbstractAsyncStub<AuthDistributorStub> {
    private AuthDistributorStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthDistributorStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthDistributorStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get the service version.
     * </pre>
     */
    public void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetVersionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void fetchAuthViewForEnvironment(com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFetchAuthViewForEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void removeAuthViewForEnvironment(com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRemoveAuthViewForEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void updateAuthViewForEnvironment(com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateAuthViewForEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service AuthDistributor.
   */
  public static final class AuthDistributorBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<AuthDistributorBlockingV2Stub> {
    private AuthDistributorBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthDistributorBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthDistributorBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Get the service version.
     * </pre>
     */
    public com.cloudera.thunderhead.service.common.version.Version.VersionResponse getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetVersionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse fetchAuthViewForEnvironment(com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getFetchAuthViewForEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse removeAuthViewForEnvironment(com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRemoveAuthViewForEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse updateAuthViewForEnvironment(com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUpdateAuthViewForEnvironmentMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service AuthDistributor.
   */
  public static final class AuthDistributorBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AuthDistributorBlockingStub> {
    private AuthDistributorBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthDistributorBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthDistributorBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get the service version.
     * </pre>
     */
    public com.cloudera.thunderhead.service.common.version.Version.VersionResponse getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetVersionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse fetchAuthViewForEnvironment(com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFetchAuthViewForEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse removeAuthViewForEnvironment(com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRemoveAuthViewForEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse updateAuthViewForEnvironment(com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateAuthViewForEnvironmentMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service AuthDistributor.
   */
  public static final class AuthDistributorFutureStub
      extends io.grpc.stub.AbstractFutureStub<AuthDistributorFutureStub> {
    private AuthDistributorFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthDistributorFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthDistributorFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get the service version.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> getVersion(
        com.cloudera.thunderhead.service.common.version.Version.VersionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetVersionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse> fetchAuthViewForEnvironment(
        com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFetchAuthViewForEnvironmentMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse> removeAuthViewForEnvironment(
        com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRemoveAuthViewForEnvironmentMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse> updateAuthViewForEnvironment(
        com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateAuthViewForEnvironmentMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_FETCH_AUTH_VIEW_FOR_ENVIRONMENT = 1;
  private static final int METHODID_REMOVE_AUTH_VIEW_FOR_ENVIRONMENT = 2;
  private static final int METHODID_UPDATE_AUTH_VIEW_FOR_ENVIRONMENT = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_VERSION:
          serviceImpl.getVersion((com.cloudera.thunderhead.service.common.version.Version.VersionRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse>) responseObserver);
          break;
        case METHODID_FETCH_AUTH_VIEW_FOR_ENVIRONMENT:
          serviceImpl.fetchAuthViewForEnvironment((com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse>) responseObserver);
          break;
        case METHODID_REMOVE_AUTH_VIEW_FOR_ENVIRONMENT:
          serviceImpl.removeAuthViewForEnvironment((com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse>) responseObserver);
          break;
        case METHODID_UPDATE_AUTH_VIEW_FOR_ENVIRONMENT:
          serviceImpl.updateAuthViewForEnvironment((com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGetVersionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
              com.cloudera.thunderhead.service.common.version.Version.VersionResponse>(
                service, METHODID_GET_VERSION)))
        .addMethod(
          getFetchAuthViewForEnvironmentMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest,
              com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse>(
                service, METHODID_FETCH_AUTH_VIEW_FOR_ENVIRONMENT)))
        .addMethod(
          getRemoveAuthViewForEnvironmentMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest,
              com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse>(
                service, METHODID_REMOVE_AUTH_VIEW_FOR_ENVIRONMENT)))
        .addMethod(
          getUpdateAuthViewForEnvironmentMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest,
              com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse>(
                service, METHODID_UPDATE_AUTH_VIEW_FOR_ENVIRONMENT)))
        .build();
  }

  private static abstract class AuthDistributorBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AuthDistributorBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AuthDistributor");
    }
  }

  private static final class AuthDistributorFileDescriptorSupplier
      extends AuthDistributorBaseDescriptorSupplier {
    AuthDistributorFileDescriptorSupplier() {}
  }

  private static final class AuthDistributorMethodDescriptorSupplier
      extends AuthDistributorBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    AuthDistributorMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (AuthDistributorGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AuthDistributorFileDescriptorSupplier())
              .addMethod(getGetVersionMethod())
              .addMethod(getFetchAuthViewForEnvironmentMethod())
              .addMethod(getRemoveAuthViewForEnvironmentMethod())
              .addMethod(getUpdateAuthViewForEnvironmentMethod())
              .build();
        }
      }
    }
    return result;
  }
}
