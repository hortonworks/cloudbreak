package com.cloudera.thunderhead.service.workloadiam;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class WorkloadIamGrpc {

  private WorkloadIamGrpc() {}

  public static final java.lang.String SERVICE_NAME = "workloadiam.WorkloadIam";

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
    if ((getGetVersionMethod = WorkloadIamGrpc.getGetVersionMethod) == null) {
      synchronized (WorkloadIamGrpc.class) {
        if ((getGetVersionMethod = WorkloadIamGrpc.getGetVersionMethod) == null) {
          WorkloadIamGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WorkloadIamMethodDescriptorSupplier("GetVersion"))
              .build();
        }
      }
    }
    return getGetVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse> getCreateSyncEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateSyncEvent",
      requestType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest.class,
      responseType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse> getCreateSyncEventMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse> getCreateSyncEventMethod;
    if ((getCreateSyncEventMethod = WorkloadIamGrpc.getCreateSyncEventMethod) == null) {
      synchronized (WorkloadIamGrpc.class) {
        if ((getCreateSyncEventMethod = WorkloadIamGrpc.getCreateSyncEventMethod) == null) {
          WorkloadIamGrpc.getCreateSyncEventMethod = getCreateSyncEventMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateSyncEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WorkloadIamMethodDescriptorSupplier("CreateSyncEvent"))
              .build();
        }
      }
    }
    return getCreateSyncEventMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse> getCommitSyncEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CommitSyncEvent",
      requestType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest.class,
      responseType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse> getCommitSyncEventMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse> getCommitSyncEventMethod;
    if ((getCommitSyncEventMethod = WorkloadIamGrpc.getCommitSyncEventMethod) == null) {
      synchronized (WorkloadIamGrpc.class) {
        if ((getCommitSyncEventMethod = WorkloadIamGrpc.getCommitSyncEventMethod) == null) {
          WorkloadIamGrpc.getCommitSyncEventMethod = getCommitSyncEventMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CommitSyncEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WorkloadIamMethodDescriptorSupplier("CommitSyncEvent"))
              .build();
        }
      }
    }
    return getCommitSyncEventMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse> getGetAutomatedSyncEnvironmentStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAutomatedSyncEnvironmentStatus",
      requestType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest.class,
      responseType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse> getGetAutomatedSyncEnvironmentStatusMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse> getGetAutomatedSyncEnvironmentStatusMethod;
    if ((getGetAutomatedSyncEnvironmentStatusMethod = WorkloadIamGrpc.getGetAutomatedSyncEnvironmentStatusMethod) == null) {
      synchronized (WorkloadIamGrpc.class) {
        if ((getGetAutomatedSyncEnvironmentStatusMethod = WorkloadIamGrpc.getGetAutomatedSyncEnvironmentStatusMethod) == null) {
          WorkloadIamGrpc.getGetAutomatedSyncEnvironmentStatusMethod = getGetAutomatedSyncEnvironmentStatusMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAutomatedSyncEnvironmentStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WorkloadIamMethodDescriptorSupplier("GetAutomatedSyncEnvironmentStatus"))
              .build();
        }
      }
    }
    return getGetAutomatedSyncEnvironmentStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse> getGetSyncEventStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetSyncEventStatus",
      requestType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest.class,
      responseType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse> getGetSyncEventStatusMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse> getGetSyncEventStatusMethod;
    if ((getGetSyncEventStatusMethod = WorkloadIamGrpc.getGetSyncEventStatusMethod) == null) {
      synchronized (WorkloadIamGrpc.class) {
        if ((getGetSyncEventStatusMethod = WorkloadIamGrpc.getGetSyncEventStatusMethod) == null) {
          WorkloadIamGrpc.getGetSyncEventStatusMethod = getGetSyncEventStatusMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetSyncEventStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WorkloadIamMethodDescriptorSupplier("GetSyncEventStatus"))
              .build();
        }
      }
    }
    return getGetSyncEventStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse> getLegacySyncUsersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "LegacySyncUsers",
      requestType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest.class,
      responseType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse> getLegacySyncUsersMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse> getLegacySyncUsersMethod;
    if ((getLegacySyncUsersMethod = WorkloadIamGrpc.getLegacySyncUsersMethod) == null) {
      synchronized (WorkloadIamGrpc.class) {
        if ((getLegacySyncUsersMethod = WorkloadIamGrpc.getLegacySyncUsersMethod) == null) {
          WorkloadIamGrpc.getLegacySyncUsersMethod = getLegacySyncUsersMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "LegacySyncUsers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WorkloadIamMethodDescriptorSupplier("LegacySyncUsers"))
              .build();
        }
      }
    }
    return getLegacySyncUsersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse> getLegacyGetSyncUsersStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "LegacyGetSyncUsersStatus",
      requestType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest.class,
      responseType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse> getLegacyGetSyncUsersStatusMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse> getLegacyGetSyncUsersStatusMethod;
    if ((getLegacyGetSyncUsersStatusMethod = WorkloadIamGrpc.getLegacyGetSyncUsersStatusMethod) == null) {
      synchronized (WorkloadIamGrpc.class) {
        if ((getLegacyGetSyncUsersStatusMethod = WorkloadIamGrpc.getLegacyGetSyncUsersStatusMethod) == null) {
          WorkloadIamGrpc.getLegacyGetSyncUsersStatusMethod = getLegacyGetSyncUsersStatusMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "LegacyGetSyncUsersStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WorkloadIamMethodDescriptorSupplier("LegacyGetSyncUsersStatus"))
              .build();
        }
      }
    }
    return getLegacyGetSyncUsersStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse> getLegacyGetEnvironmentUsersyncStateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "LegacyGetEnvironmentUsersyncState",
      requestType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest.class,
      responseType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse> getLegacyGetEnvironmentUsersyncStateMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse> getLegacyGetEnvironmentUsersyncStateMethod;
    if ((getLegacyGetEnvironmentUsersyncStateMethod = WorkloadIamGrpc.getLegacyGetEnvironmentUsersyncStateMethod) == null) {
      synchronized (WorkloadIamGrpc.class) {
        if ((getLegacyGetEnvironmentUsersyncStateMethod = WorkloadIamGrpc.getLegacyGetEnvironmentUsersyncStateMethod) == null) {
          WorkloadIamGrpc.getLegacyGetEnvironmentUsersyncStateMethod = getLegacyGetEnvironmentUsersyncStateMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "LegacyGetEnvironmentUsersyncState"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WorkloadIamMethodDescriptorSupplier("LegacyGetEnvironmentUsersyncState"))
              .build();
        }
      }
    }
    return getLegacyGetEnvironmentUsersyncStateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse> getSyncUsersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SyncUsers",
      requestType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest.class,
      responseType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse> getSyncUsersMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse> getSyncUsersMethod;
    if ((getSyncUsersMethod = WorkloadIamGrpc.getSyncUsersMethod) == null) {
      synchronized (WorkloadIamGrpc.class) {
        if ((getSyncUsersMethod = WorkloadIamGrpc.getSyncUsersMethod) == null) {
          WorkloadIamGrpc.getSyncUsersMethod = getSyncUsersMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SyncUsers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WorkloadIamMethodDescriptorSupplier("SyncUsers"))
              .build();
        }
      }
    }
    return getSyncUsersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse> getGetSyncUsersStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetSyncUsersStatus",
      requestType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest.class,
      responseType = com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest,
      com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse> getGetSyncUsersStatusMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse> getGetSyncUsersStatusMethod;
    if ((getGetSyncUsersStatusMethod = WorkloadIamGrpc.getGetSyncUsersStatusMethod) == null) {
      synchronized (WorkloadIamGrpc.class) {
        if ((getGetSyncUsersStatusMethod = WorkloadIamGrpc.getGetSyncUsersStatusMethod) == null) {
          WorkloadIamGrpc.getGetSyncUsersStatusMethod = getGetSyncUsersStatusMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest, com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetSyncUsersStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WorkloadIamMethodDescriptorSupplier("GetSyncUsersStatus"))
              .build();
        }
      }
    }
    return getGetSyncUsersStatusMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static WorkloadIamStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WorkloadIamStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WorkloadIamStub>() {
        @java.lang.Override
        public WorkloadIamStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WorkloadIamStub(channel, callOptions);
        }
      };
    return WorkloadIamStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static WorkloadIamBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WorkloadIamBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WorkloadIamBlockingV2Stub>() {
        @java.lang.Override
        public WorkloadIamBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WorkloadIamBlockingV2Stub(channel, callOptions);
        }
      };
    return WorkloadIamBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static WorkloadIamBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WorkloadIamBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WorkloadIamBlockingStub>() {
        @java.lang.Override
        public WorkloadIamBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WorkloadIamBlockingStub(channel, callOptions);
        }
      };
    return WorkloadIamBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static WorkloadIamFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WorkloadIamFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WorkloadIamFutureStub>() {
        @java.lang.Override
        public WorkloadIamFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WorkloadIamFutureStub(channel, callOptions);
        }
      };
    return WorkloadIamFutureStub.newStub(factory, channel);
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
     * Creates a new sync event. A sync event indicates that a state change is about to be made in
     * the control plane (usually in UMS). This service will persist the event and will asynchronously
     * sync the changes into the customer environment. In order to give time for the control plane
     * changes to go through before attempting a sync, a processing delay is included in the sync
     * event. To allow processing an event earlier, the CommitSyncEvent rpc should be called.
     * </pre>
     */
    default void createSyncEvent(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateSyncEventMethod(), responseObserver);
    }

    /**
     * <pre>
     * Update an existing sync event to indicate that it's ready for processing.
     * </pre>
     */
    default void commitSyncEvent(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCommitSyncEventMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the sync event state for an environment.
     * </pre>
     */
    default void getAutomatedSyncEnvironmentStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAutomatedSyncEnvironmentStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the status of a sync event.
     * </pre>
     */
    default void getSyncEventStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSyncEventStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     * Request a manual usersync. Specifying multiple environments is supported
     * for backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    default void legacySyncUsers(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLegacySyncUsersMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the status of a LegacySyncUsers request. This RPC is for supporting
     * backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    default void legacyGetSyncUsersStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLegacyGetSyncUsersStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the state and last usersync Crn for an environment. This RPC is for supporting
     * backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    default void legacyGetEnvironmentUsersyncState(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLegacyGetEnvironmentUsersyncStateMethod(), responseObserver);
    }

    /**
     * <pre>
     * Request a manual usersync. Specifying exactly one environment is required.
     * </pre>
     */
    default void syncUsers(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSyncUsersMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the status of a SyncUsers request.
     * </pre>
     */
    default void getSyncUsersStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSyncUsersStatusMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service WorkloadIam.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class WorkloadIamImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return WorkloadIamGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service WorkloadIam.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class WorkloadIamStub
      extends io.grpc.stub.AbstractAsyncStub<WorkloadIamStub> {
    private WorkloadIamStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WorkloadIamStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WorkloadIamStub(channel, callOptions);
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
     * Creates a new sync event. A sync event indicates that a state change is about to be made in
     * the control plane (usually in UMS). This service will persist the event and will asynchronously
     * sync the changes into the customer environment. In order to give time for the control plane
     * changes to go through before attempting a sync, a processing delay is included in the sync
     * event. To allow processing an event earlier, the CommitSyncEvent rpc should be called.
     * </pre>
     */
    public void createSyncEvent(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateSyncEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Update an existing sync event to indicate that it's ready for processing.
     * </pre>
     */
    public void commitSyncEvent(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCommitSyncEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the sync event state for an environment.
     * </pre>
     */
    public void getAutomatedSyncEnvironmentStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAutomatedSyncEnvironmentStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the status of a sync event.
     * </pre>
     */
    public void getSyncEventStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetSyncEventStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Request a manual usersync. Specifying multiple environments is supported
     * for backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    public void legacySyncUsers(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLegacySyncUsersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the status of a LegacySyncUsers request. This RPC is for supporting
     * backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    public void legacyGetSyncUsersStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLegacyGetSyncUsersStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the state and last usersync Crn for an environment. This RPC is for supporting
     * backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    public void legacyGetEnvironmentUsersyncState(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLegacyGetEnvironmentUsersyncStateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Request a manual usersync. Specifying exactly one environment is required.
     * </pre>
     */
    public void syncUsers(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSyncUsersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the status of a SyncUsers request.
     * </pre>
     */
    public void getSyncUsersStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetSyncUsersStatusMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service WorkloadIam.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class WorkloadIamBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<WorkloadIamBlockingV2Stub> {
    private WorkloadIamBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WorkloadIamBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WorkloadIamBlockingV2Stub(channel, callOptions);
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
     * Creates a new sync event. A sync event indicates that a state change is about to be made in
     * the control plane (usually in UMS). This service will persist the event and will asynchronously
     * sync the changes into the customer environment. In order to give time for the control plane
     * changes to go through before attempting a sync, a processing delay is included in the sync
     * event. To allow processing an event earlier, the CommitSyncEvent rpc should be called.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse createSyncEvent(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateSyncEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update an existing sync event to indicate that it's ready for processing.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse commitSyncEvent(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCommitSyncEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the sync event state for an environment.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse getAutomatedSyncEnvironmentStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetAutomatedSyncEnvironmentStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the status of a sync event.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse getSyncEventStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetSyncEventStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Request a manual usersync. Specifying multiple environments is supported
     * for backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse legacySyncUsers(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getLegacySyncUsersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the status of a LegacySyncUsers request. This RPC is for supporting
     * backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse legacyGetSyncUsersStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getLegacyGetSyncUsersStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the state and last usersync Crn for an environment. This RPC is for supporting
     * backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse legacyGetEnvironmentUsersyncState(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getLegacyGetEnvironmentUsersyncStateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Request a manual usersync. Specifying exactly one environment is required.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse syncUsers(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getSyncUsersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the status of a SyncUsers request.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse getSyncUsersStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetSyncUsersStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service WorkloadIam.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class WorkloadIamBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<WorkloadIamBlockingStub> {
    private WorkloadIamBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WorkloadIamBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WorkloadIamBlockingStub(channel, callOptions);
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
     * Creates a new sync event. A sync event indicates that a state change is about to be made in
     * the control plane (usually in UMS). This service will persist the event and will asynchronously
     * sync the changes into the customer environment. In order to give time for the control plane
     * changes to go through before attempting a sync, a processing delay is included in the sync
     * event. To allow processing an event earlier, the CommitSyncEvent rpc should be called.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse createSyncEvent(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateSyncEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update an existing sync event to indicate that it's ready for processing.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse commitSyncEvent(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCommitSyncEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the sync event state for an environment.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse getAutomatedSyncEnvironmentStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAutomatedSyncEnvironmentStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the status of a sync event.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse getSyncEventStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetSyncEventStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Request a manual usersync. Specifying multiple environments is supported
     * for backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse legacySyncUsers(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLegacySyncUsersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the status of a LegacySyncUsers request. This RPC is for supporting
     * backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse legacyGetSyncUsersStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLegacyGetSyncUsersStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the state and last usersync Crn for an environment. This RPC is for supporting
     * backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse legacyGetEnvironmentUsersyncState(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLegacyGetEnvironmentUsersyncStateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Request a manual usersync. Specifying exactly one environment is required.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse syncUsers(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSyncUsersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the status of a SyncUsers request.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse getSyncUsersStatus(com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetSyncUsersStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service WorkloadIam.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class WorkloadIamFutureStub
      extends io.grpc.stub.AbstractFutureStub<WorkloadIamFutureStub> {
    private WorkloadIamFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WorkloadIamFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WorkloadIamFutureStub(channel, callOptions);
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
     * Creates a new sync event. A sync event indicates that a state change is about to be made in
     * the control plane (usually in UMS). This service will persist the event and will asynchronously
     * sync the changes into the customer environment. In order to give time for the control plane
     * changes to go through before attempting a sync, a processing delay is included in the sync
     * event. To allow processing an event earlier, the CommitSyncEvent rpc should be called.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse> createSyncEvent(
        com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateSyncEventMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Update an existing sync event to indicate that it's ready for processing.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse> commitSyncEvent(
        com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCommitSyncEventMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the sync event state for an environment.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse> getAutomatedSyncEnvironmentStatus(
        com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAutomatedSyncEnvironmentStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the status of a sync event.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse> getSyncEventStatus(
        com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetSyncEventStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Request a manual usersync. Specifying multiple environments is supported
     * for backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse> legacySyncUsers(
        com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLegacySyncUsersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the status of a LegacySyncUsers request. This RPC is for supporting
     * backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse> legacyGetSyncUsersStatus(
        com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLegacyGetSyncUsersStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the state and last usersync Crn for an environment. This RPC is for supporting
     * backwards compatibility of the environments2 API service. Internal services
     * and new use cases should not use this RPC. Deprecated.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse> legacyGetEnvironmentUsersyncState(
        com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLegacyGetEnvironmentUsersyncStateMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Request a manual usersync. Specifying exactly one environment is required.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse> syncUsers(
        com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSyncUsersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the status of a SyncUsers request.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse> getSyncUsersStatus(
        com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetSyncUsersStatusMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_CREATE_SYNC_EVENT = 1;
  private static final int METHODID_COMMIT_SYNC_EVENT = 2;
  private static final int METHODID_GET_AUTOMATED_SYNC_ENVIRONMENT_STATUS = 3;
  private static final int METHODID_GET_SYNC_EVENT_STATUS = 4;
  private static final int METHODID_LEGACY_SYNC_USERS = 5;
  private static final int METHODID_LEGACY_GET_SYNC_USERS_STATUS = 6;
  private static final int METHODID_LEGACY_GET_ENVIRONMENT_USERSYNC_STATE = 7;
  private static final int METHODID_SYNC_USERS = 8;
  private static final int METHODID_GET_SYNC_USERS_STATUS = 9;

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
        case METHODID_CREATE_SYNC_EVENT:
          serviceImpl.createSyncEvent((com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse>) responseObserver);
          break;
        case METHODID_COMMIT_SYNC_EVENT:
          serviceImpl.commitSyncEvent((com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse>) responseObserver);
          break;
        case METHODID_GET_AUTOMATED_SYNC_ENVIRONMENT_STATUS:
          serviceImpl.getAutomatedSyncEnvironmentStatus((com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse>) responseObserver);
          break;
        case METHODID_GET_SYNC_EVENT_STATUS:
          serviceImpl.getSyncEventStatus((com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse>) responseObserver);
          break;
        case METHODID_LEGACY_SYNC_USERS:
          serviceImpl.legacySyncUsers((com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse>) responseObserver);
          break;
        case METHODID_LEGACY_GET_SYNC_USERS_STATUS:
          serviceImpl.legacyGetSyncUsersStatus((com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse>) responseObserver);
          break;
        case METHODID_LEGACY_GET_ENVIRONMENT_USERSYNC_STATE:
          serviceImpl.legacyGetEnvironmentUsersyncState((com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse>) responseObserver);
          break;
        case METHODID_SYNC_USERS:
          serviceImpl.syncUsers((com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse>) responseObserver);
          break;
        case METHODID_GET_SYNC_USERS_STATUS:
          serviceImpl.getSyncUsersStatus((com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse>) responseObserver);
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
          getCreateSyncEventMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventRequest,
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CreateSyncEventResponse>(
                service, METHODID_CREATE_SYNC_EVENT)))
        .addMethod(
          getCommitSyncEventMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventRequest,
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.CommitSyncEventResponse>(
                service, METHODID_COMMIT_SYNC_EVENT)))
        .addMethod(
          getGetAutomatedSyncEnvironmentStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusRequest,
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetAutomatedSyncEnvironmentStatusResponse>(
                service, METHODID_GET_AUTOMATED_SYNC_ENVIRONMENT_STATUS)))
        .addMethod(
          getGetSyncEventStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusRequest,
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncEventStatusResponse>(
                service, METHODID_GET_SYNC_EVENT_STATUS)))
        .addMethod(
          getLegacySyncUsersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersRequest,
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacySyncUsersResponse>(
                service, METHODID_LEGACY_SYNC_USERS)))
        .addMethod(
          getLegacyGetSyncUsersStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusRequest,
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetSyncUsersStatusResponse>(
                service, METHODID_LEGACY_GET_SYNC_USERS_STATUS)))
        .addMethod(
          getLegacyGetEnvironmentUsersyncStateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateRequest,
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.LegacyGetEnvironmentUsersyncStateResponse>(
                service, METHODID_LEGACY_GET_ENVIRONMENT_USERSYNC_STATE)))
        .addMethod(
          getSyncUsersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest,
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse>(
                service, METHODID_SYNC_USERS)))
        .addMethod(
          getGetSyncUsersStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusRequest,
              com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.GetSyncUsersStatusResponse>(
                service, METHODID_GET_SYNC_USERS_STATUS)))
        .build();
  }

  private static abstract class WorkloadIamBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    WorkloadIamBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("WorkloadIam");
    }
  }

  private static final class WorkloadIamFileDescriptorSupplier
      extends WorkloadIamBaseDescriptorSupplier {
    WorkloadIamFileDescriptorSupplier() {}
  }

  private static final class WorkloadIamMethodDescriptorSupplier
      extends WorkloadIamBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    WorkloadIamMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (WorkloadIamGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new WorkloadIamFileDescriptorSupplier())
              .addMethod(getGetVersionMethod())
              .addMethod(getCreateSyncEventMethod())
              .addMethod(getCommitSyncEventMethod())
              .addMethod(getGetAutomatedSyncEnvironmentStatusMethod())
              .addMethod(getGetSyncEventStatusMethod())
              .addMethod(getLegacySyncUsersMethod())
              .addMethod(getLegacyGetSyncUsersStatusMethod())
              .addMethod(getLegacyGetEnvironmentUsersyncStateMethod())
              .addMethod(getSyncUsersMethod())
              .addMethod(getGetSyncUsersStatusMethod())
              .build();
        }
      }
    }
    return result;
  }
}
