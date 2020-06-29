package com.cloudera.thunderhead.service.audit;

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
    comments = "Source: audit.proto")
public final class AuditGrpc {

  private AuditGrpc() {}

  public static final String SERVICE_NAME = "audit.Audit";

  // Static method descriptors that strictly reflect the proto.
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
    if ((getGetVersionMethod = AuditGrpc.getGetVersionMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getGetVersionMethod = AuditGrpc.getGetVersionMethod) == null) {
          AuditGrpc.getGetVersionMethod = getGetVersionMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "audit.Audit", "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new AuditMethodDescriptorSupplier("GetVersion"))
                  .build();
          }
        }
     }
     return getGetVersionMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateAuditEventMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> METHOD_CREATE_AUDIT_EVENT = getCreateAuditEventMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> getCreateAuditEventMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> getCreateAuditEventMethod() {
    return getCreateAuditEventMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> getCreateAuditEventMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest, com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> getCreateAuditEventMethod;
    if ((getCreateAuditEventMethod = AuditGrpc.getCreateAuditEventMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getCreateAuditEventMethod = AuditGrpc.getCreateAuditEventMethod) == null) {
          AuditGrpc.getCreateAuditEventMethod = getCreateAuditEventMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest, com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "audit.Audit", "CreateAuditEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new AuditMethodDescriptorSupplier("CreateAuditEvent"))
                  .build();
          }
        }
     }
     return getCreateAuditEventMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateAttemptAuditEventMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> METHOD_CREATE_ATTEMPT_AUDIT_EVENT = getCreateAttemptAuditEventMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> getCreateAttemptAuditEventMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> getCreateAttemptAuditEventMethod() {
    return getCreateAttemptAuditEventMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> getCreateAttemptAuditEventMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest, com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> getCreateAttemptAuditEventMethod;
    if ((getCreateAttemptAuditEventMethod = AuditGrpc.getCreateAttemptAuditEventMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getCreateAttemptAuditEventMethod = AuditGrpc.getCreateAttemptAuditEventMethod) == null) {
          AuditGrpc.getCreateAttemptAuditEventMethod = getCreateAttemptAuditEventMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest, com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "audit.Audit", "CreateAttemptAuditEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new AuditMethodDescriptorSupplier("CreateAttemptAuditEvent"))
                  .build();
          }
        }
     }
     return getCreateAttemptAuditEventMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateAttemptAuditEventWithResultMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> METHOD_UPDATE_ATTEMPT_AUDIT_EVENT_WITH_RESULT = getUpdateAttemptAuditEventWithResultMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> getUpdateAttemptAuditEventWithResultMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> getUpdateAttemptAuditEventWithResultMethod() {
    return getUpdateAttemptAuditEventWithResultMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> getUpdateAttemptAuditEventWithResultMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest, com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> getUpdateAttemptAuditEventWithResultMethod;
    if ((getUpdateAttemptAuditEventWithResultMethod = AuditGrpc.getUpdateAttemptAuditEventWithResultMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getUpdateAttemptAuditEventWithResultMethod = AuditGrpc.getUpdateAttemptAuditEventWithResultMethod) == null) {
          AuditGrpc.getUpdateAttemptAuditEventWithResultMethod = getUpdateAttemptAuditEventWithResultMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest, com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "audit.Audit", "UpdateAttemptAuditEventWithResult"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new AuditMethodDescriptorSupplier("UpdateAttemptAuditEventWithResult"))
                  .build();
          }
        }
     }
     return getUpdateAttemptAuditEventWithResultMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListEventsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> METHOD_LIST_EVENTS = getListEventsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> getListEventsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> getListEventsMethod() {
    return getListEventsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> getListEventsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest, com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> getListEventsMethod;
    if ((getListEventsMethod = AuditGrpc.getListEventsMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getListEventsMethod = AuditGrpc.getListEventsMethod) == null) {
          AuditGrpc.getListEventsMethod = getListEventsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest, com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "audit.Audit", "ListEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new AuditMethodDescriptorSupplier("ListEvents"))
                  .build();
          }
        }
     }
     return getListEventsMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getConfigureArchivingMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> METHOD_CONFIGURE_ARCHIVING = getConfigureArchivingMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> getConfigureArchivingMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> getConfigureArchivingMethod() {
    return getConfigureArchivingMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> getConfigureArchivingMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest, com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> getConfigureArchivingMethod;
    if ((getConfigureArchivingMethod = AuditGrpc.getConfigureArchivingMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getConfigureArchivingMethod = AuditGrpc.getConfigureArchivingMethod) == null) {
          AuditGrpc.getConfigureArchivingMethod = getConfigureArchivingMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest, com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "audit.Audit", "ConfigureArchiving"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new AuditMethodDescriptorSupplier("ConfigureArchiving"))
                  .build();
          }
        }
     }
     return getConfigureArchivingMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetArchivingConfigMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> METHOD_GET_ARCHIVING_CONFIG = getGetArchivingConfigMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> getGetArchivingConfigMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> getGetArchivingConfigMethod() {
    return getGetArchivingConfigMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> getGetArchivingConfigMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest, com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> getGetArchivingConfigMethod;
    if ((getGetArchivingConfigMethod = AuditGrpc.getGetArchivingConfigMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getGetArchivingConfigMethod = AuditGrpc.getGetArchivingConfigMethod) == null) {
          AuditGrpc.getGetArchivingConfigMethod = getGetArchivingConfigMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest, com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "audit.Audit", "GetArchivingConfig"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new AuditMethodDescriptorSupplier("GetArchivingConfig"))
                  .build();
          }
        }
     }
     return getGetArchivingConfigMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getArchiveAuditEventsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> METHOD_ARCHIVE_AUDIT_EVENTS = getArchiveAuditEventsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> getArchiveAuditEventsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> getArchiveAuditEventsMethod() {
    return getArchiveAuditEventsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> getArchiveAuditEventsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest, com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> getArchiveAuditEventsMethod;
    if ((getArchiveAuditEventsMethod = AuditGrpc.getArchiveAuditEventsMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getArchiveAuditEventsMethod = AuditGrpc.getArchiveAuditEventsMethod) == null) {
          AuditGrpc.getArchiveAuditEventsMethod = getArchiveAuditEventsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest, com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "audit.Audit", "ArchiveAuditEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new AuditMethodDescriptorSupplier("ArchiveAuditEvents"))
                  .build();
          }
        }
     }
     return getArchiveAuditEventsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AuditStub newStub(io.grpc.Channel channel) {
    return new AuditStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AuditBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new AuditBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AuditFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new AuditFutureStub(channel);
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class AuditImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Get the service version.
     * </pre>
     */
    public void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetVersionMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Create a new standalone audit event.
     * </pre>
     */
    public void createAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAuditEventMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Create a new attempt audit event. This call is normally followed by a
     * call to UpdateAttemptAuditEventWithResult.
     * </pre>
     */
    public void createAttemptAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAttemptAuditEventMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Update an existing attempt audit event with result data.
     * </pre>
     */
    public void updateAttemptAuditEventWithResult(com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateAttemptAuditEventWithResultMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * List audit events.
     * </pre>
     */
    public void listEvents(com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListEventsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Configure the audit service for archiving audit logs.
     * </pre>
     */
    public void configureArchiving(com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getConfigureArchivingMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Retrieve the current archiving configuration.
     * </pre>
     */
    public void getArchivingConfig(com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetArchivingConfigMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Archive audit events.
     * </pre>
     */
    public void archiveAuditEvents(com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getArchiveAuditEventsMethodHelper(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetVersionMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
                com.cloudera.thunderhead.service.common.version.Version.VersionResponse>(
                  this, METHODID_GET_VERSION)))
          .addMethod(
            getCreateAuditEventMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest,
                com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse>(
                  this, METHODID_CREATE_AUDIT_EVENT)))
          .addMethod(
            getCreateAttemptAuditEventMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest,
                com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse>(
                  this, METHODID_CREATE_ATTEMPT_AUDIT_EVENT)))
          .addMethod(
            getUpdateAttemptAuditEventWithResultMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest,
                com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse>(
                  this, METHODID_UPDATE_ATTEMPT_AUDIT_EVENT_WITH_RESULT)))
          .addMethod(
            getListEventsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest,
                com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse>(
                  this, METHODID_LIST_EVENTS)))
          .addMethod(
            getConfigureArchivingMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest,
                com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse>(
                  this, METHODID_CONFIGURE_ARCHIVING)))
          .addMethod(
            getGetArchivingConfigMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest,
                com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse>(
                  this, METHODID_GET_ARCHIVING_CONFIG)))
          .addMethod(
            getArchiveAuditEventsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest,
                com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse>(
                  this, METHODID_ARCHIVE_AUDIT_EVENTS)))
          .build();
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuditStub extends io.grpc.stub.AbstractStub<AuditStub> {
    private AuditStub(io.grpc.Channel channel) {
      super(channel);
    }

    private AuditStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new AuditStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get the service version.
     * </pre>
     */
    public void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetVersionMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create a new standalone audit event.
     * </pre>
     */
    public void createAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAuditEventMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create a new attempt audit event. This call is normally followed by a
     * call to UpdateAttemptAuditEventWithResult.
     * </pre>
     */
    public void createAttemptAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAttemptAuditEventMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Update an existing attempt audit event with result data.
     * </pre>
     */
    public void updateAttemptAuditEventWithResult(com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateAttemptAuditEventWithResultMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List audit events.
     * </pre>
     */
    public void listEvents(com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListEventsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Configure the audit service for archiving audit logs.
     * </pre>
     */
    public void configureArchiving(com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getConfigureArchivingMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Retrieve the current archiving configuration.
     * </pre>
     */
    public void getArchivingConfig(com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetArchivingConfigMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Archive audit events.
     * </pre>
     */
    public void archiveAuditEvents(com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getArchiveAuditEventsMethodHelper(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuditBlockingStub extends io.grpc.stub.AbstractStub<AuditBlockingStub> {
    private AuditBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private AuditBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new AuditBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get the service version.
     * </pre>
     */
    public com.cloudera.thunderhead.service.common.version.Version.VersionResponse getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetVersionMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a new standalone audit event.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse createAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateAuditEventMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a new attempt audit event. This call is normally followed by a
     * call to UpdateAttemptAuditEventWithResult.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse createAttemptAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateAttemptAuditEventMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update an existing attempt audit event with result data.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse updateAttemptAuditEventWithResult(com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest request) {
      return blockingUnaryCall(
          getChannel(), getUpdateAttemptAuditEventWithResultMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List audit events.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse listEvents(com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListEventsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Configure the audit service for archiving audit logs.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse configureArchiving(com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest request) {
      return blockingUnaryCall(
          getChannel(), getConfigureArchivingMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Retrieve the current archiving configuration.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse getArchivingConfig(com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetArchivingConfigMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Archive audit events.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse archiveAuditEvents(com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest request) {
      return blockingUnaryCall(
          getChannel(), getArchiveAuditEventsMethodHelper(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuditFutureStub extends io.grpc.stub.AbstractStub<AuditFutureStub> {
    private AuditFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private AuditFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new AuditFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get the service version.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> getVersion(
        com.cloudera.thunderhead.service.common.version.Version.VersionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetVersionMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create a new standalone audit event.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> createAuditEvent(
        com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAuditEventMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create a new attempt audit event. This call is normally followed by a
     * call to UpdateAttemptAuditEventWithResult.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> createAttemptAuditEvent(
        com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAttemptAuditEventMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Update an existing attempt audit event with result data.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> updateAttemptAuditEventWithResult(
        com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateAttemptAuditEventWithResultMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List audit events.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> listEvents(
        com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListEventsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Configure the audit service for archiving audit logs.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> configureArchiving(
        com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getConfigureArchivingMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Retrieve the current archiving configuration.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> getArchivingConfig(
        com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetArchivingConfigMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Archive audit events.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> archiveAuditEvents(
        com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getArchiveAuditEventsMethodHelper(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_CREATE_AUDIT_EVENT = 1;
  private static final int METHODID_CREATE_ATTEMPT_AUDIT_EVENT = 2;
  private static final int METHODID_UPDATE_ATTEMPT_AUDIT_EVENT_WITH_RESULT = 3;
  private static final int METHODID_LIST_EVENTS = 4;
  private static final int METHODID_CONFIGURE_ARCHIVING = 5;
  private static final int METHODID_GET_ARCHIVING_CONFIG = 6;
  private static final int METHODID_ARCHIVE_AUDIT_EVENTS = 7;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AuditImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(AuditImplBase serviceImpl, int methodId) {
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
        case METHODID_CREATE_AUDIT_EVENT:
          serviceImpl.createAuditEvent((com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse>) responseObserver);
          break;
        case METHODID_CREATE_ATTEMPT_AUDIT_EVENT:
          serviceImpl.createAttemptAuditEvent((com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse>) responseObserver);
          break;
        case METHODID_UPDATE_ATTEMPT_AUDIT_EVENT_WITH_RESULT:
          serviceImpl.updateAttemptAuditEventWithResult((com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse>) responseObserver);
          break;
        case METHODID_LIST_EVENTS:
          serviceImpl.listEvents((com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse>) responseObserver);
          break;
        case METHODID_CONFIGURE_ARCHIVING:
          serviceImpl.configureArchiving((com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse>) responseObserver);
          break;
        case METHODID_GET_ARCHIVING_CONFIG:
          serviceImpl.getArchivingConfig((com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse>) responseObserver);
          break;
        case METHODID_ARCHIVE_AUDIT_EVENTS:
          serviceImpl.archiveAuditEvents((com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse>) responseObserver);
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

  private static abstract class AuditBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AuditBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.audit.AuditProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Audit");
    }
  }

  private static final class AuditFileDescriptorSupplier
      extends AuditBaseDescriptorSupplier {
    AuditFileDescriptorSupplier() {}
  }

  private static final class AuditMethodDescriptorSupplier
      extends AuditBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    AuditMethodDescriptorSupplier(String methodName) {
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
      synchronized (AuditGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AuditFileDescriptorSupplier())
              .addMethod(getGetVersionMethodHelper())
              .addMethod(getCreateAuditEventMethodHelper())
              .addMethod(getCreateAttemptAuditEventMethodHelper())
              .addMethod(getUpdateAttemptAuditEventWithResultMethodHelper())
              .addMethod(getListEventsMethodHelper())
              .addMethod(getConfigureArchivingMethodHelper())
              .addMethod(getGetArchivingConfigMethodHelper())
              .addMethod(getArchiveAuditEventsMethodHelper())
              .build();
        }
      }
    }
    return result;
  }
}
