package com.cloudera.thunderhead.service.datalakedr;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class datalakeDRGrpc {

  private datalakeDRGrpc() {}

  public static final java.lang.String SERVICE_NAME = "datalakedr.datalakeDR";

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
    if ((getGetVersionMethod = datalakeDRGrpc.getGetVersionMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getGetVersionMethod = datalakeDRGrpc.getGetVersionMethod) == null) {
          datalakeDRGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new datalakeDRMethodDescriptorSupplier("GetVersion"))
              .build();
        }
      }
    }
    return getGetVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> getBackupDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BackupDatalake",
      requestType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> getBackupDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> getBackupDatalakeMethod;
    if ((getBackupDatalakeMethod = datalakeDRGrpc.getBackupDatalakeMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getBackupDatalakeMethod = datalakeDRGrpc.getBackupDatalakeMethod) == null) {
          datalakeDRGrpc.getBackupDatalakeMethod = getBackupDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BackupDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new datalakeDRMethodDescriptorSupplier("BackupDatalake"))
              .build();
        }
      }
    }
    return getBackupDatalakeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> getRestoreDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RestoreDatalake",
      requestType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> getRestoreDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> getRestoreDatalakeMethod;
    if ((getRestoreDatalakeMethod = datalakeDRGrpc.getRestoreDatalakeMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getRestoreDatalakeMethod = datalakeDRGrpc.getRestoreDatalakeMethod) == null) {
          datalakeDRGrpc.getRestoreDatalakeMethod = getRestoreDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RestoreDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new datalakeDRMethodDescriptorSupplier("RestoreDatalake"))
              .build();
        }
      }
    }
    return getRestoreDatalakeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> getBackupDatalakeStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BackupDatalakeStatus",
      requestType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest.class,
      responseType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> getBackupDatalakeStatusMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> getBackupDatalakeStatusMethod;
    if ((getBackupDatalakeStatusMethod = datalakeDRGrpc.getBackupDatalakeStatusMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getBackupDatalakeStatusMethod = datalakeDRGrpc.getBackupDatalakeStatusMethod) == null) {
          datalakeDRGrpc.getBackupDatalakeStatusMethod = getBackupDatalakeStatusMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BackupDatalakeStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new datalakeDRMethodDescriptorSupplier("BackupDatalakeStatus"))
              .build();
        }
      }
    }
    return getBackupDatalakeStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> getRestoreDatalakeStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RestoreDatalakeStatus",
      requestType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest.class,
      responseType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> getRestoreDatalakeStatusMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> getRestoreDatalakeStatusMethod;
    if ((getRestoreDatalakeStatusMethod = datalakeDRGrpc.getRestoreDatalakeStatusMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getRestoreDatalakeStatusMethod = datalakeDRGrpc.getRestoreDatalakeStatusMethod) == null) {
          datalakeDRGrpc.getRestoreDatalakeStatusMethod = getRestoreDatalakeStatusMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RestoreDatalakeStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new datalakeDRMethodDescriptorSupplier("RestoreDatalakeStatus"))
              .build();
        }
      }
    }
    return getRestoreDatalakeStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> getListDatalakeBackupsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListDatalakeBackups",
      requestType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest.class,
      responseType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> getListDatalakeBackupsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> getListDatalakeBackupsMethod;
    if ((getListDatalakeBackupsMethod = datalakeDRGrpc.getListDatalakeBackupsMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getListDatalakeBackupsMethod = datalakeDRGrpc.getListDatalakeBackupsMethod) == null) {
          datalakeDRGrpc.getListDatalakeBackupsMethod = getListDatalakeBackupsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListDatalakeBackups"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse.getDefaultInstance()))
              .setSchemaDescriptor(new datalakeDRMethodDescriptorSupplier("ListDatalakeBackups"))
              .build();
        }
      }
    }
    return getListDatalakeBackupsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse> getCancelDatalakeBackupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CancelDatalakeBackup",
      requestType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest.class,
      responseType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse> getCancelDatalakeBackupMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse> getCancelDatalakeBackupMethod;
    if ((getCancelDatalakeBackupMethod = datalakeDRGrpc.getCancelDatalakeBackupMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getCancelDatalakeBackupMethod = datalakeDRGrpc.getCancelDatalakeBackupMethod) == null) {
          datalakeDRGrpc.getCancelDatalakeBackupMethod = getCancelDatalakeBackupMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CancelDatalakeBackup"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse.getDefaultInstance()))
              .setSchemaDescriptor(new datalakeDRMethodDescriptorSupplier("CancelDatalakeBackup"))
              .build();
        }
      }
    }
    return getCancelDatalakeBackupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse> getCancelDatalakeRestoreMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CancelDatalakeRestore",
      requestType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest.class,
      responseType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse> getCancelDatalakeRestoreMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse> getCancelDatalakeRestoreMethod;
    if ((getCancelDatalakeRestoreMethod = datalakeDRGrpc.getCancelDatalakeRestoreMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getCancelDatalakeRestoreMethod = datalakeDRGrpc.getCancelDatalakeRestoreMethod) == null) {
          datalakeDRGrpc.getCancelDatalakeRestoreMethod = getCancelDatalakeRestoreMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CancelDatalakeRestore"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse.getDefaultInstance()))
              .setSchemaDescriptor(new datalakeDRMethodDescriptorSupplier("CancelDatalakeRestore"))
              .build();
        }
      }
    }
    return getCancelDatalakeRestoreMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse> getSubmitDatalakeDataInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SubmitDatalakeDataInfo",
      requestType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject.class,
      responseType = com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse> getSubmitDatalakeDataInfoMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse> getSubmitDatalakeDataInfoMethod;
    if ((getSubmitDatalakeDataInfoMethod = datalakeDRGrpc.getSubmitDatalakeDataInfoMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getSubmitDatalakeDataInfoMethod = datalakeDRGrpc.getSubmitDatalakeDataInfoMethod) == null) {
          datalakeDRGrpc.getSubmitDatalakeDataInfoMethod = getSubmitDatalakeDataInfoMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SubmitDatalakeDataInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new datalakeDRMethodDescriptorSupplier("SubmitDatalakeDataInfo"))
              .build();
        }
      }
    }
    return getSubmitDatalakeDataInfoMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static datalakeDRStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<datalakeDRStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<datalakeDRStub>() {
        @java.lang.Override
        public datalakeDRStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new datalakeDRStub(channel, callOptions);
        }
      };
    return datalakeDRStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static datalakeDRBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<datalakeDRBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<datalakeDRBlockingV2Stub>() {
        @java.lang.Override
        public datalakeDRBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new datalakeDRBlockingV2Stub(channel, callOptions);
        }
      };
    return datalakeDRBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static datalakeDRBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<datalakeDRBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<datalakeDRBlockingStub>() {
        @java.lang.Override
        public datalakeDRBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new datalakeDRBlockingStub(channel, callOptions);
        }
      };
    return datalakeDRBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static datalakeDRFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<datalakeDRFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<datalakeDRFutureStub>() {
        @java.lang.Override
        public datalakeDRFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new datalakeDRFutureStub(channel, callOptions);
        }
      };
    return datalakeDRFutureStub.newStub(factory, channel);
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
     **
     * Backup datalake
     * </pre>
     */
    default void backupDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBackupDatalakeMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Restore datalake
     * </pre>
     */
    default void restoreDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRestoreDatalakeMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Get the status datalake backup
     * </pre>
     */
    default void backupDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBackupDatalakeStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Get the status datalake restore
     * </pre>
     */
    default void restoreDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRestoreDatalakeStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * List the Backup's of a datalake
     * </pre>
     */
    default void listDatalakeBackups(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListDatalakeBackupsMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Cancel backup operation
     * </pre>
     */
    default void cancelDatalakeBackup(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCancelDatalakeBackupMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Cancel restore operation
     * </pre>
     */
    default void cancelDatalakeRestore(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCancelDatalakeRestoreMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Submit datalake data info for persisting and processing
     * </pre>
     */
    default void submitDatalakeDataInfo(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSubmitDatalakeDataInfoMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service datalakeDR.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class datalakeDRImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return datalakeDRGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service datalakeDR.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class datalakeDRStub
      extends io.grpc.stub.AbstractAsyncStub<datalakeDRStub> {
    private datalakeDRStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected datalakeDRStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new datalakeDRStub(channel, callOptions);
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
     **
     * Backup datalake
     * </pre>
     */
    public void backupDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBackupDatalakeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Restore datalake
     * </pre>
     */
    public void restoreDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRestoreDatalakeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Get the status datalake backup
     * </pre>
     */
    public void backupDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBackupDatalakeStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Get the status datalake restore
     * </pre>
     */
    public void restoreDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRestoreDatalakeStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * List the Backup's of a datalake
     * </pre>
     */
    public void listDatalakeBackups(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListDatalakeBackupsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Cancel backup operation
     * </pre>
     */
    public void cancelDatalakeBackup(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCancelDatalakeBackupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Cancel restore operation
     * </pre>
     */
    public void cancelDatalakeRestore(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCancelDatalakeRestoreMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Submit datalake data info for persisting and processing
     * </pre>
     */
    public void submitDatalakeDataInfo(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSubmitDatalakeDataInfoMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service datalakeDR.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class datalakeDRBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<datalakeDRBlockingV2Stub> {
    private datalakeDRBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected datalakeDRBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new datalakeDRBlockingV2Stub(channel, callOptions);
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
     **
     * Backup datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse backupDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getBackupDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Restore datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse restoreDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRestoreDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Get the status datalake backup
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse backupDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getBackupDatalakeStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Get the status datalake restore
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse restoreDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRestoreDatalakeStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * List the Backup's of a datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse listDatalakeBackups(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListDatalakeBackupsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Cancel backup operation
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse cancelDatalakeBackup(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCancelDatalakeBackupMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Cancel restore operation
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse cancelDatalakeRestore(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCancelDatalakeRestoreMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Submit datalake data info for persisting and processing
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse submitDatalakeDataInfo(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getSubmitDatalakeDataInfoMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service datalakeDR.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class datalakeDRBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<datalakeDRBlockingStub> {
    private datalakeDRBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected datalakeDRBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new datalakeDRBlockingStub(channel, callOptions);
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
     **
     * Backup datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse backupDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBackupDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Restore datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse restoreDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRestoreDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Get the status datalake backup
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse backupDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBackupDatalakeStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Get the status datalake restore
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse restoreDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRestoreDatalakeStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * List the Backup's of a datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse listDatalakeBackups(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListDatalakeBackupsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Cancel backup operation
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse cancelDatalakeBackup(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCancelDatalakeBackupMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Cancel restore operation
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse cancelDatalakeRestore(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCancelDatalakeRestoreMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Submit datalake data info for persisting and processing
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse submitDatalakeDataInfo(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSubmitDatalakeDataInfoMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service datalakeDR.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class datalakeDRFutureStub
      extends io.grpc.stub.AbstractFutureStub<datalakeDRFutureStub> {
    private datalakeDRFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected datalakeDRFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new datalakeDRFutureStub(channel, callOptions);
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
     **
     * Backup datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> backupDatalake(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBackupDatalakeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Restore datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> restoreDatalake(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRestoreDatalakeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Get the status datalake backup
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> backupDatalakeStatus(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBackupDatalakeStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Get the status datalake restore
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> restoreDatalakeStatus(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRestoreDatalakeStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * List the Backup's of a datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> listDatalakeBackups(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListDatalakeBackupsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Cancel backup operation
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse> cancelDatalakeBackup(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCancelDatalakeBackupMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Cancel restore operation
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse> cancelDatalakeRestore(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCancelDatalakeRestoreMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Submit datalake data info for persisting and processing
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse> submitDatalakeDataInfo(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSubmitDatalakeDataInfoMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_BACKUP_DATALAKE = 1;
  private static final int METHODID_RESTORE_DATALAKE = 2;
  private static final int METHODID_BACKUP_DATALAKE_STATUS = 3;
  private static final int METHODID_RESTORE_DATALAKE_STATUS = 4;
  private static final int METHODID_LIST_DATALAKE_BACKUPS = 5;
  private static final int METHODID_CANCEL_DATALAKE_BACKUP = 6;
  private static final int METHODID_CANCEL_DATALAKE_RESTORE = 7;
  private static final int METHODID_SUBMIT_DATALAKE_DATA_INFO = 8;

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
        case METHODID_BACKUP_DATALAKE:
          serviceImpl.backupDatalake((com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse>) responseObserver);
          break;
        case METHODID_RESTORE_DATALAKE:
          serviceImpl.restoreDatalake((com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse>) responseObserver);
          break;
        case METHODID_BACKUP_DATALAKE_STATUS:
          serviceImpl.backupDatalakeStatus((com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse>) responseObserver);
          break;
        case METHODID_RESTORE_DATALAKE_STATUS:
          serviceImpl.restoreDatalakeStatus((com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse>) responseObserver);
          break;
        case METHODID_LIST_DATALAKE_BACKUPS:
          serviceImpl.listDatalakeBackups((com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse>) responseObserver);
          break;
        case METHODID_CANCEL_DATALAKE_BACKUP:
          serviceImpl.cancelDatalakeBackup((com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse>) responseObserver);
          break;
        case METHODID_CANCEL_DATALAKE_RESTORE:
          serviceImpl.cancelDatalakeRestore((com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse>) responseObserver);
          break;
        case METHODID_SUBMIT_DATALAKE_DATA_INFO:
          serviceImpl.submitDatalakeDataInfo((com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse>) responseObserver);
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
          getBackupDatalakeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest,
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse>(
                service, METHODID_BACKUP_DATALAKE)))
        .addMethod(
          getRestoreDatalakeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest,
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse>(
                service, METHODID_RESTORE_DATALAKE)))
        .addMethod(
          getBackupDatalakeStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest,
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse>(
                service, METHODID_BACKUP_DATALAKE_STATUS)))
        .addMethod(
          getRestoreDatalakeStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest,
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse>(
                service, METHODID_RESTORE_DATALAKE_STATUS)))
        .addMethod(
          getListDatalakeBackupsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest,
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse>(
                service, METHODID_LIST_DATALAKE_BACKUPS)))
        .addMethod(
          getCancelDatalakeBackupMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupRequest,
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeBackupResponse>(
                service, METHODID_CANCEL_DATALAKE_BACKUP)))
        .addMethod(
          getCancelDatalakeRestoreMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreRequest,
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.CancelDatalakeRestoreResponse>(
                service, METHODID_CANCEL_DATALAKE_RESTORE)))
        .addMethod(
          getSubmitDatalakeDataInfoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject,
              com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SubmitDatalakeDataInfoResponse>(
                service, METHODID_SUBMIT_DATALAKE_DATA_INFO)))
        .build();
  }

  private static abstract class datalakeDRBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    datalakeDRBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("datalakeDR");
    }
  }

  private static final class datalakeDRFileDescriptorSupplier
      extends datalakeDRBaseDescriptorSupplier {
    datalakeDRFileDescriptorSupplier() {}
  }

  private static final class datalakeDRMethodDescriptorSupplier
      extends datalakeDRBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    datalakeDRMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (datalakeDRGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new datalakeDRFileDescriptorSupplier())
              .addMethod(getGetVersionMethod())
              .addMethod(getBackupDatalakeMethod())
              .addMethod(getRestoreDatalakeMethod())
              .addMethod(getBackupDatalakeStatusMethod())
              .addMethod(getRestoreDatalakeStatusMethod())
              .addMethod(getListDatalakeBackupsMethod())
              .addMethod(getCancelDatalakeBackupMethod())
              .addMethod(getCancelDatalakeRestoreMethod())
              .addMethod(getSubmitDatalakeDataInfoMethod())
              .build();
        }
      }
    }
    return result;
  }
}
