package com.cloudera.sigma.service.dbus;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class SigmaDbusGrpc {

  private SigmaDbusGrpc() {}

  public static final java.lang.String SERVICE_NAME = "sigmadbus.SigmaDbus";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest,
      com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> getPutRecordMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PutRecord",
      requestType = com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest.class,
      responseType = com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest,
      com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> getPutRecordMethod() {
    io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest, com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> getPutRecordMethod;
    if ((getPutRecordMethod = SigmaDbusGrpc.getPutRecordMethod) == null) {
      synchronized (SigmaDbusGrpc.class) {
        if ((getPutRecordMethod = SigmaDbusGrpc.getPutRecordMethod) == null) {
          SigmaDbusGrpc.getPutRecordMethod = getPutRecordMethod =
              io.grpc.MethodDescriptor.<com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest, com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PutRecord"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SigmaDbusMethodDescriptorSupplier("PutRecord"))
              .build();
        }
      }
    }
    return getPutRecordMethod;
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
    if ((getGetVersionMethod = SigmaDbusGrpc.getGetVersionMethod) == null) {
      synchronized (SigmaDbusGrpc.class) {
        if ((getGetVersionMethod = SigmaDbusGrpc.getGetVersionMethod) == null) {
          SigmaDbusGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SigmaDbusMethodDescriptorSupplier("GetVersion"))
              .build();
        }
      }
    }
    return getGetVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest,
      com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> getValidateUuidMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ValidateUuid",
      requestType = com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest.class,
      responseType = com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest,
      com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> getValidateUuidMethod() {
    io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest, com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> getValidateUuidMethod;
    if ((getValidateUuidMethod = SigmaDbusGrpc.getValidateUuidMethod) == null) {
      synchronized (SigmaDbusGrpc.class) {
        if ((getValidateUuidMethod = SigmaDbusGrpc.getValidateUuidMethod) == null) {
          SigmaDbusGrpc.getValidateUuidMethod = getValidateUuidMethod =
              io.grpc.MethodDescriptor.<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest, com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ValidateUuid"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SigmaDbusMethodDescriptorSupplier("ValidateUuid"))
              .build();
        }
      }
    }
    return getValidateUuidMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest,
      com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> getNotifyFileUploadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "NotifyFileUpload",
      requestType = com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest.class,
      responseType = com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest,
      com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> getNotifyFileUploadMethod() {
    io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest, com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> getNotifyFileUploadMethod;
    if ((getNotifyFileUploadMethod = SigmaDbusGrpc.getNotifyFileUploadMethod) == null) {
      synchronized (SigmaDbusGrpc.class) {
        if ((getNotifyFileUploadMethod = SigmaDbusGrpc.getNotifyFileUploadMethod) == null) {
          SigmaDbusGrpc.getNotifyFileUploadMethod = getNotifyFileUploadMethod =
              io.grpc.MethodDescriptor.<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest, com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "NotifyFileUpload"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SigmaDbusMethodDescriptorSupplier("NotifyFileUpload"))
              .build();
        }
      }
    }
    return getNotifyFileUploadMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SigmaDbusStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SigmaDbusStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SigmaDbusStub>() {
        @java.lang.Override
        public SigmaDbusStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SigmaDbusStub(channel, callOptions);
        }
      };
    return SigmaDbusStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static SigmaDbusBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SigmaDbusBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SigmaDbusBlockingV2Stub>() {
        @java.lang.Override
        public SigmaDbusBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SigmaDbusBlockingV2Stub(channel, callOptions);
        }
      };
    return SigmaDbusBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SigmaDbusBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SigmaDbusBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SigmaDbusBlockingStub>() {
        @java.lang.Override
        public SigmaDbusBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SigmaDbusBlockingStub(channel, callOptions);
        }
      };
    return SigmaDbusBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SigmaDbusFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SigmaDbusFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SigmaDbusFutureStub>() {
        @java.lang.Override
        public SigmaDbusFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SigmaDbusFutureStub(channel, callOptions);
        }
      };
    return SigmaDbusFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Put a new record.
     * </pre>
     */
    default void putRecord(com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPutRecordMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the service version
     * </pre>
     */
    default void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetVersionMethod(), responseObserver);
    }

    /**
     */
    default void validateUuid(com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getValidateUuidMethod(), responseObserver);
    }

    /**
     * <pre>
     * notify about the file arrival (on prem)
     * </pre>
     */
    default void notifyFileUpload(com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getNotifyFileUploadMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service SigmaDbus.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class SigmaDbusImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return SigmaDbusGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service SigmaDbus.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class SigmaDbusStub
      extends io.grpc.stub.AbstractAsyncStub<SigmaDbusStub> {
    private SigmaDbusStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SigmaDbusStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SigmaDbusStub(channel, callOptions);
    }

    /**
     * <pre>
     * Put a new record.
     * </pre>
     */
    public void putRecord(com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPutRecordMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the service version
     * </pre>
     */
    public void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetVersionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void validateUuid(com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getValidateUuidMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * notify about the file arrival (on prem)
     * </pre>
     */
    public void notifyFileUpload(com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getNotifyFileUploadMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service SigmaDbus.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class SigmaDbusBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<SigmaDbusBlockingV2Stub> {
    private SigmaDbusBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SigmaDbusBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SigmaDbusBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Put a new record.
     * </pre>
     */
    public com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse putRecord(com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getPutRecordMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the service version
     * </pre>
     */
    public com.cloudera.thunderhead.service.common.version.Version.VersionResponse getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetVersionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse validateUuid(com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getValidateUuidMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * notify about the file arrival (on prem)
     * </pre>
     */
    public com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse notifyFileUpload(com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getNotifyFileUploadMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service SigmaDbus.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class SigmaDbusBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<SigmaDbusBlockingStub> {
    private SigmaDbusBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SigmaDbusBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SigmaDbusBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Put a new record.
     * </pre>
     */
    public com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse putRecord(com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPutRecordMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the service version
     * </pre>
     */
    public com.cloudera.thunderhead.service.common.version.Version.VersionResponse getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetVersionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse validateUuid(com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getValidateUuidMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * notify about the file arrival (on prem)
     * </pre>
     */
    public com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse notifyFileUpload(com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getNotifyFileUploadMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service SigmaDbus.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class SigmaDbusFutureStub
      extends io.grpc.stub.AbstractFutureStub<SigmaDbusFutureStub> {
    private SigmaDbusFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SigmaDbusFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SigmaDbusFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Put a new record.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> putRecord(
        com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPutRecordMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the service version
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> getVersion(
        com.cloudera.thunderhead.service.common.version.Version.VersionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetVersionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> validateUuid(
        com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getValidateUuidMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * notify about the file arrival (on prem)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> notifyFileUpload(
        com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getNotifyFileUploadMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PUT_RECORD = 0;
  private static final int METHODID_GET_VERSION = 1;
  private static final int METHODID_VALIDATE_UUID = 2;
  private static final int METHODID_NOTIFY_FILE_UPLOAD = 3;

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
        case METHODID_PUT_RECORD:
          serviceImpl.putRecord((com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse>) responseObserver);
          break;
        case METHODID_GET_VERSION:
          serviceImpl.getVersion((com.cloudera.thunderhead.service.common.version.Version.VersionRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse>) responseObserver);
          break;
        case METHODID_VALIDATE_UUID:
          serviceImpl.validateUuid((com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse>) responseObserver);
          break;
        case METHODID_NOTIFY_FILE_UPLOAD:
          serviceImpl.notifyFileUpload((com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse>) responseObserver);
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
          getPutRecordMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest,
              com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse>(
                service, METHODID_PUT_RECORD)))
        .addMethod(
          getGetVersionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
              com.cloudera.thunderhead.service.common.version.Version.VersionResponse>(
                service, METHODID_GET_VERSION)))
        .addMethod(
          getValidateUuidMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest,
              com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse>(
                service, METHODID_VALIDATE_UUID)))
        .addMethod(
          getNotifyFileUploadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest,
              com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse>(
                service, METHODID_NOTIFY_FILE_UPLOAD)))
        .build();
  }

  private static abstract class SigmaDbusBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SigmaDbusBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.sigma.service.dbus.DbusProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SigmaDbus");
    }
  }

  private static final class SigmaDbusFileDescriptorSupplier
      extends SigmaDbusBaseDescriptorSupplier {
    SigmaDbusFileDescriptorSupplier() {}
  }

  private static final class SigmaDbusMethodDescriptorSupplier
      extends SigmaDbusBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    SigmaDbusMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (SigmaDbusGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SigmaDbusFileDescriptorSupplier())
              .addMethod(getPutRecordMethod())
              .addMethod(getGetVersionMethod())
              .addMethod(getValidateUuidMethod())
              .addMethod(getNotifyFileUploadMethod())
              .build();
        }
      }
    }
    return result;
  }
}
