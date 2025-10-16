package com.cloudera.thunderhead.service.clusterconnectivitymanagementv2;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * ClusterConnectivityManagementV2 service for provisioning and managing inverting-proxy.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class ClusterConnectivityManagementV2Grpc {

  private ClusterConnectivityManagementV2Grpc() {}

  public static final java.lang.String SERVICE_NAME = "clusterconnectivitymanagementv2.ClusterConnectivityManagementV2";

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
    if ((getGetVersionMethod = ClusterConnectivityManagementV2Grpc.getGetVersionMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getGetVersionMethod = ClusterConnectivityManagementV2Grpc.getGetVersionMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> getCreateOrGetInvertingProxyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateOrGetInvertingProxy",
      requestType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest.class,
      responseType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> getCreateOrGetInvertingProxyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> getCreateOrGetInvertingProxyMethod;
    if ((getCreateOrGetInvertingProxyMethod = ClusterConnectivityManagementV2Grpc.getCreateOrGetInvertingProxyMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getCreateOrGetInvertingProxyMethod = ClusterConnectivityManagementV2Grpc.getCreateOrGetInvertingProxyMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getCreateOrGetInvertingProxyMethod = getCreateOrGetInvertingProxyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateOrGetInvertingProxy"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> getRemoveInvertingProxyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RemoveInvertingProxy",
      requestType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest.class,
      responseType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> getRemoveInvertingProxyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> getRemoveInvertingProxyMethod;
    if ((getRemoveInvertingProxyMethod = ClusterConnectivityManagementV2Grpc.getRemoveInvertingProxyMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getRemoveInvertingProxyMethod = ClusterConnectivityManagementV2Grpc.getRemoveInvertingProxyMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getRemoveInvertingProxyMethod = getRemoveInvertingProxyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RemoveInvertingProxy"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> getRegisterAgentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterAgent",
      requestType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest.class,
      responseType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> getRegisterAgentMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> getRegisterAgentMethod;
    if ((getRegisterAgentMethod = ClusterConnectivityManagementV2Grpc.getRegisterAgentMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getRegisterAgentMethod = ClusterConnectivityManagementV2Grpc.getRegisterAgentMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getRegisterAgentMethod = getRegisterAgentMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterAgent"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> getUnregisterAgentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnregisterAgent",
      requestType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest.class,
      responseType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> getUnregisterAgentMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> getUnregisterAgentMethod;
    if ((getUnregisterAgentMethod = ClusterConnectivityManagementV2Grpc.getUnregisterAgentMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getUnregisterAgentMethod = ClusterConnectivityManagementV2Grpc.getUnregisterAgentMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getUnregisterAgentMethod = getUnregisterAgentMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnregisterAgent"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> getListAgentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListAgents",
      requestType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest.class,
      responseType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> getListAgentsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> getListAgentsMethod;
    if ((getListAgentsMethod = ClusterConnectivityManagementV2Grpc.getListAgentsMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getListAgentsMethod = ClusterConnectivityManagementV2Grpc.getListAgentsMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getListAgentsMethod = getListAgentsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListAgents"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> getGetAllAgentsCertificatesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAllAgentsCertificates",
      requestType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest.class,
      responseType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> getGetAllAgentsCertificatesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> getGetAllAgentsCertificatesMethod;
    if ((getGetAllAgentsCertificatesMethod = ClusterConnectivityManagementV2Grpc.getGetAllAgentsCertificatesMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getGetAllAgentsCertificatesMethod = ClusterConnectivityManagementV2Grpc.getGetAllAgentsCertificatesMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getGetAllAgentsCertificatesMethod = getGetAllAgentsCertificatesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAllAgentsCertificates"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> getRotateAgentAccessKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RotateAgentAccessKey",
      requestType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> getRotateAgentAccessKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> getRotateAgentAccessKeyMethod;
    if ((getRotateAgentAccessKeyMethod = ClusterConnectivityManagementV2Grpc.getRotateAgentAccessKeyMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getRotateAgentAccessKeyMethod = ClusterConnectivityManagementV2Grpc.getRotateAgentAccessKeyMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getRotateAgentAccessKeyMethod = getRotateAgentAccessKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RotateAgentAccessKey"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse> getDeactivateAgentAccessKeyPairMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeactivateAgentAccessKeyPair",
      requestType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest.class,
      responseType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse> getDeactivateAgentAccessKeyPairMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse> getDeactivateAgentAccessKeyPairMethod;
    if ((getDeactivateAgentAccessKeyPairMethod = ClusterConnectivityManagementV2Grpc.getDeactivateAgentAccessKeyPairMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getDeactivateAgentAccessKeyPairMethod = ClusterConnectivityManagementV2Grpc.getDeactivateAgentAccessKeyPairMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getDeactivateAgentAccessKeyPairMethod = getDeactivateAgentAccessKeyPairMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeactivateAgentAccessKeyPair"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ClusterConnectivityManagementV2MethodDescriptorSupplier("DeactivateAgentAccessKeyPair"))
              .build();
        }
      }
    }
    return getDeactivateAgentAccessKeyPairMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse> getCreateAgentAccessKeyPairMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateAgentAccessKeyPair",
      requestType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest.class,
      responseType = com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest,
      com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse> getCreateAgentAccessKeyPairMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse> getCreateAgentAccessKeyPairMethod;
    if ((getCreateAgentAccessKeyPairMethod = ClusterConnectivityManagementV2Grpc.getCreateAgentAccessKeyPairMethod) == null) {
      synchronized (ClusterConnectivityManagementV2Grpc.class) {
        if ((getCreateAgentAccessKeyPairMethod = ClusterConnectivityManagementV2Grpc.getCreateAgentAccessKeyPairMethod) == null) {
          ClusterConnectivityManagementV2Grpc.getCreateAgentAccessKeyPairMethod = getCreateAgentAccessKeyPairMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest, com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateAgentAccessKeyPair"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ClusterConnectivityManagementV2MethodDescriptorSupplier("CreateAgentAccessKeyPair"))
              .build();
        }
      }
    }
    return getCreateAgentAccessKeyPairMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ClusterConnectivityManagementV2Stub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClusterConnectivityManagementV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClusterConnectivityManagementV2Stub>() {
        @java.lang.Override
        public ClusterConnectivityManagementV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClusterConnectivityManagementV2Stub(channel, callOptions);
        }
      };
    return ClusterConnectivityManagementV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static ClusterConnectivityManagementV2BlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClusterConnectivityManagementV2BlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClusterConnectivityManagementV2BlockingV2Stub>() {
        @java.lang.Override
        public ClusterConnectivityManagementV2BlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClusterConnectivityManagementV2BlockingV2Stub(channel, callOptions);
        }
      };
    return ClusterConnectivityManagementV2BlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ClusterConnectivityManagementV2BlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClusterConnectivityManagementV2BlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClusterConnectivityManagementV2BlockingStub>() {
        @java.lang.Override
        public ClusterConnectivityManagementV2BlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClusterConnectivityManagementV2BlockingStub(channel, callOptions);
        }
      };
    return ClusterConnectivityManagementV2BlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ClusterConnectivityManagementV2FutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClusterConnectivityManagementV2FutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClusterConnectivityManagementV2FutureStub>() {
        @java.lang.Override
        public ClusterConnectivityManagementV2FutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClusterConnectivityManagementV2FutureStub(channel, callOptions);
        }
      };
    return ClusterConnectivityManagementV2FutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * ClusterConnectivityManagementV2 service for provisioning and managing inverting-proxy.
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
     * CreateOrGetInvertingProxy will create new deployment If it is not already present.
     * It also polls for the status and updates the status accordingly.
     * </pre>
     */
    default void createOrGetInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateOrGetInvertingProxyMethod(), responseObserver);
    }

    /**
     * <pre>
     * RemoveInvertingProxy will remove inverting-proxy deployment.
     * Mainly used for reaper process.
     * </pre>
     */
    default void removeInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRemoveInvertingProxyMethod(), responseObserver);
    }

    /**
     * <pre>
     * RegisterAgent for generating and registering agent key-cert pair.
     * </pre>
     */
    default void registerAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterAgentMethod(), responseObserver);
    }

    /**
     * <pre>
     * UnregisterAgent for removing agent key-cert pair while environment deletion.
     * </pre>
     */
    default void unregisterAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnregisterAgentMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists all registered agents matching a supplied query
     * </pre>
     */
    default void listAgents(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListAgentsMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetAllAgentsCertificates for getting certificates of all the agents for an account.
     * </pre>
     */
    default void getAllAgentsCertificates(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAllAgentsCertificatesMethod(), responseObserver);
    }

    /**
     * <pre>
     * RotateAgentAccessKey for rotating workload machine user key pair
     * </pre>
     */
    default void rotateAgentAccessKey(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRotateAgentAccessKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * DeactivateAgentAccessKeyPair for deleting an existing pair of access key and key id for an agent.
     * </pre>
     */
    default void deactivateAgentAccessKeyPair(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeactivateAgentAccessKeyPairMethod(), responseObserver);
    }

    /**
     * <pre>
     * CreateAgentAccessKeyPair for creating a new pair of access key and key id for an agent.
     * </pre>
     */
    default void createAgentAccessKeyPair(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateAgentAccessKeyPairMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ClusterConnectivityManagementV2.
   * <pre>
   * ClusterConnectivityManagementV2 service for provisioning and managing inverting-proxy.
   * </pre>
   */
  public static abstract class ClusterConnectivityManagementV2ImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ClusterConnectivityManagementV2Grpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ClusterConnectivityManagementV2.
   * <pre>
   * ClusterConnectivityManagementV2 service for provisioning and managing inverting-proxy.
   * </pre>
   */
  public static final class ClusterConnectivityManagementV2Stub
      extends io.grpc.stub.AbstractAsyncStub<ClusterConnectivityManagementV2Stub> {
    private ClusterConnectivityManagementV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterConnectivityManagementV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClusterConnectivityManagementV2Stub(channel, callOptions);
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
     * CreateOrGetInvertingProxy will create new deployment If it is not already present.
     * It also polls for the status and updates the status accordingly.
     * </pre>
     */
    public void createOrGetInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateOrGetInvertingProxyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * RemoveInvertingProxy will remove inverting-proxy deployment.
     * Mainly used for reaper process.
     * </pre>
     */
    public void removeInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRemoveInvertingProxyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * RegisterAgent for generating and registering agent key-cert pair.
     * </pre>
     */
    public void registerAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterAgentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * UnregisterAgent for removing agent key-cert pair while environment deletion.
     * </pre>
     */
    public void unregisterAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnregisterAgentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists all registered agents matching a supplied query
     * </pre>
     */
    public void listAgents(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListAgentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetAllAgentsCertificates for getting certificates of all the agents for an account.
     * </pre>
     */
    public void getAllAgentsCertificates(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAllAgentsCertificatesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * RotateAgentAccessKey for rotating workload machine user key pair
     * </pre>
     */
    public void rotateAgentAccessKey(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRotateAgentAccessKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * DeactivateAgentAccessKeyPair for deleting an existing pair of access key and key id for an agent.
     * </pre>
     */
    public void deactivateAgentAccessKeyPair(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeactivateAgentAccessKeyPairMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * CreateAgentAccessKeyPair for creating a new pair of access key and key id for an agent.
     * </pre>
     */
    public void createAgentAccessKeyPair(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateAgentAccessKeyPairMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ClusterConnectivityManagementV2.
   * <pre>
   * ClusterConnectivityManagementV2 service for provisioning and managing inverting-proxy.
   * </pre>
   */
  public static final class ClusterConnectivityManagementV2BlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<ClusterConnectivityManagementV2BlockingV2Stub> {
    private ClusterConnectivityManagementV2BlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterConnectivityManagementV2BlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClusterConnectivityManagementV2BlockingV2Stub(channel, callOptions);
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
     * CreateOrGetInvertingProxy will create new deployment If it is not already present.
     * It also polls for the status and updates the status accordingly.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse createOrGetInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateOrGetInvertingProxyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RemoveInvertingProxy will remove inverting-proxy deployment.
     * Mainly used for reaper process.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse removeInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRemoveInvertingProxyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RegisterAgent for generating and registering agent key-cert pair.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse registerAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRegisterAgentMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * UnregisterAgent for removing agent key-cert pair while environment deletion.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse unregisterAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUnregisterAgentMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all registered agents matching a supplied query
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse listAgents(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListAgentsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetAllAgentsCertificates for getting certificates of all the agents for an account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse getAllAgentsCertificates(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetAllAgentsCertificatesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RotateAgentAccessKey for rotating workload machine user key pair
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse rotateAgentAccessKey(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRotateAgentAccessKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeactivateAgentAccessKeyPair for deleting an existing pair of access key and key id for an agent.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse deactivateAgentAccessKeyPair(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeactivateAgentAccessKeyPairMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateAgentAccessKeyPair for creating a new pair of access key and key id for an agent.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse createAgentAccessKeyPair(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateAgentAccessKeyPairMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service ClusterConnectivityManagementV2.
   * <pre>
   * ClusterConnectivityManagementV2 service for provisioning and managing inverting-proxy.
   * </pre>
   */
  public static final class ClusterConnectivityManagementV2BlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ClusterConnectivityManagementV2BlockingStub> {
    private ClusterConnectivityManagementV2BlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterConnectivityManagementV2BlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClusterConnectivityManagementV2BlockingStub(channel, callOptions);
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
     * CreateOrGetInvertingProxy will create new deployment If it is not already present.
     * It also polls for the status and updates the status accordingly.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse createOrGetInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateOrGetInvertingProxyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RemoveInvertingProxy will remove inverting-proxy deployment.
     * Mainly used for reaper process.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse removeInvertingProxy(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRemoveInvertingProxyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RegisterAgent for generating and registering agent key-cert pair.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse registerAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterAgentMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * UnregisterAgent for removing agent key-cert pair while environment deletion.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse unregisterAgent(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnregisterAgentMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all registered agents matching a supplied query
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse listAgents(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListAgentsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetAllAgentsCertificates for getting certificates of all the agents for an account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse getAllAgentsCertificates(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAllAgentsCertificatesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RotateAgentAccessKey for rotating workload machine user key pair
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse rotateAgentAccessKey(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRotateAgentAccessKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeactivateAgentAccessKeyPair for deleting an existing pair of access key and key id for an agent.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse deactivateAgentAccessKeyPair(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeactivateAgentAccessKeyPairMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateAgentAccessKeyPair for creating a new pair of access key and key id for an agent.
     * </pre>
     */
    public com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse createAgentAccessKeyPair(com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateAgentAccessKeyPairMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ClusterConnectivityManagementV2.
   * <pre>
   * ClusterConnectivityManagementV2 service for provisioning and managing inverting-proxy.
   * </pre>
   */
  public static final class ClusterConnectivityManagementV2FutureStub
      extends io.grpc.stub.AbstractFutureStub<ClusterConnectivityManagementV2FutureStub> {
    private ClusterConnectivityManagementV2FutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterConnectivityManagementV2FutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClusterConnectivityManagementV2FutureStub(channel, callOptions);
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
     * CreateOrGetInvertingProxy will create new deployment If it is not already present.
     * It also polls for the status and updates the status accordingly.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> createOrGetInvertingProxy(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateOrGetInvertingProxyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * RemoveInvertingProxy will remove inverting-proxy deployment.
     * Mainly used for reaper process.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse> removeInvertingProxy(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRemoveInvertingProxyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * RegisterAgent for generating and registering agent key-cert pair.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse> registerAgent(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterAgentMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * UnregisterAgent for removing agent key-cert pair while environment deletion.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> unregisterAgent(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnregisterAgentMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists all registered agents matching a supplied query
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse> listAgents(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListAgentsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetAllAgentsCertificates for getting certificates of all the agents for an account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse> getAllAgentsCertificates(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAllAgentsCertificatesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * RotateAgentAccessKey for rotating workload machine user key pair
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse> rotateAgentAccessKey(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRotateAgentAccessKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * DeactivateAgentAccessKeyPair for deleting an existing pair of access key and key id for an agent.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse> deactivateAgentAccessKeyPair(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeactivateAgentAccessKeyPairMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * CreateAgentAccessKeyPair for creating a new pair of access key and key id for an agent.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse> createAgentAccessKeyPair(
        com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateAgentAccessKeyPairMethod(), getCallOptions()), request);
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
  private static final int METHODID_DEACTIVATE_AGENT_ACCESS_KEY_PAIR = 8;
  private static final int METHODID_CREATE_AGENT_ACCESS_KEY_PAIR = 9;

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
        case METHODID_DEACTIVATE_AGENT_ACCESS_KEY_PAIR:
          serviceImpl.deactivateAgentAccessKeyPair((com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse>) responseObserver);
          break;
        case METHODID_CREATE_AGENT_ACCESS_KEY_PAIR:
          serviceImpl.createAgentAccessKeyPair((com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse>) responseObserver);
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
          getCreateOrGetInvertingProxyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest,
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse>(
                service, METHODID_CREATE_OR_GET_INVERTING_PROXY)))
        .addMethod(
          getRemoveInvertingProxyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyRequest,
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RemoveInvertingProxyResponse>(
                service, METHODID_REMOVE_INVERTING_PROXY)))
        .addMethod(
          getRegisterAgentMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentRequest,
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RegisterAgentResponse>(
                service, METHODID_REGISTER_AGENT)))
        .addMethod(
          getUnregisterAgentMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentRequest,
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse>(
                service, METHODID_UNREGISTER_AGENT)))
        .addMethod(
          getListAgentsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsRequest,
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.ListAgentsResponse>(
                service, METHODID_LIST_AGENTS)))
        .addMethod(
          getGetAllAgentsCertificatesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesRequest,
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.GetAllAgentsCertificatesResponse>(
                service, METHODID_GET_ALL_AGENTS_CERTIFICATES)))
        .addMethod(
          getRotateAgentAccessKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyRequest,
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.RotateAgentAccessKeyResponse>(
                service, METHODID_ROTATE_AGENT_ACCESS_KEY)))
        .addMethod(
          getDeactivateAgentAccessKeyPairMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest,
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse>(
                service, METHODID_DEACTIVATE_AGENT_ACCESS_KEY_PAIR)))
        .addMethod(
          getCreateAgentAccessKeyPairMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest,
              com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse>(
                service, METHODID_CREATE_AGENT_ACCESS_KEY_PAIR)))
        .build();
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
    private final java.lang.String methodName;

    ClusterConnectivityManagementV2MethodDescriptorSupplier(java.lang.String methodName) {
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
              .addMethod(getGetVersionMethod())
              .addMethod(getCreateOrGetInvertingProxyMethod())
              .addMethod(getRemoveInvertingProxyMethod())
              .addMethod(getRegisterAgentMethod())
              .addMethod(getUnregisterAgentMethod())
              .addMethod(getListAgentsMethod())
              .addMethod(getGetAllAgentsCertificatesMethod())
              .addMethod(getRotateAgentAccessKeyMethod())
              .addMethod(getDeactivateAgentAccessKeyPairMethod())
              .addMethod(getCreateAgentAccessKeyPairMethod())
              .build();
        }
      }
    }
    return result;
  }
}
