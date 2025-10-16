package com.cloudera.thunderhead.service.audit;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class AuditGrpc {

  private AuditGrpc() {}

  public static final java.lang.String SERVICE_NAME = "audit.Audit";

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
    if ((getGetVersionMethod = AuditGrpc.getGetVersionMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getGetVersionMethod = AuditGrpc.getGetVersionMethod) == null) {
          AuditGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> getCreateAuditEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateAuditEvent",
      requestType = com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest.class,
      responseType = com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> getCreateAuditEventMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest, com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> getCreateAuditEventMethod;
    if ((getCreateAuditEventMethod = AuditGrpc.getCreateAuditEventMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getCreateAuditEventMethod = AuditGrpc.getCreateAuditEventMethod) == null) {
          AuditGrpc.getCreateAuditEventMethod = getCreateAuditEventMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest, com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateAuditEvent"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> getCreateAttemptAuditEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateAttemptAuditEvent",
      requestType = com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest.class,
      responseType = com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> getCreateAttemptAuditEventMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest, com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> getCreateAttemptAuditEventMethod;
    if ((getCreateAttemptAuditEventMethod = AuditGrpc.getCreateAttemptAuditEventMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getCreateAttemptAuditEventMethod = AuditGrpc.getCreateAttemptAuditEventMethod) == null) {
          AuditGrpc.getCreateAttemptAuditEventMethod = getCreateAttemptAuditEventMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest, com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateAttemptAuditEvent"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> getUpdateAttemptAuditEventWithResultMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateAttemptAuditEventWithResult",
      requestType = com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest.class,
      responseType = com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> getUpdateAttemptAuditEventWithResultMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest, com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> getUpdateAttemptAuditEventWithResultMethod;
    if ((getUpdateAttemptAuditEventWithResultMethod = AuditGrpc.getUpdateAttemptAuditEventWithResultMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getUpdateAttemptAuditEventWithResultMethod = AuditGrpc.getUpdateAttemptAuditEventWithResultMethod) == null) {
          AuditGrpc.getUpdateAttemptAuditEventWithResultMethod = getUpdateAttemptAuditEventWithResultMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest, com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateAttemptAuditEventWithResult"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> getListEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListEvents",
      requestType = com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest.class,
      responseType = com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> getListEventsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest, com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> getListEventsMethod;
    if ((getListEventsMethod = AuditGrpc.getListEventsMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getListEventsMethod = AuditGrpc.getListEventsMethod) == null) {
          AuditGrpc.getListEventsMethod = getListEventsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest, com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListEvents"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> getConfigureArchivingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ConfigureArchiving",
      requestType = com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest.class,
      responseType = com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> getConfigureArchivingMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest, com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> getConfigureArchivingMethod;
    if ((getConfigureArchivingMethod = AuditGrpc.getConfigureArchivingMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getConfigureArchivingMethod = AuditGrpc.getConfigureArchivingMethod) == null) {
          AuditGrpc.getConfigureArchivingMethod = getConfigureArchivingMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest, com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ConfigureArchiving"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> getGetArchivingConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetArchivingConfig",
      requestType = com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest.class,
      responseType = com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> getGetArchivingConfigMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest, com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> getGetArchivingConfigMethod;
    if ((getGetArchivingConfigMethod = AuditGrpc.getGetArchivingConfigMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getGetArchivingConfigMethod = AuditGrpc.getGetArchivingConfigMethod) == null) {
          AuditGrpc.getGetArchivingConfigMethod = getGetArchivingConfigMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest, com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetArchivingConfig"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> getArchiveAuditEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ArchiveAuditEvents",
      requestType = com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest.class,
      responseType = com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest,
      com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> getArchiveAuditEventsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest, com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> getArchiveAuditEventsMethod;
    if ((getArchiveAuditEventsMethod = AuditGrpc.getArchiveAuditEventsMethod) == null) {
      synchronized (AuditGrpc.class) {
        if ((getArchiveAuditEventsMethod = AuditGrpc.getArchiveAuditEventsMethod) == null) {
          AuditGrpc.getArchiveAuditEventsMethod = getArchiveAuditEventsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest, com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ArchiveAuditEvents"))
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
    io.grpc.stub.AbstractStub.StubFactory<AuditStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditStub>() {
        @java.lang.Override
        public AuditStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditStub(channel, callOptions);
        }
      };
    return AuditStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static AuditBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuditBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditBlockingV2Stub>() {
        @java.lang.Override
        public AuditBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditBlockingV2Stub(channel, callOptions);
        }
      };
    return AuditBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AuditBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuditBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditBlockingStub>() {
        @java.lang.Override
        public AuditBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditBlockingStub(channel, callOptions);
        }
      };
    return AuditBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AuditFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuditFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditFutureStub>() {
        @java.lang.Override
        public AuditFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditFutureStub(channel, callOptions);
        }
      };
    return AuditFutureStub.newStub(factory, channel);
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
     * Get the service version.
     * </pre>
     */
    default void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetVersionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create a new standalone audit event.
     * </pre>
     */
    default void createAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateAuditEventMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create a new attempt audit event. This call is normally followed by a
     * call to UpdateAttemptAuditEventWithResult.
     * </pre>
     */
    default void createAttemptAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateAttemptAuditEventMethod(), responseObserver);
    }

    /**
     * <pre>
     * Update an existing attempt audit event with result data.
     * </pre>
     */
    default void updateAttemptAuditEventWithResult(com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateAttemptAuditEventWithResultMethod(), responseObserver);
    }

    /**
     * <pre>
     * List audit events.
     * </pre>
     */
    default void listEvents(com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListEventsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Configure the audit service for archiving audit logs.
     * </pre>
     */
    default void configureArchiving(com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getConfigureArchivingMethod(), responseObserver);
    }

    /**
     * <pre>
     * Retrieve the current archiving configuration.
     * </pre>
     */
    default void getArchivingConfig(com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetArchivingConfigMethod(), responseObserver);
    }

    /**
     * <pre>
     * Archive audit events.
     * </pre>
     */
    default void archiveAuditEvents(com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getArchiveAuditEventsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service Audit.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class AuditImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AuditGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service Audit.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuditStub
      extends io.grpc.stub.AbstractAsyncStub<AuditStub> {
    private AuditStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditStub(channel, callOptions);
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
     * Create a new standalone audit event.
     * </pre>
     */
    public void createAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateAuditEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create a new attempt audit event. This call is normally followed by a
     * call to UpdateAttemptAuditEventWithResult.
     * </pre>
     */
    public void createAttemptAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateAttemptAuditEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Update an existing attempt audit event with result data.
     * </pre>
     */
    public void updateAttemptAuditEventWithResult(com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateAttemptAuditEventWithResultMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List audit events.
     * </pre>
     */
    public void listEvents(com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListEventsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Configure the audit service for archiving audit logs.
     * </pre>
     */
    public void configureArchiving(com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getConfigureArchivingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Retrieve the current archiving configuration.
     * </pre>
     */
    public void getArchivingConfig(com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetArchivingConfigMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Archive audit events.
     * </pre>
     */
    public void archiveAuditEvents(com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getArchiveAuditEventsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service Audit.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuditBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<AuditBlockingV2Stub> {
    private AuditBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditBlockingV2Stub(channel, callOptions);
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
     * <pre>
     * Create a new standalone audit event.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse createAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateAuditEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a new attempt audit event. This call is normally followed by a
     * call to UpdateAttemptAuditEventWithResult.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse createAttemptAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateAttemptAuditEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update an existing attempt audit event with result data.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse updateAttemptAuditEventWithResult(com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUpdateAttemptAuditEventWithResultMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List audit events.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse listEvents(com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListEventsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Configure the audit service for archiving audit logs.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse configureArchiving(com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getConfigureArchivingMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Retrieve the current archiving configuration.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse getArchivingConfig(com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetArchivingConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Archive audit events.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse archiveAuditEvents(com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getArchiveAuditEventsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service Audit.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuditBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AuditBlockingStub> {
    private AuditBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditBlockingStub(channel, callOptions);
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
     * Create a new standalone audit event.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse createAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateAuditEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a new attempt audit event. This call is normally followed by a
     * call to UpdateAttemptAuditEventWithResult.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse createAttemptAuditEvent(com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateAttemptAuditEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update an existing attempt audit event with result data.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse updateAttemptAuditEventWithResult(com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateAttemptAuditEventWithResultMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List audit events.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse listEvents(com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListEventsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Configure the audit service for archiving audit logs.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse configureArchiving(com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getConfigureArchivingMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Retrieve the current archiving configuration.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse getArchivingConfig(com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetArchivingConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Archive audit events.
     * </pre>
     */
    public com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse archiveAuditEvents(com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getArchiveAuditEventsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service Audit.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuditFutureStub
      extends io.grpc.stub.AbstractFutureStub<AuditFutureStub> {
    private AuditFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditFutureStub(channel, callOptions);
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
     * Create a new standalone audit event.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse> createAuditEvent(
        com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateAuditEventMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create a new attempt audit event. This call is normally followed by a
     * call to UpdateAttemptAuditEventWithResult.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse> createAttemptAuditEvent(
        com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateAttemptAuditEventMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Update an existing attempt audit event with result data.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse> updateAttemptAuditEventWithResult(
        com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateAttemptAuditEventWithResultMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List audit events.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse> listEvents(
        com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListEventsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Configure the audit service for archiving audit logs.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse> configureArchiving(
        com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getConfigureArchivingMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Retrieve the current archiving configuration.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse> getArchivingConfig(
        com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetArchivingConfigMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Archive audit events.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse> archiveAuditEvents(
        com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getArchiveAuditEventsMethod(), getCallOptions()), request);
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
          getCreateAuditEventMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventRequest,
              com.cloudera.thunderhead.service.audit.AuditProto.CreateAuditEventResponse>(
                service, METHODID_CREATE_AUDIT_EVENT)))
        .addMethod(
          getCreateAttemptAuditEventMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventRequest,
              com.cloudera.thunderhead.service.audit.AuditProto.CreateAttemptAuditEventResponse>(
                service, METHODID_CREATE_ATTEMPT_AUDIT_EVENT)))
        .addMethod(
          getUpdateAttemptAuditEventWithResultMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultRequest,
              com.cloudera.thunderhead.service.audit.AuditProto.UpdateAttemptAuditEventWithResultResponse>(
                service, METHODID_UPDATE_ATTEMPT_AUDIT_EVENT_WITH_RESULT)))
        .addMethod(
          getListEventsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.audit.AuditProto.ListEventsRequest,
              com.cloudera.thunderhead.service.audit.AuditProto.ListEventsResponse>(
                service, METHODID_LIST_EVENTS)))
        .addMethod(
          getConfigureArchivingMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingRequest,
              com.cloudera.thunderhead.service.audit.AuditProto.ConfigureArchivingResponse>(
                service, METHODID_CONFIGURE_ARCHIVING)))
        .addMethod(
          getGetArchivingConfigMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigRequest,
              com.cloudera.thunderhead.service.audit.AuditProto.GetArchivingConfigResponse>(
                service, METHODID_GET_ARCHIVING_CONFIG)))
        .addMethod(
          getArchiveAuditEventsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsRequest,
              com.cloudera.thunderhead.service.audit.AuditProto.ArchiveAuditEventsResponse>(
                service, METHODID_ARCHIVE_AUDIT_EVENTS)))
        .build();
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
    private final java.lang.String methodName;

    AuditMethodDescriptorSupplier(java.lang.String methodName) {
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
              .addMethod(getGetVersionMethod())
              .addMethod(getCreateAuditEventMethod())
              .addMethod(getCreateAttemptAuditEventMethod())
              .addMethod(getUpdateAttemptAuditEventWithResultMethod())
              .addMethod(getListEventsMethod())
              .addMethod(getConfigureArchivingMethod())
              .addMethod(getGetArchivingConfigMethod())
              .addMethod(getArchiveAuditEventsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
