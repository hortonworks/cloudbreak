package com.cloudera.thunderhead.service.clusterconnectivitymanagementv2;

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
 * ClusterConnectivityManagementV2 service for provisioning and managing inverting-proxy.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.12.0)",
    comments = "Source: clusterconnectivitymanagementv2.proto")
public final class ClusterConnectivityManagementV2Grpc {

  private ClusterConnectivityManagementV2Grpc() {}

  public static final String SERVICE_NAME = "clusterconnectivitymanagementv2.ClusterConnectivityManagementV2";

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
    if ((getGetVersionMethod = ClusterConnectivityManagementV2Grpc.getGetVersionMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getGetVersionMethod = ClusterConnectivityManagementV2Grpc.getGetVersionMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getGetVersionMethod = getGetVersionMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "clusterconnectivitymanagementv2.ClusterConnectivityManagementV2", "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ClusterConnectivityManagementV2MethodDescriptorSupplier("GetVersion"))
                  .build();
          }
        }
     }
     return getGetVersionMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateOrGetInvertingProxyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> METHOD_CREATE_OR_GET_INVERTING_PROXY = getCreateOrGetInvertingProxyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> getCreateOrGetInvertingProxyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> getCreateOrGetInvertingProxyMethod() {
    return getCreateOrGetInvertingProxyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> getCreateOrGetInvertingProxyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> getCreateOrGetInvertingProxyMethod;
    if ((getCreateOrGetInvertingProxyMethod = ClusterConnectivityManagementV2Grpc.getCreateOrGetInvertingProxyMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getCreateOrGetInvertingProxyMethod = ClusterConnectivityManagementV2Grpc.getCreateOrGetInvertingProxyMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getCreateOrGetInvertingProxyMethod = getCreateOrGetInvertingProxyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "clusterconnectivitymanagementv2.ClusterConnectivityManagementV2", "CreateOrGetInvertingProxy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ClusterConnectivityManagementV2MethodDescriptorSupplier("CreateOrGetInvertingProxy"))
                  .build();
          }
        }
     }
     return getCreateOrGetInvertingProxyMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getRemoveInvertingProxyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> METHOD_REMOVE_INVERTING_PROXY = getRemoveInvertingProxyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> getRemoveInvertingProxyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> getRemoveInvertingProxyMethod() {
    return getRemoveInvertingProxyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> getRemoveInvertingProxyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> getRemoveInvertingProxyMethod;
    if ((getRemoveInvertingProxyMethod = ClusterConnectivityManagementV2Grpc.getRemoveInvertingProxyMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getRemoveInvertingProxyMethod = ClusterConnectivityManagementV2Grpc.getRemoveInvertingProxyMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getRemoveInvertingProxyMethod = getRemoveInvertingProxyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "clusterconnectivitymanagementv2.ClusterConnectivityManagementV2", "RemoveInvertingProxy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ClusterConnectivityManagementV2MethodDescriptorSupplier("RemoveInvertingProxy"))
                  .build();
          }
        }
     }
     return getRemoveInvertingProxyMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getRegisterAgentMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> METHOD_REGISTER_AGENT = getRegisterAgentMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> getRegisterAgentMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> getRegisterAgentMethod() {
    return getRegisterAgentMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> getRegisterAgentMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> getRegisterAgentMethod;
    if ((getRegisterAgentMethod = ClusterConnectivityManagementV2Grpc.getRegisterAgentMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getRegisterAgentMethod = ClusterConnectivityManagementV2Grpc.getRegisterAgentMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getRegisterAgentMethod = getRegisterAgentMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "clusterconnectivitymanagementv2.ClusterConnectivityManagementV2", "RegisterAgent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ClusterConnectivityManagementV2MethodDescriptorSupplier("RegisterAgent"))
                  .build();
          }
        }
     }
     return getRegisterAgentMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUnregisterAgentMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> METHOD_UNREGISTER_AGENT = getUnregisterAgentMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> getUnregisterAgentMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> getUnregisterAgentMethod() {
    return getUnregisterAgentMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> getUnregisterAgentMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> getUnregisterAgentMethod;
    if ((getUnregisterAgentMethod = ClusterConnectivityManagementV2Grpc.getUnregisterAgentMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getUnregisterAgentMethod = ClusterConnectivityManagementV2Grpc.getUnregisterAgentMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getUnregisterAgentMethod = getUnregisterAgentMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "clusterconnectivitymanagementv2.ClusterConnectivityManagementV2", "UnregisterAgent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ClusterConnectivityManagementV2MethodDescriptorSupplier("UnregisterAgent"))
                  .build();
          }
        }
     }
     return getUnregisterAgentMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListAgentsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> METHOD_LIST_AGENTS = getListAgentsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> getListAgentsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> getListAgentsMethod() {
    return getListAgentsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> getListAgentsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> getListAgentsMethod;
    if ((getListAgentsMethod = ClusterConnectivityManagementV2Grpc.getListAgentsMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getListAgentsMethod = ClusterConnectivityManagementV2Grpc.getListAgentsMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getListAgentsMethod = getListAgentsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "clusterconnectivitymanagementv2.ClusterConnectivityManagementV2", "ListAgents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ClusterConnectivityManagementV2MethodDescriptorSupplier("ListAgents"))
                  .build();
          }
        }
     }
     return getListAgentsMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAllAgentsCertificatesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> METHOD_GET_ALL_AGENTS_CERTIFICATES = getGetAllAgentsCertificatesMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> getGetAllAgentsCertificatesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> getGetAllAgentsCertificatesMethod() {
    return getGetAllAgentsCertificatesMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> getGetAllAgentsCertificatesMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> getGetAllAgentsCertificatesMethod;
    if ((getGetAllAgentsCertificatesMethod = ClusterConnectivityManagementV2Grpc.getGetAllAgentsCertificatesMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getGetAllAgentsCertificatesMethod = ClusterConnectivityManagementV2Grpc.getGetAllAgentsCertificatesMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getGetAllAgentsCertificatesMethod = getGetAllAgentsCertificatesMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "clusterconnectivitymanagementv2.ClusterConnectivityManagementV2", "GetAllAgentsCertificates"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ClusterConnectivityManagementV2MethodDescriptorSupplier("GetAllAgentsCertificates"))
                  .build();
          }
        }
     }
     return getGetAllAgentsCertificatesMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getRotateAgentAccessKeyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> METHOD_ROTATE_AGENT_ACCESS_KEY = getRotateAgentAccessKeyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> getRotateAgentAccessKeyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> getRotateAgentAccessKeyMethod() {
    return getRotateAgentAccessKeyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> getRotateAgentAccessKeyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> getRotateAgentAccessKeyMethod;
    if ((getRotateAgentAccessKeyMethod = ClusterConnectivityManagementV2Grpc.getRotateAgentAccessKeyMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getRotateAgentAccessKeyMethod = ClusterConnectivityManagementV2Grpc.getRotateAgentAccessKeyMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getRotateAgentAccessKeyMethod = getRotateAgentAccessKeyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "clusterconnectivitymanagementv2.ClusterConnectivityManagementV2", "RotateAgentAccessKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ClusterConnectivityManagementV2MethodDescriptorSupplier("RotateAgentAccessKey"))
                  .build();
          }
        }
     }
     return getRotateAgentAccessKeyMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ClusterConnectivityManagementV2Stub newStub(io.grpc.Channel channel) {
    return new ClusterConnectivityManagementV2Stub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ClusterConnectivityManagementV2BlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ClusterConnectivityManagementV2BlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ClusterConnectivityManagementV2FutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ClusterConnectivityManagementV2FutureStub(channel);
  }

  /**
   * <pre>
   * ClusterConnectivityManagementV2 service for provisioning and managing inverting-proxy.
   * </pre>
   */
  public static abstract class ClusterConnectivityManagementV2ImplBase implements io.grpc.BindableService {

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
     * CreateOrGetInvertingProxy will create new deployment If it is not already present.
     * It also polls for the status and updates the status accordingly.
     * </pre>
     */
    public void createOrGetInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateOrGetInvertingProxyMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * RemoveInvertingProxy will remove inverting-proxy deployment.
     * Mainly used for reaper process.
     * </pre>
     */
    public void removeInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRemoveInvertingProxyMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * RegisterAgent for generating and registering agent key-cert pair.
     * </pre>
     */
    public void registerAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRegisterAgentMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * UnregisterAgent for removing agent key-cert pair while environment deletion.
     * </pre>
     */
    public void unregisterAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getUnregisterAgentMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Lists all registered agents matching a supplied query
     * </pre>
     */
    public void listAgents(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListAgentsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * GetAllAgentsCertificates for getting certificates of all the agents for an account.
     * </pre>
     */
    public void getAllAgentsCertificates(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAllAgentsCertificatesMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * RotateAgentAccessKey for rotating workload machine user key pair
     * </pre>
     */
    public void rotateAgentAccessKey(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRotateAgentAccessKeyMethodHelper(), responseObserver);
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
            getCreateOrGetInvertingProxyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest,
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse>(
                  this, METHODID_CREATE_OR_GET_INVERTING_PROXY)))
          .addMethod(
            getRemoveInvertingProxyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest,
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse>(
                  this, METHODID_REMOVE_INVERTING_PROXY)))
          .addMethod(
            getRegisterAgentMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest,
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse>(
                  this, METHODID_REGISTER_AGENT)))
          .addMethod(
            getUnregisterAgentMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest,
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse>(
                  this, METHODID_UNREGISTER_AGENT)))
          .addMethod(
            getListAgentsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest,
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse>(
                  this, METHODID_LIST_AGENTS)))
          .addMethod(
            getGetAllAgentsCertificatesMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest,
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse>(
                  this, METHODID_GET_ALL_AGENTS_CERTIFICATES)))
          .addMethod(
            getRotateAgentAccessKeyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest,
                com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse>(
                  this, METHODID_ROTATE_AGENT_ACCESS_KEY)))
          .build();
    }
  }

  /**
   * <pre>
   * ClusterConnectivityManagementV2 service for provisioning and managing inverting-proxy.
   * </pre>
   */
  public static final class ClusterConnectivityManagementV2Stub extends io.grpc.stub.AbstractStub<ClusterConnectivityManagementV2Stub> {
    private ClusterConnectivityManagementV2Stub(io.grpc.Channel channel) {
      super(channel);
    }

    private ClusterConnectivityManagementV2Stub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterConnectivityManagementV2Stub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ClusterConnectivityManagementV2Stub(channel, callOptions);
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
     * CreateOrGetInvertingProxy will create new deployment If it is not already present.
     * It also polls for the status and updates the status accordingly.
     * </pre>
     */
    public void createOrGetInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateOrGetInvertingProxyMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * RemoveInvertingProxy will remove inverting-proxy deployment.
     * Mainly used for reaper process.
     * </pre>
     */
    public void removeInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRemoveInvertingProxyMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * RegisterAgent for generating and registering agent key-cert pair.
     * </pre>
     */
    public void registerAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRegisterAgentMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * UnregisterAgent for removing agent key-cert pair while environment deletion.
     * </pre>
     */
    public void unregisterAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnregisterAgentMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists all registered agents matching a supplied query
     * </pre>
     */
    public void listAgents(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListAgentsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetAllAgentsCertificates for getting certificates of all the agents for an account.
     * </pre>
     */
    public void getAllAgentsCertificates(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAllAgentsCertificatesMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * RotateAgentAccessKey for rotating workload machine user key pair
     * </pre>
     */
    public void rotateAgentAccessKey(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRotateAgentAccessKeyMethodHelper(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * ClusterConnectivityManagementV2 service for provisioning and managing inverting-proxy.
   * </pre>
   */
  public static final class ClusterConnectivityManagementV2BlockingStub extends io.grpc.stub.AbstractStub<ClusterConnectivityManagementV2BlockingStub> {
    private ClusterConnectivityManagementV2BlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ClusterConnectivityManagementV2BlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterConnectivityManagementV2BlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ClusterConnectivityManagementV2BlockingStub(channel, callOptions);
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
     * CreateOrGetInvertingProxy will create new deployment If it is not already present.
     * It also polls for the status and updates the status accordingly.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse createOrGetInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateOrGetInvertingProxyMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RemoveInvertingProxy will remove inverting-proxy deployment.
     * Mainly used for reaper process.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse removeInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest request) {
      return blockingUnaryCall(
          getChannel(), getRemoveInvertingProxyMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RegisterAgent for generating and registering agent key-cert pair.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse registerAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest request) {
      return blockingUnaryCall(
          getChannel(), getRegisterAgentMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * UnregisterAgent for removing agent key-cert pair while environment deletion.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse unregisterAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest request) {
      return blockingUnaryCall(
          getChannel(), getUnregisterAgentMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all registered agents matching a supplied query
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse listAgents(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListAgentsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetAllAgentsCertificates for getting certificates of all the agents for an account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse getAllAgentsCertificates(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetAllAgentsCertificatesMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RotateAgentAccessKey for rotating workload machine user key pair
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse rotateAgentAccessKey(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest request) {
      return blockingUnaryCall(
          getChannel(), getRotateAgentAccessKeyMethodHelper(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * ClusterConnectivityManagementV2 service for provisioning and managing inverting-proxy.
   * </pre>
   */
  public static final class ClusterConnectivityManagementV2FutureStub extends io.grpc.stub.AbstractStub<ClusterConnectivityManagementV2FutureStub> {
    private ClusterConnectivityManagementV2FutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ClusterConnectivityManagementV2FutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterConnectivityManagementV2FutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ClusterConnectivityManagementV2FutureStub(channel, callOptions);
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
     * CreateOrGetInvertingProxy will create new deployment If it is not already present.
     * It also polls for the status and updates the status accordingly.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> createOrGetInvertingProxy(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateOrGetInvertingProxyMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * RemoveInvertingProxy will remove inverting-proxy deployment.
     * Mainly used for reaper process.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> removeInvertingProxy(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRemoveInvertingProxyMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * RegisterAgent for generating and registering agent key-cert pair.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> registerAgent(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRegisterAgentMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * UnregisterAgent for removing agent key-cert pair while environment deletion.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> unregisterAgent(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getUnregisterAgentMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists all registered agents matching a supplied query
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> listAgents(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListAgentsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetAllAgentsCertificates for getting certificates of all the agents for an account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> getAllAgentsCertificates(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAllAgentsCertificatesMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * RotateAgentAccessKey for rotating workload machine user key pair
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> rotateAgentAccessKey(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRotateAgentAccessKeyMethodHelper(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_CREATE_OR_GET_INVERTING_PROXY = 1;
  private static final int METHODID_REMOVE_INVERTING_PROXY = 2;
  private static final int METHODID_REGISTER_AGENT = 3;
  private static final int METHODID_UNREGISTER_AGENT = 4;
  private static final int METHODID_LIST_AGENTS = 5;
  private static final int METHODID_GET_ALL_AGENTS_CERTIFICATES = 6;
  private static final int METHODID_ROTATE_AGENT_ACCESS_KEY = 7;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ClusterConnectivityManagementV2ImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ClusterConnectivityManagementV2ImplBase serviceImpl, int methodId) {
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
        case METHODID_CREATE_OR_GET_INVERTING_PROXY:
          serviceImpl.createOrGetInvertingProxy((com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse>) responseObserver);
          break;
        case METHODID_REMOVE_INVERTING_PROXY:
          serviceImpl.removeInvertingProxy((com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse>) responseObserver);
          break;
        case METHODID_REGISTER_AGENT:
          serviceImpl.registerAgent((com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse>) responseObserver);
          break;
        case METHODID_UNREGISTER_AGENT:
          serviceImpl.unregisterAgent((com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse>) responseObserver);
          break;
        case METHODID_LIST_AGENTS:
          serviceImpl.listAgents((com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse>) responseObserver);
          break;
        case METHODID_GET_ALL_AGENTS_CERTIFICATES:
          serviceImpl.getAllAgentsCertificates((com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse>) responseObserver);
          break;
        case METHODID_ROTATE_AGENT_ACCESS_KEY:
          serviceImpl.rotateAgentAccessKey((com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse>) responseObserver);
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

  private static abstract class ClusterConnectivityManagementV2BaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ClusterConnectivityManagementV2BaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ClusterConnectivityManagementV2");
    }
  }

  private static final class ClusterConnectivityManagementV2FileDescriptorSupplier
      extends ClusterConnectivityManagementV2BaseDescriptorSupplier {
    ClusterConnectivityManagementV2FileDescriptorSupplier() {}
  }

  private static final class ClusterConnectivityManagementV2MethodDescriptorSupplier
      extends ClusterConnectivityManagementV2BaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ClusterConnectivityManagementV2MethodDescriptorSupplier(String methodName) {
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
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ClusterConnectivityManagementV2FileDescriptorSupplier())
              .addMethod(getGetVersionMethodHelper())
              .addMethod(getCreateOrGetInvertingProxyMethodHelper())
              .addMethod(getRemoveInvertingProxyMethodHelper())
              .addMethod(getRegisterAgentMethodHelper())
              .addMethod(getUnregisterAgentMethodHelper())
              .addMethod(getListAgentsMethodHelper())
              .addMethod(getGetAllAgentsCertificatesMethodHelper())
              .addMethod(getRotateAgentAccessKeyMethodHelper())
              .build();
        }
      }
    }
    return result;
  }
}
