package com.cloudera.thunderhead.service.minasshdmanagement;

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
    comments = "Source: minasshdmanagement.proto")
public final class MinaSshdManagementGrpc {

  private MinaSshdManagementGrpc() {}

  public static final String SERVICE_NAME = "minasshdmanagement.MinaSshdManagement";

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
    if ((getGetVersionMethod = MinaSshdManagementGrpc.getGetVersionMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getGetVersionMethod = MinaSshdManagementGrpc.getGetVersionMethod) == null) {
          MinaSshdManagementGrpc.getGetVersionMethod = getGetVersionMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "minasshdmanagement.MinaSshdManagement", "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new MinaSshdManagementMethodDescriptorSupplier("GetVersion"))
                  .build();
          }
        }
     }
     return getGetVersionMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getAcquireMinaSshdServiceMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse> METHOD_ACQUIRE_MINA_SSHD_SERVICE = getAcquireMinaSshdServiceMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse> getAcquireMinaSshdServiceMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse> getAcquireMinaSshdServiceMethod() {
    return getAcquireMinaSshdServiceMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse> getAcquireMinaSshdServiceMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse> getAcquireMinaSshdServiceMethod;
    if ((getAcquireMinaSshdServiceMethod = MinaSshdManagementGrpc.getAcquireMinaSshdServiceMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getAcquireMinaSshdServiceMethod = MinaSshdManagementGrpc.getAcquireMinaSshdServiceMethod) == null) {
          MinaSshdManagementGrpc.getAcquireMinaSshdServiceMethod = getAcquireMinaSshdServiceMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "minasshdmanagement.MinaSshdManagement", "AcquireMinaSshdService"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new MinaSshdManagementMethodDescriptorSupplier("AcquireMinaSshdService"))
                  .build();
          }
        }
     }
     return getAcquireMinaSshdServiceMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListMinaSshdServicesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse> METHOD_LIST_MINA_SSHD_SERVICES = getListMinaSshdServicesMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse> getListMinaSshdServicesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse> getListMinaSshdServicesMethod() {
    return getListMinaSshdServicesMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse> getListMinaSshdServicesMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse> getListMinaSshdServicesMethod;
    if ((getListMinaSshdServicesMethod = MinaSshdManagementGrpc.getListMinaSshdServicesMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getListMinaSshdServicesMethod = MinaSshdManagementGrpc.getListMinaSshdServicesMethod) == null) {
          MinaSshdManagementGrpc.getListMinaSshdServicesMethod = getListMinaSshdServicesMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "minasshdmanagement.MinaSshdManagement", "ListMinaSshdServices"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new MinaSshdManagementMethodDescriptorSupplier("ListMinaSshdServices"))
                  .build();
          }
        }
     }
     return getListMinaSshdServicesMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGenerateAndRegisterSshTunnelingKeyPairMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse> METHOD_GENERATE_AND_REGISTER_SSH_TUNNELING_KEY_PAIR = getGenerateAndRegisterSshTunnelingKeyPairMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse> getGenerateAndRegisterSshTunnelingKeyPairMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse> getGenerateAndRegisterSshTunnelingKeyPairMethod() {
    return getGenerateAndRegisterSshTunnelingKeyPairMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse> getGenerateAndRegisterSshTunnelingKeyPairMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse> getGenerateAndRegisterSshTunnelingKeyPairMethod;
    if ((getGenerateAndRegisterSshTunnelingKeyPairMethod = MinaSshdManagementGrpc.getGenerateAndRegisterSshTunnelingKeyPairMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getGenerateAndRegisterSshTunnelingKeyPairMethod = MinaSshdManagementGrpc.getGenerateAndRegisterSshTunnelingKeyPairMethod) == null) {
          MinaSshdManagementGrpc.getGenerateAndRegisterSshTunnelingKeyPairMethod = getGenerateAndRegisterSshTunnelingKeyPairMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "minasshdmanagement.MinaSshdManagement", "GenerateAndRegisterSshTunnelingKeyPair"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new MinaSshdManagementMethodDescriptorSupplier("GenerateAndRegisterSshTunnelingKeyPair"))
                  .build();
          }
        }
     }
     return getGenerateAndRegisterSshTunnelingKeyPairMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getRegisterSshTunnelingKeyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse> METHOD_REGISTER_SSH_TUNNELING_KEY = getRegisterSshTunnelingKeyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse> getRegisterSshTunnelingKeyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse> getRegisterSshTunnelingKeyMethod() {
    return getRegisterSshTunnelingKeyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse> getRegisterSshTunnelingKeyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse> getRegisterSshTunnelingKeyMethod;
    if ((getRegisterSshTunnelingKeyMethod = MinaSshdManagementGrpc.getRegisterSshTunnelingKeyMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getRegisterSshTunnelingKeyMethod = MinaSshdManagementGrpc.getRegisterSshTunnelingKeyMethod) == null) {
          MinaSshdManagementGrpc.getRegisterSshTunnelingKeyMethod = getRegisterSshTunnelingKeyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "minasshdmanagement.MinaSshdManagement", "RegisterSshTunnelingKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new MinaSshdManagementMethodDescriptorSupplier("RegisterSshTunnelingKey"))
                  .build();
          }
        }
     }
     return getRegisterSshTunnelingKeyMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUnregisterSshTunnelingKeyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> METHOD_UNREGISTER_SSH_TUNNELING_KEY = getUnregisterSshTunnelingKeyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> getUnregisterSshTunnelingKeyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> getUnregisterSshTunnelingKeyMethod() {
    return getUnregisterSshTunnelingKeyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> getUnregisterSshTunnelingKeyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> getUnregisterSshTunnelingKeyMethod;
    if ((getUnregisterSshTunnelingKeyMethod = MinaSshdManagementGrpc.getUnregisterSshTunnelingKeyMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getUnregisterSshTunnelingKeyMethod = MinaSshdManagementGrpc.getUnregisterSshTunnelingKeyMethod) == null) {
          MinaSshdManagementGrpc.getUnregisterSshTunnelingKeyMethod = getUnregisterSshTunnelingKeyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "minasshdmanagement.MinaSshdManagement", "UnregisterSshTunnelingKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new MinaSshdManagementMethodDescriptorSupplier("UnregisterSshTunnelingKey"))
                  .build();
          }
        }
     }
     return getUnregisterSshTunnelingKeyMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListSshTunnelingKeysMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> METHOD_LIST_SSH_TUNNELING_KEYS = getListSshTunnelingKeysMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> getListSshTunnelingKeysMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> getListSshTunnelingKeysMethod() {
    return getListSshTunnelingKeysMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> getListSshTunnelingKeysMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> getListSshTunnelingKeysMethod;
    if ((getListSshTunnelingKeysMethod = MinaSshdManagementGrpc.getListSshTunnelingKeysMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getListSshTunnelingKeysMethod = MinaSshdManagementGrpc.getListSshTunnelingKeysMethod) == null) {
          MinaSshdManagementGrpc.getListSshTunnelingKeysMethod = getListSshTunnelingKeysMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "minasshdmanagement.MinaSshdManagement", "ListSshTunnelingKeys"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new MinaSshdManagementMethodDescriptorSupplier("ListSshTunnelingKeys"))
                  .build();
          }
        }
     }
     return getListSshTunnelingKeysMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static MinaSshdManagementStub newStub(io.grpc.Channel channel) {
    return new MinaSshdManagementStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MinaSshdManagementBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new MinaSshdManagementBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MinaSshdManagementFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new MinaSshdManagementFutureStub(channel);
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class MinaSshdManagementImplBase implements io.grpc.BindableService {

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
     * Acquire an open MinaSshdService. This always returns a MinaSshdService
     * open to rececive new connections of which there will always be at most
     * one per account.
     * The MinaSshdService may not be ready. If one already exists, it will
     * be returned. If one does not, a new one will have been created, but the
     * initializing workflows may not have completed yet.
     * </pre>
     */
    public void acquireMinaSshdService(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAcquireMinaSshdServiceMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Currently there can only be one MinaSshdService open per account.
     * Use this to check if the MinaSshdService is ready as well as to get
     * the public key for the server.
     * </pre>
     */
    public void listMinaSshdServices(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListMinaSshdServicesMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Generates a ssh key pair, registers the public key, and returns the
     * enciphered private key and public key
     * MinaSshdService must be ready or this will fail.
     * </pre>
     */
    public void generateAndRegisterSshTunnelingKeyPair(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGenerateAndRegisterSshTunnelingKeyPairMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Register an ssh tunneling public key in case you want to generate the ssh
     * tunneling key pair separately or use a different algorithm from the
     * default.
     * Right now only RSA or ED25519 are supported
     * MinaSshdService must be ready or this will fail.
     * </pre>
     */
    public void registerSshTunnelingKey(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRegisterSshTunnelingKeyMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Unregister Ssh Tunneling Key
     * </pre>
     */
    public void unregisterSshTunnelingKey(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getUnregisterSshTunnelingKeyMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * For minaSshdService
     * </pre>
     */
    public void listSshTunnelingKeys(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListSshTunnelingKeysMethodHelper(), responseObserver);
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
            getAcquireMinaSshdServiceMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest,
                com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse>(
                  this, METHODID_ACQUIRE_MINA_SSHD_SERVICE)))
          .addMethod(
            getListMinaSshdServicesMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest,
                com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse>(
                  this, METHODID_LIST_MINA_SSHD_SERVICES)))
          .addMethod(
            getGenerateAndRegisterSshTunnelingKeyPairMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest,
                com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse>(
                  this, METHODID_GENERATE_AND_REGISTER_SSH_TUNNELING_KEY_PAIR)))
          .addMethod(
            getRegisterSshTunnelingKeyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest,
                com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse>(
                  this, METHODID_REGISTER_SSH_TUNNELING_KEY)))
          .addMethod(
            getUnregisterSshTunnelingKeyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest,
                com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse>(
                  this, METHODID_UNREGISTER_SSH_TUNNELING_KEY)))
          .addMethod(
            getListSshTunnelingKeysMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest,
                com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse>(
                  this, METHODID_LIST_SSH_TUNNELING_KEYS)))
          .build();
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MinaSshdManagementStub extends io.grpc.stub.AbstractStub<MinaSshdManagementStub> {
    private MinaSshdManagementStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MinaSshdManagementStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MinaSshdManagementStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MinaSshdManagementStub(channel, callOptions);
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
     * Acquire an open MinaSshdService. This always returns a MinaSshdService
     * open to rececive new connections of which there will always be at most
     * one per account.
     * The MinaSshdService may not be ready. If one already exists, it will
     * be returned. If one does not, a new one will have been created, but the
     * initializing workflows may not have completed yet.
     * </pre>
     */
    public void acquireMinaSshdService(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAcquireMinaSshdServiceMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Currently there can only be one MinaSshdService open per account.
     * Use this to check if the MinaSshdService is ready as well as to get
     * the public key for the server.
     * </pre>
     */
    public void listMinaSshdServices(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListMinaSshdServicesMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Generates a ssh key pair, registers the public key, and returns the
     * enciphered private key and public key
     * MinaSshdService must be ready or this will fail.
     * </pre>
     */
    public void generateAndRegisterSshTunnelingKeyPair(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGenerateAndRegisterSshTunnelingKeyPairMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Register an ssh tunneling public key in case you want to generate the ssh
     * tunneling key pair separately or use a different algorithm from the
     * default.
     * Right now only RSA or ED25519 are supported
     * MinaSshdService must be ready or this will fail.
     * </pre>
     */
    public void registerSshTunnelingKey(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRegisterSshTunnelingKeyMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unregister Ssh Tunneling Key
     * </pre>
     */
    public void unregisterSshTunnelingKey(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnregisterSshTunnelingKeyMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * For minaSshdService
     * </pre>
     */
    public void listSshTunnelingKeys(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListSshTunnelingKeysMethodHelper(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MinaSshdManagementBlockingStub extends io.grpc.stub.AbstractStub<MinaSshdManagementBlockingStub> {
    private MinaSshdManagementBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MinaSshdManagementBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MinaSshdManagementBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MinaSshdManagementBlockingStub(channel, callOptions);
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
     * Acquire an open MinaSshdService. This always returns a MinaSshdService
     * open to rececive new connections of which there will always be at most
     * one per account.
     * The MinaSshdService may not be ready. If one already exists, it will
     * be returned. If one does not, a new one will have been created, but the
     * initializing workflows may not have completed yet.
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse acquireMinaSshdService(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest request) {
      return blockingUnaryCall(
          getChannel(), getAcquireMinaSshdServiceMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Currently there can only be one MinaSshdService open per account.
     * Use this to check if the MinaSshdService is ready as well as to get
     * the public key for the server.
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse listMinaSshdServices(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest request) {
      return blockingUnaryCall(
          getChannel(), getListMinaSshdServicesMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Generates a ssh key pair, registers the public key, and returns the
     * enciphered private key and public key
     * MinaSshdService must be ready or this will fail.
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse generateAndRegisterSshTunnelingKeyPair(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest request) {
      return blockingUnaryCall(
          getChannel(), getGenerateAndRegisterSshTunnelingKeyPairMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Register an ssh tunneling public key in case you want to generate the ssh
     * tunneling key pair separately or use a different algorithm from the
     * default.
     * Right now only RSA or ED25519 are supported
     * MinaSshdService must be ready or this will fail.
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse registerSshTunnelingKey(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest request) {
      return blockingUnaryCall(
          getChannel(), getRegisterSshTunnelingKeyMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unregister Ssh Tunneling Key
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse unregisterSshTunnelingKey(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest request) {
      return blockingUnaryCall(
          getChannel(), getUnregisterSshTunnelingKeyMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * For minaSshdService
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse listSshTunnelingKeys(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest request) {
      return blockingUnaryCall(
          getChannel(), getListSshTunnelingKeysMethodHelper(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MinaSshdManagementFutureStub extends io.grpc.stub.AbstractStub<MinaSshdManagementFutureStub> {
    private MinaSshdManagementFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MinaSshdManagementFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MinaSshdManagementFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MinaSshdManagementFutureStub(channel, callOptions);
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
     * Acquire an open MinaSshdService. This always returns a MinaSshdService
     * open to rececive new connections of which there will always be at most
     * one per account.
     * The MinaSshdService may not be ready. If one already exists, it will
     * be returned. If one does not, a new one will have been created, but the
     * initializing workflows may not have completed yet.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse> acquireMinaSshdService(
        com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAcquireMinaSshdServiceMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Currently there can only be one MinaSshdService open per account.
     * Use this to check if the MinaSshdService is ready as well as to get
     * the public key for the server.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse> listMinaSshdServices(
        com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListMinaSshdServicesMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Generates a ssh key pair, registers the public key, and returns the
     * enciphered private key and public key
     * MinaSshdService must be ready or this will fail.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse> generateAndRegisterSshTunnelingKeyPair(
        com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGenerateAndRegisterSshTunnelingKeyPairMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Register an ssh tunneling public key in case you want to generate the ssh
     * tunneling key pair separately or use a different algorithm from the
     * default.
     * Right now only RSA or ED25519 are supported
     * MinaSshdService must be ready or this will fail.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse> registerSshTunnelingKey(
        com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRegisterSshTunnelingKeyMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unregister Ssh Tunneling Key
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> unregisterSshTunnelingKey(
        com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getUnregisterSshTunnelingKeyMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * For minaSshdService
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> listSshTunnelingKeys(
        com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListSshTunnelingKeysMethodHelper(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_ACQUIRE_MINA_SSHD_SERVICE = 1;
  private static final int METHODID_LIST_MINA_SSHD_SERVICES = 2;
  private static final int METHODID_GENERATE_AND_REGISTER_SSH_TUNNELING_KEY_PAIR = 3;
  private static final int METHODID_REGISTER_SSH_TUNNELING_KEY = 4;
  private static final int METHODID_UNREGISTER_SSH_TUNNELING_KEY = 5;
  private static final int METHODID_LIST_SSH_TUNNELING_KEYS = 6;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final MinaSshdManagementImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(MinaSshdManagementImplBase serviceImpl, int methodId) {
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
        case METHODID_ACQUIRE_MINA_SSHD_SERVICE:
          serviceImpl.acquireMinaSshdService((com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse>) responseObserver);
          break;
        case METHODID_LIST_MINA_SSHD_SERVICES:
          serviceImpl.listMinaSshdServices((com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse>) responseObserver);
          break;
        case METHODID_GENERATE_AND_REGISTER_SSH_TUNNELING_KEY_PAIR:
          serviceImpl.generateAndRegisterSshTunnelingKeyPair((com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse>) responseObserver);
          break;
        case METHODID_REGISTER_SSH_TUNNELING_KEY:
          serviceImpl.registerSshTunnelingKey((com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse>) responseObserver);
          break;
        case METHODID_UNREGISTER_SSH_TUNNELING_KEY:
          serviceImpl.unregisterSshTunnelingKey((com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse>) responseObserver);
          break;
        case METHODID_LIST_SSH_TUNNELING_KEYS:
          serviceImpl.listSshTunnelingKeys((com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse>) responseObserver);
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

  private static abstract class MinaSshdManagementBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    MinaSshdManagementBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("MinaSshdManagement");
    }
  }

  private static final class MinaSshdManagementFileDescriptorSupplier
      extends MinaSshdManagementBaseDescriptorSupplier {
    MinaSshdManagementFileDescriptorSupplier() {}
  }

  private static final class MinaSshdManagementMethodDescriptorSupplier
      extends MinaSshdManagementBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    MinaSshdManagementMethodDescriptorSupplier(String methodName) {
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
      synchronized (MinaSshdManagementGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new MinaSshdManagementFileDescriptorSupplier())
              .addMethod(getGetVersionMethodHelper())
              .addMethod(getAcquireMinaSshdServiceMethodHelper())
              .addMethod(getListMinaSshdServicesMethodHelper())
              .addMethod(getGenerateAndRegisterSshTunnelingKeyPairMethodHelper())
              .addMethod(getRegisterSshTunnelingKeyMethodHelper())
              .addMethod(getUnregisterSshTunnelingKeyMethodHelper())
              .addMethod(getListSshTunnelingKeysMethodHelper())
              .build();
        }
      }
    }
    return result;
  }
}
