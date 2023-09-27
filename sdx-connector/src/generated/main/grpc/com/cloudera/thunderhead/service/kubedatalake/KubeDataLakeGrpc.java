package com.cloudera.thunderhead.service.kubedatalake;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.42.1)",
    comments = "Source: kubedatalake.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class KubeDataLakeGrpc {

  private KubeDataLakeGrpc() {}

  public static final String SERVICE_NAME = "kubedatalake.KubeDataLake";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeRequest,
      com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeResponse> getCreateDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateDatalake",
      requestType = com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeRequest,
      com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeResponse> getCreateDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeRequest, com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeResponse> getCreateDatalakeMethod;
    if ((getCreateDatalakeMethod = KubeDataLakeGrpc.getCreateDatalakeMethod) == null) {
      synchronized (KubeDataLakeGrpc.class) {
        if ((getCreateDatalakeMethod = KubeDataLakeGrpc.getCreateDatalakeMethod) == null) {
          KubeDataLakeGrpc.getCreateDatalakeMethod = getCreateDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeRequest, com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new KubeDataLakeMethodDescriptorSupplier("CreateDatalake"))
              .build();
        }
      }
    }
    return getCreateDatalakeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeRequest,
      com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeResponse> getDeleteDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteDatalake",
      requestType = com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeRequest,
      com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeResponse> getDeleteDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeRequest, com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeResponse> getDeleteDatalakeMethod;
    if ((getDeleteDatalakeMethod = KubeDataLakeGrpc.getDeleteDatalakeMethod) == null) {
      synchronized (KubeDataLakeGrpc.class) {
        if ((getDeleteDatalakeMethod = KubeDataLakeGrpc.getDeleteDatalakeMethod) == null) {
          KubeDataLakeGrpc.getDeleteDatalakeMethod = getDeleteDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeRequest, com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new KubeDataLakeMethodDescriptorSupplier("DeleteDatalake"))
              .build();
        }
      }
    }
    return getDeleteDatalakeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeRequest,
      com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeResponse> getDescribeDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeDatalake",
      requestType = com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeRequest,
      com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeResponse> getDescribeDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeRequest, com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeResponse> getDescribeDatalakeMethod;
    if ((getDescribeDatalakeMethod = KubeDataLakeGrpc.getDescribeDatalakeMethod) == null) {
      synchronized (KubeDataLakeGrpc.class) {
        if ((getDescribeDatalakeMethod = KubeDataLakeGrpc.getDescribeDatalakeMethod) == null) {
          KubeDataLakeGrpc.getDescribeDatalakeMethod = getDescribeDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeRequest, com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new KubeDataLakeMethodDescriptorSupplier("DescribeDatalake"))
              .build();
        }
      }
    }
    return getDescribeDatalakeMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static KubeDataLakeStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<KubeDataLakeStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<KubeDataLakeStub>() {
        @java.lang.Override
        public KubeDataLakeStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new KubeDataLakeStub(channel, callOptions);
        }
      };
    return KubeDataLakeStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static KubeDataLakeBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<KubeDataLakeBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<KubeDataLakeBlockingStub>() {
        @java.lang.Override
        public KubeDataLakeBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new KubeDataLakeBlockingStub(channel, callOptions);
        }
      };
    return KubeDataLakeBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static KubeDataLakeFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<KubeDataLakeFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<KubeDataLakeFutureStub>() {
        @java.lang.Override
        public KubeDataLakeFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new KubeDataLakeFutureStub(channel, callOptions);
        }
      };
    return KubeDataLakeFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class KubeDataLakeImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Create a Datalake
     * </pre>
     */
    public void createDatalake(com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateDatalakeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete a Datalake
     * </pre>
     */
    public void deleteDatalake(com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteDatalakeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    public void describeDatalake(com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeDatalakeMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateDatalakeMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeRequest,
                com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeResponse>(
                  this, METHODID_CREATE_DATALAKE)))
          .addMethod(
            getDeleteDatalakeMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeRequest,
                com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeResponse>(
                  this, METHODID_DELETE_DATALAKE)))
          .addMethod(
            getDescribeDatalakeMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeRequest,
                com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeResponse>(
                  this, METHODID_DESCRIBE_DATALAKE)))
          .build();
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class KubeDataLakeStub extends io.grpc.stub.AbstractAsyncStub<KubeDataLakeStub> {
    private KubeDataLakeStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected KubeDataLakeStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new KubeDataLakeStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a Datalake
     * </pre>
     */
    public void createDatalake(com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateDatalakeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete a Datalake
     * </pre>
     */
    public void deleteDatalake(com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteDatalakeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    public void describeDatalake(com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeDatalakeMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class KubeDataLakeBlockingStub extends io.grpc.stub.AbstractBlockingStub<KubeDataLakeBlockingStub> {
    private KubeDataLakeBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected KubeDataLakeBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new KubeDataLakeBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeResponse createDatalake(com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeResponse deleteDatalake(com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeResponse describeDatalake(com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeDatalakeMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class KubeDataLakeFutureStub extends io.grpc.stub.AbstractFutureStub<KubeDataLakeFutureStub> {
    private KubeDataLakeFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected KubeDataLakeFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new KubeDataLakeFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a Datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeResponse> createDatalake(
        com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateDatalakeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete a Datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeResponse> deleteDatalake(
        com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteDatalakeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeResponse> describeDatalake(
        com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeDatalakeMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_DATALAKE = 0;
  private static final int METHODID_DELETE_DATALAKE = 1;
  private static final int METHODID_DESCRIBE_DATALAKE = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final KubeDataLakeImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(KubeDataLakeImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_DATALAKE:
          serviceImpl.createDatalake((com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.CreateDatalakeResponse>) responseObserver);
          break;
        case METHODID_DELETE_DATALAKE:
          serviceImpl.deleteDatalake((com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DeleteDatalakeResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_DATALAKE:
          serviceImpl.describeDatalake((com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.DescribeDatalakeResponse>) responseObserver);
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

  private static abstract class KubeDataLakeBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    KubeDataLakeBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("KubeDataLake");
    }
  }

  private static final class KubeDataLakeFileDescriptorSupplier
      extends KubeDataLakeBaseDescriptorSupplier {
    KubeDataLakeFileDescriptorSupplier() {}
  }

  private static final class KubeDataLakeMethodDescriptorSupplier
      extends KubeDataLakeBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    KubeDataLakeMethodDescriptorSupplier(String methodName) {
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
      synchronized (KubeDataLakeGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new KubeDataLakeFileDescriptorSupplier())
              .addMethod(getCreateDatalakeMethod())
              .addMethod(getDeleteDatalakeMethod())
              .addMethod(getDescribeDatalakeMethod())
              .build();
        }
      }
    }
    return result;
  }
}
