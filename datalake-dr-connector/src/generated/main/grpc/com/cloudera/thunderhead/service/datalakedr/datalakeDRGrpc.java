package com.cloudera.thunderhead.service.datalakedr;

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
    comments = "Source: datalakedr.proto")
public final class datalakeDRGrpc {

  private datalakeDRGrpc() {}

  public static final String SERVICE_NAME = "datalakedr.datalakeDR";

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
    if ((getGetVersionMethod = datalakeDRGrpc.getGetVersionMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getGetVersionMethod = datalakeDRGrpc.getGetVersionMethod) == null) {
          datalakeDRGrpc.getGetVersionMethod = getGetVersionMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "datalakedr.datalakeDR", "GetVersion"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getBackupDatalakeMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> METHOD_BACKUP_DATALAKE = getBackupDatalakeMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> getBackupDatalakeMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> getBackupDatalakeMethod() {
    return getBackupDatalakeMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> getBackupDatalakeMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> getBackupDatalakeMethod;
    if ((getBackupDatalakeMethod = datalakeDRGrpc.getBackupDatalakeMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getBackupDatalakeMethod = datalakeDRGrpc.getBackupDatalakeMethod) == null) {
          datalakeDRGrpc.getBackupDatalakeMethod = getBackupDatalakeMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "datalakedr.datalakeDR", "BackupDatalake"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getRestoreDatalakeMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> METHOD_RESTORE_DATALAKE = getRestoreDatalakeMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> getRestoreDatalakeMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> getRestoreDatalakeMethod() {
    return getRestoreDatalakeMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> getRestoreDatalakeMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> getRestoreDatalakeMethod;
    if ((getRestoreDatalakeMethod = datalakeDRGrpc.getRestoreDatalakeMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getRestoreDatalakeMethod = datalakeDRGrpc.getRestoreDatalakeMethod) == null) {
          datalakeDRGrpc.getRestoreDatalakeMethod = getRestoreDatalakeMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "datalakedr.datalakeDR", "RestoreDatalake"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getBackupDatalakeStatusMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> METHOD_BACKUP_DATALAKE_STATUS = getBackupDatalakeStatusMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> getBackupDatalakeStatusMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> getBackupDatalakeStatusMethod() {
    return getBackupDatalakeStatusMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> getBackupDatalakeStatusMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> getBackupDatalakeStatusMethod;
    if ((getBackupDatalakeStatusMethod = datalakeDRGrpc.getBackupDatalakeStatusMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getBackupDatalakeStatusMethod = datalakeDRGrpc.getBackupDatalakeStatusMethod) == null) {
          datalakeDRGrpc.getBackupDatalakeStatusMethod = getBackupDatalakeStatusMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "datalakedr.datalakeDR", "BackupDatalakeStatus"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getRestoreDatalakeStatusMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> METHOD_RESTORE_DATALAKE_STATUS = getRestoreDatalakeStatusMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> getRestoreDatalakeStatusMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> getRestoreDatalakeStatusMethod() {
    return getRestoreDatalakeStatusMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> getRestoreDatalakeStatusMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> getRestoreDatalakeStatusMethod;
    if ((getRestoreDatalakeStatusMethod = datalakeDRGrpc.getRestoreDatalakeStatusMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getRestoreDatalakeStatusMethod = datalakeDRGrpc.getRestoreDatalakeStatusMethod) == null) {
          datalakeDRGrpc.getRestoreDatalakeStatusMethod = getRestoreDatalakeStatusMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "datalakedr.datalakeDR", "RestoreDatalakeStatus"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListDatalakeBackupsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> METHOD_LIST_DATALAKE_BACKUPS = getListDatalakeBackupsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> getListDatalakeBackupsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> getListDatalakeBackupsMethod() {
    return getListDatalakeBackupsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest,
      com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> getListDatalakeBackupsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> getListDatalakeBackupsMethod;
    if ((getListDatalakeBackupsMethod = datalakeDRGrpc.getListDatalakeBackupsMethod) == null) {
      synchronized (datalakeDRGrpc.class) {
        if ((getListDatalakeBackupsMethod = datalakeDRGrpc.getListDatalakeBackupsMethod) == null) {
          datalakeDRGrpc.getListDatalakeBackupsMethod = getListDatalakeBackupsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest, com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "datalakedr.datalakeDR", "ListDatalakeBackups"))
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

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static datalakeDRStub newStub(io.grpc.Channel channel) {
    return new datalakeDRStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static datalakeDRBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new datalakeDRBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static datalakeDRFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new datalakeDRFutureStub(channel);
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class datalakeDRImplBase implements io.grpc.BindableService {

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
     **
     * Backup datalake
     * </pre>
     */
    public void backupDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getBackupDatalakeMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     **
     * Restore datalake
     * </pre>
     */
    public void restoreDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRestoreDatalakeMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     **
     * Get the status datalake backup
     * </pre>
     */
    public void backupDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getBackupDatalakeStatusMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     **
     * Get the status datalake restore
     * </pre>
     */
    public void restoreDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRestoreDatalakeStatusMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     **
     * List the Backup's of a datalake
     * </pre>
     */
    public void listDatalakeBackups(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListDatalakeBackupsMethodHelper(), responseObserver);
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
            getBackupDatalakeMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest,
                com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse>(
                  this, METHODID_BACKUP_DATALAKE)))
          .addMethod(
            getRestoreDatalakeMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest,
                com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse>(
                  this, METHODID_RESTORE_DATALAKE)))
          .addMethod(
            getBackupDatalakeStatusMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest,
                com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse>(
                  this, METHODID_BACKUP_DATALAKE_STATUS)))
          .addMethod(
            getRestoreDatalakeStatusMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest,
                com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse>(
                  this, METHODID_RESTORE_DATALAKE_STATUS)))
          .addMethod(
            getListDatalakeBackupsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest,
                com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse>(
                  this, METHODID_LIST_DATALAKE_BACKUPS)))
          .build();
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class datalakeDRStub extends io.grpc.stub.AbstractStub<datalakeDRStub> {
    private datalakeDRStub(io.grpc.Channel channel) {
      super(channel);
    }

    private datalakeDRStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected datalakeDRStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new datalakeDRStub(channel, callOptions);
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
     **
     * Backup datalake
     * </pre>
     */
    public void backupDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBackupDatalakeMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Restore datalake
     * </pre>
     */
    public void restoreDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRestoreDatalakeMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Get the status datalake backup
     * </pre>
     */
    public void backupDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBackupDatalakeStatusMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Get the status datalake restore
     * </pre>
     */
    public void restoreDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRestoreDatalakeStatusMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * List the Backup's of a datalake
     * </pre>
     */
    public void listDatalakeBackups(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListDatalakeBackupsMethodHelper(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class datalakeDRBlockingStub extends io.grpc.stub.AbstractStub<datalakeDRBlockingStub> {
    private datalakeDRBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private datalakeDRBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected datalakeDRBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new datalakeDRBlockingStub(channel, callOptions);
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
     **
     * Backup datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse backupDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest request) {
      return blockingUnaryCall(
          getChannel(), getBackupDatalakeMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Restore datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse restoreDatalake(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest request) {
      return blockingUnaryCall(
          getChannel(), getRestoreDatalakeMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Get the status datalake backup
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse backupDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest request) {
      return blockingUnaryCall(
          getChannel(), getBackupDatalakeStatusMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Get the status datalake restore
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse restoreDatalakeStatus(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest request) {
      return blockingUnaryCall(
          getChannel(), getRestoreDatalakeStatusMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * List the Backup's of a datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse listDatalakeBackups(com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest request) {
      return blockingUnaryCall(
          getChannel(), getListDatalakeBackupsMethodHelper(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class datalakeDRFutureStub extends io.grpc.stub.AbstractStub<datalakeDRFutureStub> {
    private datalakeDRFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private datalakeDRFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected datalakeDRFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new datalakeDRFutureStub(channel, callOptions);
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
     **
     * Backup datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse> backupDatalake(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getBackupDatalakeMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Restore datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse> restoreDatalake(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRestoreDatalakeMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Get the status datalake backup
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse> backupDatalakeStatus(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getBackupDatalakeStatusMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Get the status datalake restore
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse> restoreDatalakeStatus(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRestoreDatalakeStatusMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * List the Backup's of a datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupResponse> listDatalakeBackups(
        com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.ListDatalakeBackupRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListDatalakeBackupsMethodHelper(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_BACKUP_DATALAKE = 1;
  private static final int METHODID_RESTORE_DATALAKE = 2;
  private static final int METHODID_BACKUP_DATALAKE_STATUS = 3;
  private static final int METHODID_RESTORE_DATALAKE_STATUS = 4;
  private static final int METHODID_LIST_DATALAKE_BACKUPS = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final datalakeDRImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(datalakeDRImplBase serviceImpl, int methodId) {
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
    private final String methodName;

    datalakeDRMethodDescriptorSupplier(String methodName) {
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
              .addMethod(getGetVersionMethodHelper())
              .addMethod(getBackupDatalakeMethodHelper())
              .addMethod(getRestoreDatalakeMethodHelper())
              .addMethod(getBackupDatalakeStatusMethodHelper())
              .addMethod(getRestoreDatalakeStatusMethodHelper())
              .addMethod(getListDatalakeBackupsMethodHelper())
              .build();
        }
      }
    }
    return result;
  }
}
