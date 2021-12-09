package com.cloudera.thunderhead.service.minasshd;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.42.1)",
    comments = "Source: minasshd.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class MinaSshdGrpc {

  private MinaSshdGrpc() {}

  public static final String SERVICE_NAME = "minasshd.MinaSshd";

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
    if ((getGetVersionMethod = MinaSshdGrpc.getGetVersionMethod) == null) {
      synchronized (MinaSshdGrpc.class) {
        if ((getGetVersionMethod = MinaSshdGrpc.getGetVersionMethod) == null) {
          MinaSshdGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new MinaSshdMethodDescriptorSupplier("GetVersion"))
              .build();
        }
      }
    }
    return getGetVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointRequest,
      com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointResponse> getGetServiceEndpointMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetServiceEndpoint",
      requestType = com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointRequest.class,
      responseType = com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointRequest,
      com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointResponse> getGetServiceEndpointMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointRequest, com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointResponse> getGetServiceEndpointMethod;
    if ((getGetServiceEndpointMethod = MinaSshdGrpc.getGetServiceEndpointMethod) == null) {
      synchronized (MinaSshdGrpc.class) {
        if ((getGetServiceEndpointMethod = MinaSshdGrpc.getGetServiceEndpointMethod) == null) {
          MinaSshdGrpc.getGetServiceEndpointMethod = getGetServiceEndpointMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointRequest, com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetServiceEndpoint"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointResponse.getDefaultInstance()))
              .setSchemaDescriptor(new MinaSshdMethodDescriptorSupplier("GetServiceEndpoint"))
              .build();
        }
      }
    }
    return getGetServiceEndpointMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static MinaSshdStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MinaSshdStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MinaSshdStub>() {
        @java.lang.Override
        public MinaSshdStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MinaSshdStub(channel, callOptions);
        }
      };
    return MinaSshdStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MinaSshdBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MinaSshdBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MinaSshdBlockingStub>() {
        @java.lang.Override
        public MinaSshdBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MinaSshdBlockingStub(channel, callOptions);
        }
      };
    return MinaSshdBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MinaSshdFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MinaSshdFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MinaSshdFutureStub>() {
        @java.lang.Override
        public MinaSshdFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MinaSshdFutureStub(channel, callOptions);
        }
      };
    return MinaSshdFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class MinaSshdImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Get the service version.
     * </pre>
     */
    public void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetVersionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get service endpoints.
     * </pre>
     */
    public void getServiceEndpoint(com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetServiceEndpointMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetVersionMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
                com.cloudera.thunderhead.service.common.version.Version.VersionResponse>(
                  this, METHODID_GET_VERSION)))
          .addMethod(
            getGetServiceEndpointMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointRequest,
                com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointResponse>(
                  this, METHODID_GET_SERVICE_ENDPOINT)))
          .build();
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MinaSshdStub extends io.grpc.stub.AbstractAsyncStub<MinaSshdStub> {
    private MinaSshdStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MinaSshdStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MinaSshdStub(channel, callOptions);
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
     * <pre>
     * Get service endpoints.
     * </pre>
     */
    public void getServiceEndpoint(com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetServiceEndpointMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MinaSshdBlockingStub extends io.grpc.stub.AbstractBlockingStub<MinaSshdBlockingStub> {
    private MinaSshdBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MinaSshdBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MinaSshdBlockingStub(channel, callOptions);
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
     * <pre>
     * Get service endpoints.
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointResponse getServiceEndpoint(com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetServiceEndpointMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MinaSshdFutureStub extends io.grpc.stub.AbstractFutureStub<MinaSshdFutureStub> {
    private MinaSshdFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MinaSshdFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MinaSshdFutureStub(channel, callOptions);
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
     * <pre>
     * Get service endpoints.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointResponse> getServiceEndpoint(
        com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetServiceEndpointMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_GET_SERVICE_ENDPOINT = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final MinaSshdImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(MinaSshdImplBase serviceImpl, int methodId) {
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
        case METHODID_GET_SERVICE_ENDPOINT:
          serviceImpl.getServiceEndpoint((com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshd.MinaSshdProto.GetServiceEndpointResponse>) responseObserver);
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

  private static abstract class MinaSshdBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    MinaSshdBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.minasshd.MinaSshdProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("MinaSshd");
    }
  }

  private static final class MinaSshdFileDescriptorSupplier
      extends MinaSshdBaseDescriptorSupplier {
    MinaSshdFileDescriptorSupplier() {}
  }

  private static final class MinaSshdMethodDescriptorSupplier
      extends MinaSshdBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    MinaSshdMethodDescriptorSupplier(String methodName) {
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
      synchronized (MinaSshdGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new MinaSshdFileDescriptorSupplier())
              .addMethod(getGetVersionMethod())
              .addMethod(getGetServiceEndpointMethod())
              .build();
        }
      }
    }
    return result;
  }
}
