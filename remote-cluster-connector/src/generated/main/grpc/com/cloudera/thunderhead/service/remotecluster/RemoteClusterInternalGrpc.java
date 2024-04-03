package com.cloudera.thunderhead.service.remotecluster;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * RemoteCluster endpoints for internal use only.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.53.0)",
    comments = "Source: remoteclusterinternal.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class RemoteClusterInternalGrpc {

  private RemoteClusterInternalGrpc() {}

  public static final String SERVICE_NAME = "remotecluster.RemoteClusterInternal";

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
   * RemoteCluster endpoints for internal use only.
   * </pre>
   */
  public static abstract class RemoteClusterInternalImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * List all registered Private Cloud control plane configurations across all accounts.
     * </pre>
     */
    public void listAllPvcControlPlanes(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListAllPvcControlPlanesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Generate an auth token for the requests sent to Private Cloud control plane.
     * </pre>
     */
    public void generatePvcControlPlaneAuthToken(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGeneratePvcControlPlaneAuthTokenMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getListAllPvcControlPlanesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest,
                com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse>(
                  this, METHODID_LIST_ALL_PVC_CONTROL_PLANES)))
          .addMethod(
            getGeneratePvcControlPlaneAuthTokenMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest,
                com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse>(
                  this, METHODID_GENERATE_PVC_CONTROL_PLANE_AUTH_TOKEN)))
          .build();
    }
  }

  /**
   * <pre>
   * RemoteCluster endpoints for internal use only.
   * </pre>
   */
  public static final class RemoteClusterInternalStub extends io.grpc.stub.AbstractAsyncStub<RemoteClusterInternalStub> {
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
     * List all registered Private Cloud control plane configurations across all accounts.
     * </pre>
     */
    public void listAllPvcControlPlanes(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListAllPvcControlPlanesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Generate an auth token for the requests sent to Private Cloud control plane.
     * </pre>
     */
    public void generatePvcControlPlaneAuthToken(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGeneratePvcControlPlaneAuthTokenMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * RemoteCluster endpoints for internal use only.
   * </pre>
   */
  public static final class RemoteClusterInternalBlockingStub extends io.grpc.stub.AbstractBlockingStub<RemoteClusterInternalBlockingStub> {
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
     * List all registered Private Cloud control plane configurations across all accounts.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse listAllPvcControlPlanes(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListAllPvcControlPlanesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Generate an auth token for the requests sent to Private Cloud control plane.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse generatePvcControlPlaneAuthToken(com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGeneratePvcControlPlaneAuthTokenMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * RemoteCluster endpoints for internal use only.
   * </pre>
   */
  public static final class RemoteClusterInternalFutureStub extends io.grpc.stub.AbstractFutureStub<RemoteClusterInternalFutureStub> {
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
     * List all registered Private Cloud control plane configurations across all accounts.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse> listAllPvcControlPlanes(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListAllPvcControlPlanesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Generate an auth token for the requests sent to Private Cloud control plane.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenResponse> generatePvcControlPlaneAuthToken(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.GeneratePvcControlPlaneAuthTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGeneratePvcControlPlaneAuthTokenMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_ALL_PVC_CONTROL_PLANES = 0;
  private static final int METHODID_GENERATE_PVC_CONTROL_PLANE_AUTH_TOKEN = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RemoteClusterInternalImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RemoteClusterInternalImplBase serviceImpl, int methodId) {
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
    private final String methodName;

    RemoteClusterInternalMethodDescriptorSupplier(String methodName) {
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
              .build();
        }
      }
    }
    return result;
  }
}
