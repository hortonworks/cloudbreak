package com.cloudera.thunderhead.service.meteringingestion;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all RPCs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class MeteringIngestionGrpc {

  private MeteringIngestionGrpc() {}

  public static final java.lang.String SERVICE_NAME = "meteringingestion.MeteringIngestion";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest,
      com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse> getSubmitEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SubmitEvent",
      requestType = com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest.class,
      responseType = com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest,
      com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse> getSubmitEventMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest, com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse> getSubmitEventMethod;
    if ((getSubmitEventMethod = MeteringIngestionGrpc.getSubmitEventMethod) == null) {
      synchronized (MeteringIngestionGrpc.class) {
        if ((getSubmitEventMethod = MeteringIngestionGrpc.getSubmitEventMethod) == null) {
          MeteringIngestionGrpc.getSubmitEventMethod = getSubmitEventMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest, com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SubmitEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse.getDefaultInstance()))
              .setSchemaDescriptor(new MeteringIngestionMethodDescriptorSupplier("SubmitEvent"))
              .build();
        }
      }
    }
    return getSubmitEventMethod;
  }

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
    if ((getGetVersionMethod = MeteringIngestionGrpc.getGetVersionMethod) == null) {
      synchronized (MeteringIngestionGrpc.class) {
        if ((getGetVersionMethod = MeteringIngestionGrpc.getGetVersionMethod) == null) {
          MeteringIngestionGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new MeteringIngestionMethodDescriptorSupplier("GetVersion"))
              .build();
        }
      }
    }
    return getGetVersionMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static MeteringIngestionStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MeteringIngestionStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MeteringIngestionStub>() {
        @java.lang.Override
        public MeteringIngestionStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MeteringIngestionStub(channel, callOptions);
        }
      };
    return MeteringIngestionStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static MeteringIngestionBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MeteringIngestionBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MeteringIngestionBlockingV2Stub>() {
        @java.lang.Override
        public MeteringIngestionBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MeteringIngestionBlockingV2Stub(channel, callOptions);
        }
      };
    return MeteringIngestionBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MeteringIngestionBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MeteringIngestionBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MeteringIngestionBlockingStub>() {
        @java.lang.Override
        public MeteringIngestionBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MeteringIngestionBlockingStub(channel, callOptions);
        }
      };
    return MeteringIngestionBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MeteringIngestionFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MeteringIngestionFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MeteringIngestionFutureStub>() {
        @java.lang.Override
        public MeteringIngestionFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MeteringIngestionFutureStub(channel, callOptions);
        }
      };
    return MeteringIngestionFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * For future compatibility, all RPCs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Submit a new metering event
     * </pre>
     */
    default void submitEvent(com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSubmitEventMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the service version.
     * </pre>
     */
    default void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetVersionMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service MeteringIngestion.
   * <pre>
   * For future compatibility, all RPCs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class MeteringIngestionImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return MeteringIngestionGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service MeteringIngestion.
   * <pre>
   * For future compatibility, all RPCs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MeteringIngestionStub
      extends io.grpc.stub.AbstractAsyncStub<MeteringIngestionStub> {
    private MeteringIngestionStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MeteringIngestionStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MeteringIngestionStub(channel, callOptions);
    }

    /**
     * <pre>
     * Submit a new metering event
     * </pre>
     */
    public void submitEvent(com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSubmitEventMethod(), getCallOptions()), request, responseObserver);
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
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service MeteringIngestion.
   * <pre>
   * For future compatibility, all RPCs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MeteringIngestionBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<MeteringIngestionBlockingV2Stub> {
    private MeteringIngestionBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MeteringIngestionBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MeteringIngestionBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Submit a new metering event
     * </pre>
     */
    public com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse submitEvent(com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getSubmitEventMethod(), getCallOptions(), request);
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
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service MeteringIngestion.
   * <pre>
   * For future compatibility, all RPCs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MeteringIngestionBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<MeteringIngestionBlockingStub> {
    private MeteringIngestionBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MeteringIngestionBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MeteringIngestionBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Submit a new metering event
     * </pre>
     */
    public com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse submitEvent(com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSubmitEventMethod(), getCallOptions(), request);
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
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service MeteringIngestion.
   * <pre>
   * For future compatibility, all RPCs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MeteringIngestionFutureStub
      extends io.grpc.stub.AbstractFutureStub<MeteringIngestionFutureStub> {
    private MeteringIngestionFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MeteringIngestionFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MeteringIngestionFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Submit a new metering event
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse> submitEvent(
        com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSubmitEventMethod(), getCallOptions()), request);
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
  }

  private static final int METHODID_SUBMIT_EVENT = 0;
  private static final int METHODID_GET_VERSION = 1;

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
        case METHODID_SUBMIT_EVENT:
          serviceImpl.submitEvent((com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse>) responseObserver);
          break;
        case METHODID_GET_VERSION:
          serviceImpl.getVersion((com.cloudera.thunderhead.service.common.version.Version.VersionRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse>) responseObserver);
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
          getSubmitEventMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest,
              com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse>(
                service, METHODID_SUBMIT_EVENT)))
        .addMethod(
          getGetVersionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
              com.cloudera.thunderhead.service.common.version.Version.VersionResponse>(
                service, METHODID_GET_VERSION)))
        .build();
  }

  private static abstract class MeteringIngestionBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    MeteringIngestionBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("MeteringIngestion");
    }
  }

  private static final class MeteringIngestionFileDescriptorSupplier
      extends MeteringIngestionBaseDescriptorSupplier {
    MeteringIngestionFileDescriptorSupplier() {}
  }

  private static final class MeteringIngestionMethodDescriptorSupplier
      extends MeteringIngestionBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    MeteringIngestionMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (MeteringIngestionGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new MeteringIngestionFileDescriptorSupplier())
              .addMethod(getSubmitEventMethod())
              .addMethod(getGetVersionMethod())
              .build();
        }
      }
    }
    return result;
  }
}
