package com.cloudera.thunderhead.service.idbrokermappingmanagement;

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
 * Protocol for ID Broker Mapping Management Service. This service runs in the
 * CDP control plane. It receives requests to get and set ID Broker mappings
 * from the CDP Environments API Service, and from backend services that need
 * access to the mappings (for example, the Datalake Management Service).
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.12.0)",
    comments = "Source: idbrokermappingmanagement.proto")
public final class IdBrokerMappingManagementGrpc {

  private IdBrokerMappingManagementGrpc() {}

  public static final String SERVICE_NAME = "idbrokermappingmanagement.IdBrokerMappingManagement";

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
    if ((getGetVersionMethod = IdBrokerMappingManagementGrpc.getGetVersionMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getGetVersionMethod = IdBrokerMappingManagementGrpc.getGetVersionMethod) == null) {
          IdBrokerMappingManagementGrpc.getGetVersionMethod = getGetVersionMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "idbrokermappingmanagement.IdBrokerMappingManagement", "GetVersion"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetMappingsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> METHOD_GET_MAPPINGS = getGetMappingsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> getGetMappingsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> getGetMappingsMethod() {
    return getGetMappingsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> getGetMappingsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> getGetMappingsMethod;
    if ((getGetMappingsMethod = IdBrokerMappingManagementGrpc.getGetMappingsMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getGetMappingsMethod = IdBrokerMappingManagementGrpc.getGetMappingsMethod) == null) {
          IdBrokerMappingManagementGrpc.getGetMappingsMethod = getGetMappingsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "idbrokermappingmanagement.IdBrokerMappingManagement", "GetMappings"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getSetMappingsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> METHOD_SET_MAPPINGS = getSetMappingsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> getSetMappingsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> getSetMappingsMethod() {
    return getSetMappingsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> getSetMappingsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> getSetMappingsMethod;
    if ((getSetMappingsMethod = IdBrokerMappingManagementGrpc.getSetMappingsMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getSetMappingsMethod = IdBrokerMappingManagementGrpc.getSetMappingsMethod) == null) {
          IdBrokerMappingManagementGrpc.getSetMappingsMethod = getSetMappingsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "idbrokermappingmanagement.IdBrokerMappingManagement", "SetMappings"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteMappingsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> METHOD_DELETE_MAPPINGS = getDeleteMappingsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> getDeleteMappingsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> getDeleteMappingsMethod() {
    return getDeleteMappingsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> getDeleteMappingsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> getDeleteMappingsMethod;
    if ((getDeleteMappingsMethod = IdBrokerMappingManagementGrpc.getDeleteMappingsMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getDeleteMappingsMethod = IdBrokerMappingManagementGrpc.getDeleteMappingsMethod) == null) {
          IdBrokerMappingManagementGrpc.getDeleteMappingsMethod = getDeleteMappingsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "idbrokermappingmanagement.IdBrokerMappingManagement", "DeleteMappings"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getSyncMappingsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> METHOD_SYNC_MAPPINGS = getSyncMappingsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> getSyncMappingsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> getSyncMappingsMethod() {
    return getSyncMappingsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> getSyncMappingsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> getSyncMappingsMethod;
    if ((getSyncMappingsMethod = IdBrokerMappingManagementGrpc.getSyncMappingsMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getSyncMappingsMethod = IdBrokerMappingManagementGrpc.getSyncMappingsMethod) == null) {
          IdBrokerMappingManagementGrpc.getSyncMappingsMethod = getSyncMappingsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "idbrokermappingmanagement.IdBrokerMappingManagement", "SyncMappings"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetMappingsSyncStatusMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> METHOD_GET_MAPPINGS_SYNC_STATUS = getGetMappingsSyncStatusMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> getGetMappingsSyncStatusMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> getGetMappingsSyncStatusMethod() {
    return getGetMappingsSyncStatusMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> getGetMappingsSyncStatusMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> getGetMappingsSyncStatusMethod;
    if ((getGetMappingsSyncStatusMethod = IdBrokerMappingManagementGrpc.getGetMappingsSyncStatusMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getGetMappingsSyncStatusMethod = IdBrokerMappingManagementGrpc.getGetMappingsSyncStatusMethod) == null) {
          IdBrokerMappingManagementGrpc.getGetMappingsSyncStatusMethod = getGetMappingsSyncStatusMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "idbrokermappingmanagement.IdBrokerMappingManagement", "GetMappingsSyncStatus"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetMappingsConfigMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> METHOD_GET_MAPPINGS_CONFIG = getGetMappingsConfigMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> getGetMappingsConfigMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> getGetMappingsConfigMethod() {
    return getGetMappingsConfigMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest,
      com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> getGetMappingsConfigMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> getGetMappingsConfigMethod;
    if ((getGetMappingsConfigMethod = IdBrokerMappingManagementGrpc.getGetMappingsConfigMethod) == null) {
      synchronized (IdBrokerMappingManagementGrpc.class) {
        if ((getGetMappingsConfigMethod = IdBrokerMappingManagementGrpc.getGetMappingsConfigMethod) == null) {
          IdBrokerMappingManagementGrpc.getGetMappingsConfigMethod = getGetMappingsConfigMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest, com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "idbrokermappingmanagement.IdBrokerMappingManagement", "GetMappingsConfig"))
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
    return new IdBrokerMappingManagementStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static IdBrokerMappingManagementBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new IdBrokerMappingManagementBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static IdBrokerMappingManagementFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new IdBrokerMappingManagementFutureStub(channel);
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
      asyncUnimplementedUnaryCall(getGetVersionMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Get all ID Broker mappings for an environment.
     * </pre>
     */
    public void getMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetMappingsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Set all ID Broker mappings for an environment. WARNING: overwrites all
     * existing mapping state, including the dataAccessRole and the baselineRole.
     * </pre>
     */
    public void setMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSetMappingsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Deletes all ID Broker mappings for an environment.
     * </pre>
     */
    public void deleteMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteMappingsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Sync ID Broker mappings for an environment to all associated datalake clusters.
     * </pre>
     */
    public void syncMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSyncMappingsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Get the status of an ID Broker mapping sync attempt.
     * </pre>
     */
    public void getMappingsSyncStatus(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetMappingsSyncStatusMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Get all ID Broker mappings for an environment in a form that matches
     * ID Broker's configuration model.
     * </pre>
     */
    public void getMappingsConfig(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetMappingsConfigMethodHelper(), responseObserver);
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
            getGetMappingsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest,
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse>(
                  this, METHODID_GET_MAPPINGS)))
          .addMethod(
            getSetMappingsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest,
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse>(
                  this, METHODID_SET_MAPPINGS)))
          .addMethod(
            getDeleteMappingsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest,
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse>(
                  this, METHODID_DELETE_MAPPINGS)))
          .addMethod(
            getSyncMappingsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest,
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse>(
                  this, METHODID_SYNC_MAPPINGS)))
          .addMethod(
            getGetMappingsSyncStatusMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest,
                com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse>(
                  this, METHODID_GET_MAPPINGS_SYNC_STATUS)))
          .addMethod(
            getGetMappingsConfigMethodHelper(),
            asyncUnaryCall(
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
  public static final class IdBrokerMappingManagementStub extends io.grpc.stub.AbstractStub<IdBrokerMappingManagementStub> {
    private IdBrokerMappingManagementStub(io.grpc.Channel channel) {
      super(channel);
    }

    private IdBrokerMappingManagementStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected IdBrokerMappingManagementStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new IdBrokerMappingManagementStub(channel, callOptions);
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
     * Get all ID Broker mappings for an environment.
     * </pre>
     */
    public void getMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetMappingsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set all ID Broker mappings for an environment. WARNING: overwrites all
     * existing mapping state, including the dataAccessRole and the baselineRole.
     * </pre>
     */
    public void setMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetMappingsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes all ID Broker mappings for an environment.
     * </pre>
     */
    public void deleteMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteMappingsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Sync ID Broker mappings for an environment to all associated datalake clusters.
     * </pre>
     */
    public void syncMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSyncMappingsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the status of an ID Broker mapping sync attempt.
     * </pre>
     */
    public void getMappingsSyncStatus(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetMappingsSyncStatusMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get all ID Broker mappings for an environment in a form that matches
     * ID Broker's configuration model.
     * </pre>
     */
    public void getMappingsConfig(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetMappingsConfigMethodHelper(), getCallOptions()), request, responseObserver);
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
  public static final class IdBrokerMappingManagementBlockingStub extends io.grpc.stub.AbstractStub<IdBrokerMappingManagementBlockingStub> {
    private IdBrokerMappingManagementBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private IdBrokerMappingManagementBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected IdBrokerMappingManagementBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new IdBrokerMappingManagementBlockingStub(channel, callOptions);
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
     * Get all ID Broker mappings for an environment.
     * </pre>
     */
    public com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse getMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetMappingsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set all ID Broker mappings for an environment. WARNING: overwrites all
     * existing mapping state, including the dataAccessRole and the baselineRole.
     * </pre>
     */
    public com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse setMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest request) {
      return blockingUnaryCall(
          getChannel(), getSetMappingsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes all ID Broker mappings for an environment.
     * </pre>
     */
    public com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse deleteMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteMappingsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Sync ID Broker mappings for an environment to all associated datalake clusters.
     * </pre>
     */
    public com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse syncMappings(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest request) {
      return blockingUnaryCall(
          getChannel(), getSyncMappingsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the status of an ID Broker mapping sync attempt.
     * </pre>
     */
    public com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse getMappingsSyncStatus(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetMappingsSyncStatusMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get all ID Broker mappings for an environment in a form that matches
     * ID Broker's configuration model.
     * </pre>
     */
    public com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse getMappingsConfig(com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetMappingsConfigMethodHelper(), getCallOptions(), request);
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
  public static final class IdBrokerMappingManagementFutureStub extends io.grpc.stub.AbstractStub<IdBrokerMappingManagementFutureStub> {
    private IdBrokerMappingManagementFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private IdBrokerMappingManagementFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected IdBrokerMappingManagementFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new IdBrokerMappingManagementFutureStub(channel, callOptions);
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
     * Get all ID Broker mappings for an environment.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse> getMappings(
        com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetMappingsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set all ID Broker mappings for an environment. WARNING: overwrites all
     * existing mapping state, including the dataAccessRole and the baselineRole.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse> setMappings(
        com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSetMappingsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes all ID Broker mappings for an environment.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse> deleteMappings(
        com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteMappingsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Sync ID Broker mappings for an environment to all associated datalake clusters.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsResponse> syncMappings(
        com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SyncMappingsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSyncMappingsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the status of an ID Broker mapping sync attempt.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusResponse> getMappingsSyncStatus(
        com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsSyncStatusRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetMappingsSyncStatusMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get all ID Broker mappings for an environment in a form that matches
     * ID Broker's configuration model.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse> getMappingsConfig(
        com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetMappingsConfigMethodHelper(), getCallOptions()), request);
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
              .addMethod(getGetVersionMethodHelper())
              .addMethod(getGetMappingsMethodHelper())
              .addMethod(getSetMappingsMethodHelper())
              .addMethod(getDeleteMappingsMethodHelper())
              .addMethod(getSyncMappingsMethodHelper())
              .addMethod(getGetMappingsSyncStatusMethodHelper())
              .addMethod(getGetMappingsConfigMethodHelper())
              .build();
        }
      }
    }
    return result;
  }
}
