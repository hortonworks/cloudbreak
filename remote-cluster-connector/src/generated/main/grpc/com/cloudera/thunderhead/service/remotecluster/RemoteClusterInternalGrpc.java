package com.cloudera.thunderhead.service.remotecluster;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * RemoteClusterInternal Service :: RemoteClusterInternal Service lists RemoteCluster endpoints for internal use only.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class RemoteClusterInternalGrpc {

  private RemoteClusterInternalGrpc() {}

  public static final java.lang.String SERVICE_NAME = "remotecluster.RemoteClusterInternal";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse> getListAllPvcControlPlanesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListAllPvcControlPlanes",
      requestType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest.class,
      responseType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse> getListAllPvcControlPlanesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse> getListAllPvcControlPlanesMethod;
    if ((getListAllPvcControlPlanesMethod = RemoteClusterInternalGrpc.getListAllPvcControlPlanesMethod) == null) {
      synchronized (RemoteClusterInternalGrpc.class) {
        if ((getListAllPvcControlPlanesMethod = RemoteClusterInternalGrpc.getListAllPvcControlPlanesMethod) == null) {
          RemoteClusterInternalGrpc.getListAllPvcControlPlanesMethod = getListAllPvcControlPlanesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListAllPvcControlPlanes"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RemoteClusterInternalMethodDescriptorSupplier("ListAllPvcControlPlanes"))
              .build();
        }
      }
    }
    return getListAllPvcControlPlanesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse> getGeneratePvcControlPlaneAuthTokenMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GeneratePvcControlPlaneAuthToken",
      requestType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest.class,
      responseType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse> getGeneratePvcControlPlaneAuthTokenMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse> getGeneratePvcControlPlaneAuthTokenMethod;
    if ((getGeneratePvcControlPlaneAuthTokenMethod = RemoteClusterInternalGrpc.getGeneratePvcControlPlaneAuthTokenMethod) == null) {
      synchronized (RemoteClusterInternalGrpc.class) {
        if ((getGeneratePvcControlPlaneAuthTokenMethod = RemoteClusterInternalGrpc.getGeneratePvcControlPlaneAuthTokenMethod) == null) {
          RemoteClusterInternalGrpc.getGeneratePvcControlPlaneAuthTokenMethod = getGeneratePvcControlPlaneAuthTokenMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GeneratePvcControlPlaneAuthToken"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RemoteClusterInternalMethodDescriptorSupplier("GeneratePvcControlPlaneAuthToken"))
              .build();
        }
      }
    }
    return getGeneratePvcControlPlaneAuthTokenMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse> getGeneratePvcWorkloadAuthTokenMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GeneratePvcWorkloadAuthToken",
      requestType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest.class,
      responseType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse> getGeneratePvcWorkloadAuthTokenMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse> getGeneratePvcWorkloadAuthTokenMethod;
    if ((getGeneratePvcWorkloadAuthTokenMethod = RemoteClusterInternalGrpc.getGeneratePvcWorkloadAuthTokenMethod) == null) {
      synchronized (RemoteClusterInternalGrpc.class) {
        if ((getGeneratePvcWorkloadAuthTokenMethod = RemoteClusterInternalGrpc.getGeneratePvcWorkloadAuthTokenMethod) == null) {
          RemoteClusterInternalGrpc.getGeneratePvcWorkloadAuthTokenMethod = getGeneratePvcWorkloadAuthTokenMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GeneratePvcWorkloadAuthToken"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RemoteClusterInternalMethodDescriptorSupplier("GeneratePvcWorkloadAuthToken"))
              .build();
        }
      }
    }
    return getGeneratePvcWorkloadAuthTokenMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse> getRegisterPvcBaseClusterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterPvcBaseCluster",
      requestType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest.class,
      responseType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse> getRegisterPvcBaseClusterMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse> getRegisterPvcBaseClusterMethod;
    if ((getRegisterPvcBaseClusterMethod = RemoteClusterInternalGrpc.getRegisterPvcBaseClusterMethod) == null) {
      synchronized (RemoteClusterInternalGrpc.class) {
        if ((getRegisterPvcBaseClusterMethod = RemoteClusterInternalGrpc.getRegisterPvcBaseClusterMethod) == null) {
          RemoteClusterInternalGrpc.getRegisterPvcBaseClusterMethod = getRegisterPvcBaseClusterMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterPvcBaseCluster"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RemoteClusterInternalMethodDescriptorSupplier("RegisterPvcBaseCluster"))
              .build();
        }
      }
    }
    return getRegisterPvcBaseClusterMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RemoteClusterInternalStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RemoteClusterInternalStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RemoteClusterInternalStub>() {
        @java.lang.Override
        public RemoteClusterInternalStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RemoteClusterInternalStub(channel, callOptions);
        }
      };
    return RemoteClusterInternalStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static RemoteClusterInternalBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RemoteClusterInternalBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RemoteClusterInternalBlockingV2Stub>() {
        @java.lang.Override
        public RemoteClusterInternalBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RemoteClusterInternalBlockingV2Stub(channel, callOptions);
        }
      };
    return RemoteClusterInternalBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RemoteClusterInternalBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RemoteClusterInternalBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RemoteClusterInternalBlockingStub>() {
        @java.lang.Override
        public RemoteClusterInternalBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RemoteClusterInternalBlockingStub(channel, callOptions);
        }
      };
    return RemoteClusterInternalBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RemoteClusterInternalFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RemoteClusterInternalFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RemoteClusterInternalFutureStub>() {
        @java.lang.Override
        public RemoteClusterInternalFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RemoteClusterInternalFutureStub(channel, callOptions);
        }
      };
    return RemoteClusterInternalFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * RemoteClusterInternal Service :: RemoteClusterInternal Service lists RemoteCluster endpoints for internal use only.
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * ListAllPvcControlPlanes method :: Lists all registered Private Cloud control plane configurations across all accounts.
     * </pre>
     */
    default void listAllPvcControlPlanes(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListAllPvcControlPlanesMethod(), responseObserver);
    }

    /**
     * <pre>
     * GeneratePvcControlPlaneAuthToken method :: Generates an auth token for the requests sent to Private Cloud control plane.
     * </pre>
     */
    default void generatePvcControlPlaneAuthToken(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGeneratePvcControlPlaneAuthTokenMethod(), responseObserver);
    }

    /**
     * <pre>
     * GeneratePvcWorkloadAuthToken method :: Generates a workload auth token for the requests sent to Private Cloud workload.
     * </pre>
     */
    default void generatePvcWorkloadAuthToken(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGeneratePvcWorkloadAuthTokenMethod(), responseObserver);
    }

    /**
     * <pre>
     * RegisterPvcBaseCluster method :: Partially registers a base cluster from an already registered PvC Control Plane.
     * </pre>
     */
    default void registerPvcBaseCluster(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterPvcBaseClusterMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service RemoteClusterInternal.
   * <pre>
   * RemoteClusterInternal Service :: RemoteClusterInternal Service lists RemoteCluster endpoints for internal use only.
   * </pre>
   */
  public static abstract class RemoteClusterInternalImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return RemoteClusterInternalGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service RemoteClusterInternal.
   * <pre>
   * RemoteClusterInternal Service :: RemoteClusterInternal Service lists RemoteCluster endpoints for internal use only.
   * </pre>
   */
  public static final class RemoteClusterInternalStub
      extends io.grpc.stub.AbstractAsyncStub<RemoteClusterInternalStub> {
    private RemoteClusterInternalStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RemoteClusterInternalStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RemoteClusterInternalStub(channel, callOptions);
    }

    /**
     * <pre>
     * ListAllPvcControlPlanes method :: Lists all registered Private Cloud control plane configurations across all accounts.
     * </pre>
     */
    public void listAllPvcControlPlanes(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListAllPvcControlPlanesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GeneratePvcControlPlaneAuthToken method :: Generates an auth token for the requests sent to Private Cloud control plane.
     * </pre>
     */
    public void generatePvcControlPlaneAuthToken(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGeneratePvcControlPlaneAuthTokenMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GeneratePvcWorkloadAuthToken method :: Generates a workload auth token for the requests sent to Private Cloud workload.
     * </pre>
     */
    public void generatePvcWorkloadAuthToken(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGeneratePvcWorkloadAuthTokenMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * RegisterPvcBaseCluster method :: Partially registers a base cluster from an already registered PvC Control Plane.
     * </pre>
     */
    public void registerPvcBaseCluster(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterPvcBaseClusterMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service RemoteClusterInternal.
   * <pre>
   * RemoteClusterInternal Service :: RemoteClusterInternal Service lists RemoteCluster endpoints for internal use only.
   * </pre>
   */
  public static final class RemoteClusterInternalBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<RemoteClusterInternalBlockingV2Stub> {
    private RemoteClusterInternalBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RemoteClusterInternalBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RemoteClusterInternalBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * ListAllPvcControlPlanes method :: Lists all registered Private Cloud control plane configurations across all accounts.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse listAllPvcControlPlanes(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListAllPvcControlPlanesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GeneratePvcControlPlaneAuthToken method :: Generates an auth token for the requests sent to Private Cloud control plane.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse generatePvcControlPlaneAuthToken(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGeneratePvcControlPlaneAuthTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GeneratePvcWorkloadAuthToken method :: Generates a workload auth token for the requests sent to Private Cloud workload.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse generatePvcWorkloadAuthToken(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGeneratePvcWorkloadAuthTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RegisterPvcBaseCluster method :: Partially registers a base cluster from an already registered PvC Control Plane.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse registerPvcBaseCluster(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRegisterPvcBaseClusterMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service RemoteClusterInternal.
   * <pre>
   * RemoteClusterInternal Service :: RemoteClusterInternal Service lists RemoteCluster endpoints for internal use only.
   * </pre>
   */
  public static final class RemoteClusterInternalBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<RemoteClusterInternalBlockingStub> {
    private RemoteClusterInternalBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RemoteClusterInternalBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RemoteClusterInternalBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * ListAllPvcControlPlanes method :: Lists all registered Private Cloud control plane configurations across all accounts.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse listAllPvcControlPlanes(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListAllPvcControlPlanesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GeneratePvcControlPlaneAuthToken method :: Generates an auth token for the requests sent to Private Cloud control plane.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse generatePvcControlPlaneAuthToken(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGeneratePvcControlPlaneAuthTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GeneratePvcWorkloadAuthToken method :: Generates a workload auth token for the requests sent to Private Cloud workload.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse generatePvcWorkloadAuthToken(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGeneratePvcWorkloadAuthTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RegisterPvcBaseCluster method :: Partially registers a base cluster from an already registered PvC Control Plane.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse registerPvcBaseCluster(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterPvcBaseClusterMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service RemoteClusterInternal.
   * <pre>
   * RemoteClusterInternal Service :: RemoteClusterInternal Service lists RemoteCluster endpoints for internal use only.
   * </pre>
   */
  public static final class RemoteClusterInternalFutureStub
      extends io.grpc.stub.AbstractFutureStub<RemoteClusterInternalFutureStub> {
    private RemoteClusterInternalFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RemoteClusterInternalFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RemoteClusterInternalFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * ListAllPvcControlPlanes method :: Lists all registered Private Cloud control plane configurations across all accounts.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse> listAllPvcControlPlanes(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListAllPvcControlPlanesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GeneratePvcControlPlaneAuthToken method :: Generates an auth token for the requests sent to Private Cloud control plane.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse> generatePvcControlPlaneAuthToken(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGeneratePvcControlPlaneAuthTokenMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GeneratePvcWorkloadAuthToken method :: Generates a workload auth token for the requests sent to Private Cloud workload.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse> generatePvcWorkloadAuthToken(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGeneratePvcWorkloadAuthTokenMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * RegisterPvcBaseCluster method :: Partially registers a base cluster from an already registered PvC Control Plane.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse> registerPvcBaseCluster(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterPvcBaseClusterMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_ALL_PVC_CONTROL_PLANES = 0;
  private static final int METHODID_GENERATE_PVC_CONTROL_PLANE_AUTH_TOKEN = 1;
  private static final int METHODID_GENERATE_PVC_WORKLOAD_AUTH_TOKEN = 2;
  private static final int METHODID_REGISTER_PVC_BASE_CLUSTER = 3;

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
        case METHODID_LIST_ALL_PVC_CONTROL_PLANES:
          serviceImpl.listAllPvcControlPlanes((com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse>) responseObserver);
          break;
        case METHODID_GENERATE_PVC_CONTROL_PLANE_AUTH_TOKEN:
          serviceImpl.generatePvcControlPlaneAuthToken((com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse>) responseObserver);
          break;
        case METHODID_GENERATE_PVC_WORKLOAD_AUTH_TOKEN:
          serviceImpl.generatePvcWorkloadAuthToken((com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse>) responseObserver);
          break;
        case METHODID_REGISTER_PVC_BASE_CLUSTER:
          serviceImpl.registerPvcBaseCluster((com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse>) responseObserver);
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
          getListAllPvcControlPlanesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest,
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse>(
                service, METHODID_LIST_ALL_PVC_CONTROL_PLANES)))
        .addMethod(
          getGeneratePvcControlPlaneAuthTokenMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest,
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse>(
                service, METHODID_GENERATE_PVC_CONTROL_PLANE_AUTH_TOKEN)))
        .addMethod(
          getGeneratePvcWorkloadAuthTokenMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenRequest,
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcWorkloadAuthTokenResponse>(
                service, METHODID_GENERATE_PVC_WORKLOAD_AUTH_TOKEN)))
        .addMethod(
          getRegisterPvcBaseClusterMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest,
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse>(
                service, METHODID_REGISTER_PVC_BASE_CLUSTER)))
        .build();
  }

  private static abstract class RemoteClusterInternalBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RemoteClusterInternalBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RemoteClusterInternal");
    }
  }

  private static final class RemoteClusterInternalFileDescriptorSupplier
      extends RemoteClusterInternalBaseDescriptorSupplier {
    RemoteClusterInternalFileDescriptorSupplier() {}
  }

  private static final class RemoteClusterInternalMethodDescriptorSupplier
      extends RemoteClusterInternalBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    RemoteClusterInternalMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (RemoteClusterInternalGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RemoteClusterInternalFileDescriptorSupplier())
              .addMethod(getListAllPvcControlPlanesMethod())
              .addMethod(getGeneratePvcControlPlaneAuthTokenMethod())
              .addMethod(getGeneratePvcWorkloadAuthTokenMethod())
              .addMethod(getRegisterPvcBaseClusterMethod())
              .build();
        }
      }
    }
    return result;
  }
}
