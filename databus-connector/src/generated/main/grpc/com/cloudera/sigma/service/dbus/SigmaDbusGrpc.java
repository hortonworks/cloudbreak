package com.cloudera.sigma.service.dbus;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.12.0)",
    comments = "Source: sigmadbus.proto")
public final class SigmaDbusGrpc {

  private SigmaDbusGrpc() {}

  public static final String SERVICE_NAME = "sigmadbus.SigmaDbus";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getPutRecordMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest,
      com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> METHOD_PUT_RECORD = getPutRecordMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest,
      com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> getPutRecordMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest,
      com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> getPutRecordMethod() {
    return getPutRecordMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest,
      com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> getPutRecordMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest, com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> getPutRecordMethod;
    if ((getPutRecordMethod = SigmaDbusGrpc.getPutRecordMethod) == null) {
      synchronized (SigmaDbusGrpc.class) {
        if ((getPutRecordMethod = SigmaDbusGrpc.getPutRecordMethod) == null) {
          SigmaDbusGrpc.getPutRecordMethod = getPutRecordMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest, com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sigmadbus.SigmaDbus", "PutRecord"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetVersionMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
      com.cloudera.thunderhead.service.common.version.Version.VersionResponse> METHOD_GET_VERSION = getGetVersionMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
      com.cloudera.thunderhead.service.common.version.Version.VersionResponse> getGetVersionMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
      com.cloudera.thunderhead.service.common.version.Version.VersionResponse> getGetVersionMethod() {
    return getGetVersionMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
      com.cloudera.thunderhead.service.common.version.Version.VersionResponse> getGetVersionMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse> getGetVersionMethod;
    if ((getGetVersionMethod = SigmaDbusGrpc.getGetVersionMethod) == null) {
      synchronized (SigmaDbusGrpc.class) {
        if ((getGetVersionMethod = SigmaDbusGrpc.getGetVersionMethod) == null) {
          SigmaDbusGrpc.getGetVersionMethod = getGetVersionMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sigmadbus.SigmaDbus", "GetVersion"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getValidateUuidMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest,
      com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> METHOD_VALIDATE_UUID = getValidateUuidMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest,
      com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> getValidateUuidMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest,
      com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> getValidateUuidMethod() {
    return getValidateUuidMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest,
      com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> getValidateUuidMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest, com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> getValidateUuidMethod;
    if ((getValidateUuidMethod = SigmaDbusGrpc.getValidateUuidMethod) == null) {
      synchronized (SigmaDbusGrpc.class) {
        if ((getValidateUuidMethod = SigmaDbusGrpc.getValidateUuidMethod) == null) {
          SigmaDbusGrpc.getValidateUuidMethod = getValidateUuidMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest, com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sigmadbus.SigmaDbus", "ValidateUuid"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getNotifyFileUploadMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest,
      com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> METHOD_NOTIFY_FILE_UPLOAD = getNotifyFileUploadMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest,
      com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> getNotifyFileUploadMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest,
      com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> getNotifyFileUploadMethod() {
    return getNotifyFileUploadMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest,
      com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> getNotifyFileUploadMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest, com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> getNotifyFileUploadMethod;
    if ((getNotifyFileUploadMethod = SigmaDbusGrpc.getNotifyFileUploadMethod) == null) {
      synchronized (SigmaDbusGrpc.class) {
        if ((getNotifyFileUploadMethod = SigmaDbusGrpc.getNotifyFileUploadMethod) == null) {
          SigmaDbusGrpc.getNotifyFileUploadMethod = getNotifyFileUploadMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest, com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "sigmadbus.SigmaDbus", "NotifyFileUpload"))
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
    return new SigmaDbusStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SigmaDbusBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new SigmaDbusBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SigmaDbusFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new SigmaDbusFutureStub(channel);
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class SigmaDbusImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Put a new record.
     * </pre>
     */
    public void putRecord(com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPutRecordMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Get the service version
     * </pre>
     */
    public void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetVersionMethodHelper(), responseObserver);
    }

    /**
     */
    public void validateUuid(com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getValidateUuidMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * notify about the file arrival (on prem)
     * </pre>
     */
    public void notifyFileUpload(com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getNotifyFileUploadMethodHelper(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getPutRecordMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest,
                com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse>(
                  this, METHODID_PUT_RECORD)))
          .addMethod(
            getGetVersionMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
                com.cloudera.thunderhead.service.common.version.Version.VersionResponse>(
                  this, METHODID_GET_VERSION)))
          .addMethod(
            getValidateUuidMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest,
                com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse>(
                  this, METHODID_VALIDATE_UUID)))
          .addMethod(
            getNotifyFileUploadMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest,
                com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse>(
                  this, METHODID_NOTIFY_FILE_UPLOAD)))
          .build();
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class SigmaDbusStub extends io.grpc.stub.AbstractStub<SigmaDbusStub> {
    private SigmaDbusStub(io.grpc.Channel channel) {
      super(channel);
    }

    private SigmaDbusStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SigmaDbusStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new SigmaDbusStub(channel, callOptions);
    }

    /**
     * <pre>
     * Put a new record.
     * </pre>
     */
    public void putRecord(com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPutRecordMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the service version
     * </pre>
     */
    public void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetVersionMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void validateUuid(com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getValidateUuidMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * notify about the file arrival (on prem)
     * </pre>
     */
    public void notifyFileUpload(com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getNotifyFileUploadMethodHelper(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class SigmaDbusBlockingStub extends io.grpc.stub.AbstractStub<SigmaDbusBlockingStub> {
    private SigmaDbusBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private SigmaDbusBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SigmaDbusBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new SigmaDbusBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Put a new record.
     * </pre>
     */
    public com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse putRecord(com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest request) {
      return blockingUnaryCall(
          getChannel(), getPutRecordMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the service version
     * </pre>
     */
    public com.cloudera.thunderhead.service.common.version.Version.VersionResponse getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetVersionMethodHelper(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse validateUuid(com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest request) {
      return blockingUnaryCall(
          getChannel(), getValidateUuidMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * notify about the file arrival (on prem)
     * </pre>
     */
    public com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse notifyFileUpload(com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest request) {
      return blockingUnaryCall(
          getChannel(), getNotifyFileUploadMethodHelper(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class SigmaDbusFutureStub extends io.grpc.stub.AbstractStub<SigmaDbusFutureStub> {
    private SigmaDbusFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private SigmaDbusFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SigmaDbusFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new SigmaDbusFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Put a new record.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.sigma.service.dbus.DbusProto.PutRecordResponse> putRecord(
        com.cloudera.sigma.service.dbus.DbusProto.PutRecordRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPutRecordMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the service version
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> getVersion(
        com.cloudera.thunderhead.service.common.version.Version.VersionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetVersionMethodHelper(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidResponse> validateUuid(
        com.cloudera.sigma.service.dbus.DbusProto.ValidateUuidRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getValidateUuidMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * notify about the file arrival (on prem)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadResponse> notifyFileUpload(
        com.cloudera.sigma.service.dbus.DbusProto.NotifyFileUploadRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getNotifyFileUploadMethodHelper(), getCallOptions()), request);
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
    private final SigmaDbusImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SigmaDbusImplBase serviceImpl, int methodId) {
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
    private final String methodName;

    SigmaDbusMethodDescriptorSupplier(String methodName) {
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
              .addMethod(getPutRecordMethodHelper())
              .addMethod(getGetVersionMethodHelper())
              .addMethod(getValidateUuidMethodHelper())
              .addMethod(getNotifyFileUploadMethodHelper())
              .build();
        }
      }
    }
    return result;
  }
}
