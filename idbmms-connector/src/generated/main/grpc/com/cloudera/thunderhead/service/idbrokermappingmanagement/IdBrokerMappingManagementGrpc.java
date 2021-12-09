package com.cloudera.thunderhead.service.idbrokermappingmanagement;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Protocol for ID Broker Mapping Management Service. This service runs in the
 * CDP control plane. It receives requests to get and set ID Broker mappings
 * from the CDP Environments API Service, and from backend services that need
 * access to the mappings (for example, the Datalake Management Service).
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.42.1)",
    comments = "Source: idbrokermappingmanagement.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class IdBrokerMappingManagementGrpc {

  private IdBrokerMappingManagementGrpc() {}

  public static final String SERVICE_NAME = "idbrokermappingmanagement.IdBrokerMappingManagement";

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
    if ((getGetVersionMethod = IdBrokerMappingManagementGrpc.getGetVersionMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getGetVersionMethod = IdBrokerMappingManagementGrpc.getGetVersionMethod) == null) {
          IdBrokerMappingManagementGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new IdBrokerMappingManagementMethodDescriptorSupplier("GetVersion"))
              .build();
        }
      }
    }
    return getGetVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> getGetMappingsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetMappings",
      requestType = com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest.class,
      responseType = com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> getGetMappingsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> getGetMappingsMethod;
    if ((getGetMappingsMethod = IdBrokerMappingManagementGrpc.getGetMappingsMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getGetMappingsMethod = IdBrokerMappingManagementGrpc.getGetMappingsMethod) == null) {
          IdBrokerMappingManagementGrpc.getGetMappingsMethod = getGetMappingsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetMappings"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new IdBrokerMappingManagementMethodDescriptorSupplier("GetMappings"))
              .build();
        }
      }
    }
    return getGetMappingsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> getSetMappingsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetMappings",
      requestType = com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest.class,
      responseType = com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> getSetMappingsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> getSetMappingsMethod;
    if ((getSetMappingsMethod = IdBrokerMappingManagementGrpc.getSetMappingsMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getSetMappingsMethod = IdBrokerMappingManagementGrpc.getSetMappingsMethod) == null) {
          IdBrokerMappingManagementGrpc.getSetMappingsMethod = getSetMappingsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetMappings"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new IdBrokerMappingManagementMethodDescriptorSupplier("SetMappings"))
              .build();
        }
      }
    }
    return getSetMappingsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> getDeleteMappingsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteMappings",
      requestType = com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest.class,
      responseType = com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> getDeleteMappingsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> getDeleteMappingsMethod;
    if ((getDeleteMappingsMethod = IdBrokerMappingManagementGrpc.getDeleteMappingsMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getDeleteMappingsMethod = IdBrokerMappingManagementGrpc.getDeleteMappingsMethod) == null) {
          IdBrokerMappingManagementGrpc.getDeleteMappingsMethod = getDeleteMappingsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteMappings"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new IdBrokerMappingManagementMethodDescriptorSupplier("DeleteMappings"))
              .build();
        }
      }
    }
    return getDeleteMappingsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> getSyncMappingsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SyncMappings",
      requestType = com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest.class,
      responseType = com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> getSyncMappingsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> getSyncMappingsMethod;
    if ((getSyncMappingsMethod = IdBrokerMappingManagementGrpc.getSyncMappingsMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getSyncMappingsMethod = IdBrokerMappingManagementGrpc.getSyncMappingsMethod) == null) {
          IdBrokerMappingManagementGrpc.getSyncMappingsMethod = getSyncMappingsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SyncMappings"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new IdBrokerMappingManagementMethodDescriptorSupplier("SyncMappings"))
              .build();
        }
      }
    }
    return getSyncMappingsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> getGetMappingsSyncStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetMappingsSyncStatus",
      requestType = com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest.class,
      responseType = com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> getGetMappingsSyncStatusMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> getGetMappingsSyncStatusMethod;
    if ((getGetMappingsSyncStatusMethod = IdBrokerMappingManagementGrpc.getGetMappingsSyncStatusMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getGetMappingsSyncStatusMethod = IdBrokerMappingManagementGrpc.getGetMappingsSyncStatusMethod) == null) {
          IdBrokerMappingManagementGrpc.getGetMappingsSyncStatusMethod = getGetMappingsSyncStatusMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetMappingsSyncStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new IdBrokerMappingManagementMethodDescriptorSupplier("GetMappingsSyncStatus"))
              .build();
        }
      }
    }
    return getGetMappingsSyncStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> getGetMappingsConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetMappingsConfig",
      requestType = com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest.class,
      responseType = com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> getGetMappingsConfigMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> getGetMappingsConfigMethod;
    if ((getGetMappingsConfigMethod = IdBrokerMappingManagementGrpc.getGetMappingsConfigMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getGetMappingsConfigMethod = IdBrokerMappingManagementGrpc.getGetMappingsConfigMethod) == null) {
          IdBrokerMappingManagementGrpc.getGetMappingsConfigMethod = getGetMappingsConfigMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetMappingsConfig"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse.getDefaultInstance()))
              .setSchemaDescriptor(new IdBrokerMappingManagementMethodDescriptorSupplier("GetMappingsConfig"))
              .build();
        }
      }
    }
    return getGetMappingsConfigMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static IdBrokerMappingManagementStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<IdBrokerMappingManagementStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<IdBrokerMappingManagementStub>() {
        @java.lang.Override
        public IdBrokerMappingManagementStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new IdBrokerMappingManagementStub(channel, callOptions);
        }
      };
    return IdBrokerMappingManagementStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static IdBrokerMappingManagementBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<IdBrokerMappingManagementBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<IdBrokerMappingManagementBlockingStub>() {
        @java.lang.Override
        public IdBrokerMappingManagementBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new IdBrokerMappingManagementBlockingStub(channel, callOptions);
        }
      };
    return IdBrokerMappingManagementBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static IdBrokerMappingManagementFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<IdBrokerMappingManagementFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<IdBrokerMappingManagementFutureStub>() {
        @java.lang.Override
        public IdBrokerMappingManagementFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new IdBrokerMappingManagementFutureStub(channel, callOptions);
        }
      };
    return IdBrokerMappingManagementFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Protocol for ID Broker Mapping Management Service. This service runs in the
   * CDP control plane. It receives requests to get and set ID Broker mappings
   * from the CDP Environments API Service, and from backend services that need
   * access to the mappings (for example, the Datalake Management Service).
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class IdBrokerMappingManagementImplBase implements io.grpc.BindableService {

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
     * Get all ID Broker mappings for an environment.
     * </pre>
     */
    public void getMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMappingsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Set all ID Broker mappings for an environment. WARNING: overwrites all
     * existing mapping state, including the dataAccessRole and the baselineRole.
     * </pre>
     */
    public void setMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetMappingsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Deletes all ID Broker mappings for an environment.
     * </pre>
     */
    public void deleteMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteMappingsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Sync ID Broker mappings for an environment to all associated datalake clusters.
     * </pre>
     */
    public void syncMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSyncMappingsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the status of an ID Broker mapping sync attempt.
     * </pre>
     */
    public void getMappingsSyncStatus(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMappingsSyncStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get all ID Broker mappings for an environment in a form that matches
     * ID Broker's configuration model.
     * </pre>
     */
    public void getMappingsConfig(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMappingsConfigMethod(), responseObserver);
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
            getGetMappingsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest,
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse>(
                  this, METHODID_GET_MAPPINGS)))
          .addMethod(
            getSetMappingsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest,
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse>(
                  this, METHODID_SET_MAPPINGS)))
          .addMethod(
            getDeleteMappingsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest,
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse>(
                  this, METHODID_DELETE_MAPPINGS)))
          .addMethod(
            getSyncMappingsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest,
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse>(
                  this, METHODID_SYNC_MAPPINGS)))
          .addMethod(
            getGetMappingsSyncStatusMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest,
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse>(
                  this, METHODID_GET_MAPPINGS_SYNC_STATUS)))
          .addMethod(
            getGetMappingsConfigMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest,
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse>(
                  this, METHODID_GET_MAPPINGS_CONFIG)))
          .build();
    }
  }

  /**
   * <pre>
   * Protocol for ID Broker Mapping Management Service. This service runs in the
   * CDP control plane. It receives requests to get and set ID Broker mappings
   * from the CDP Environments API Service, and from backend services that need
   * access to the mappings (for example, the Datalake Management Service).
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class IdBrokerMappingManagementStub extends io.grpc.stub.AbstractAsyncStub<IdBrokerMappingManagementStub> {
    private IdBrokerMappingManagementStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected IdBrokerMappingManagementStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new IdBrokerMappingManagementStub(channel, callOptions);
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
     * Get all ID Broker mappings for an environment.
     * </pre>
     */
    public void getMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMappingsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set all ID Broker mappings for an environment. WARNING: overwrites all
     * existing mapping state, including the dataAccessRole and the baselineRole.
     * </pre>
     */
    public void setMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetMappingsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes all ID Broker mappings for an environment.
     * </pre>
     */
    public void deleteMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteMappingsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Sync ID Broker mappings for an environment to all associated datalake clusters.
     * </pre>
     */
    public void syncMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSyncMappingsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the status of an ID Broker mapping sync attempt.
     * </pre>
     */
    public void getMappingsSyncStatus(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMappingsSyncStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get all ID Broker mappings for an environment in a form that matches
     * ID Broker's configuration model.
     * </pre>
     */
    public void getMappingsConfig(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMappingsConfigMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Protocol for ID Broker Mapping Management Service. This service runs in the
   * CDP control plane. It receives requests to get and set ID Broker mappings
   * from the CDP Environments API Service, and from backend services that need
   * access to the mappings (for example, the Datalake Management Service).
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class IdBrokerMappingManagementBlockingStub extends io.grpc.stub.AbstractBlockingStub<IdBrokerMappingManagementBlockingStub> {
    private IdBrokerMappingManagementBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected IdBrokerMappingManagementBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new IdBrokerMappingManagementBlockingStub(channel, callOptions);
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
     * Get all ID Broker mappings for an environment.
     * </pre>
     */
    public com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse getMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMappingsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set all ID Broker mappings for an environment. WARNING: overwrites all
     * existing mapping state, including the dataAccessRole and the baselineRole.
     * </pre>
     */
    public com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse setMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetMappingsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes all ID Broker mappings for an environment.
     * </pre>
     */
    public com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse deleteMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteMappingsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Sync ID Broker mappings for an environment to all associated datalake clusters.
     * </pre>
     */
    public com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse syncMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSyncMappingsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the status of an ID Broker mapping sync attempt.
     * </pre>
     */
    public com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse getMappingsSyncStatus(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMappingsSyncStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get all ID Broker mappings for an environment in a form that matches
     * ID Broker's configuration model.
     * </pre>
     */
    public com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse getMappingsConfig(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMappingsConfigMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Protocol for ID Broker Mapping Management Service. This service runs in the
   * CDP control plane. It receives requests to get and set ID Broker mappings
   * from the CDP Environments API Service, and from backend services that need
   * access to the mappings (for example, the Datalake Management Service).
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class IdBrokerMappingManagementFutureStub extends io.grpc.stub.AbstractFutureStub<IdBrokerMappingManagementFutureStub> {
    private IdBrokerMappingManagementFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected IdBrokerMappingManagementFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new IdBrokerMappingManagementFutureStub(channel, callOptions);
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
     * Get all ID Broker mappings for an environment.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> getMappings(
        com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMappingsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set all ID Broker mappings for an environment. WARNING: overwrites all
     * existing mapping state, including the dataAccessRole and the baselineRole.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> setMappings(
        com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetMappingsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes all ID Broker mappings for an environment.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> deleteMappings(
        com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteMappingsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Sync ID Broker mappings for an environment to all associated datalake clusters.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> syncMappings(
        com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSyncMappingsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the status of an ID Broker mapping sync attempt.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> getMappingsSyncStatus(
        com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMappingsSyncStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get all ID Broker mappings for an environment in a form that matches
     * ID Broker's configuration model.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> getMappingsConfig(
        com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMappingsConfigMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_GET_MAPPINGS = 1;
  private static final int METHODID_SET_MAPPINGS = 2;
  private static final int METHODID_DELETE_MAPPINGS = 3;
  private static final int METHODID_SYNC_MAPPINGS = 4;
  private static final int METHODID_GET_MAPPINGS_SYNC_STATUS = 5;
  private static final int METHODID_GET_MAPPINGS_CONFIG = 6;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final IdBrokerMappingManagementImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(IdBrokerMappingManagementImplBase serviceImpl, int methodId) {
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
        case METHODID_GET_MAPPINGS:
          serviceImpl.getMappings((com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse>) responseObserver);
          break;
        case METHODID_SET_MAPPINGS:
          serviceImpl.setMappings((com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse>) responseObserver);
          break;
        case METHODID_DELETE_MAPPINGS:
          serviceImpl.deleteMappings((com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse>) responseObserver);
          break;
        case METHODID_SYNC_MAPPINGS:
          serviceImpl.syncMappings((com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse>) responseObserver);
          break;
        case METHODID_GET_MAPPINGS_SYNC_STATUS:
          serviceImpl.getMappingsSyncStatus((com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse>) responseObserver);
          break;
        case METHODID_GET_MAPPINGS_CONFIG:
          serviceImpl.getMappingsConfig((com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse>) responseObserver);
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

  private static abstract class IdBrokerMappingManagementBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    IdBrokerMappingManagementBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("IdBrokerMappingManagement");
    }
  }

  private static final class IdBrokerMappingManagementFileDescriptorSupplier
      extends IdBrokerMappingManagementBaseDescriptorSupplier {
    IdBrokerMappingManagementFileDescriptorSupplier() {}
  }

  private static final class IdBrokerMappingManagementMethodDescriptorSupplier
      extends IdBrokerMappingManagementBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    IdBrokerMappingManagementMethodDescriptorSupplier(String methodName) {
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
      synchronized (IdBrokerMappingManagementGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new IdBrokerMappingManagementFileDescriptorSupplier())
              .addMethod(getGetVersionMethod())
              .addMethod(getGetMappingsMethod())
              .addMethod(getSetMappingsMethod())
              .addMethod(getDeleteMappingsMethod())
              .addMethod(getSyncMappingsMethod())
              .addMethod(getGetMappingsSyncStatusMethod())
              .addMethod(getGetMappingsConfigMethod())
              .build();
        }
      }
    }
    return result;
  }
}
