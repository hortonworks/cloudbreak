package com.cloudera.thunderhead.service.sdxsvcadmin;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Cloudera :: Defining sdxsvcAdmin API service
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.53.0)",
    comments = "Source: sdxsvcadmin.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class SDXSvcAdminGrpc {

  private SDXSvcAdminGrpc() {}

  public static final String SERVICE_NAME = "sdxsvcadmin.SDXSvcAdmin";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsResponse> getListSupportedCloudRegionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListSupportedCloudRegions",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsResponse> getListSupportedCloudRegionsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsResponse> getListSupportedCloudRegionsMethod;
    if ((getListSupportedCloudRegionsMethod = SDXSvcAdminGrpc.getListSupportedCloudRegionsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getListSupportedCloudRegionsMethod = SDXSvcAdminGrpc.getListSupportedCloudRegionsMethod) == null) {
          SDXSvcAdminGrpc.getListSupportedCloudRegionsMethod = getListSupportedCloudRegionsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListSupportedCloudRegions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("ListSupportedCloudRegions"))
              .build();
        }
      }
    }
    return getListSupportedCloudRegionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsResponse> getListSupportedCloudPlatformsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListSupportedCloudPlatforms",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsResponse> getListSupportedCloudPlatformsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsResponse> getListSupportedCloudPlatformsMethod;
    if ((getListSupportedCloudPlatformsMethod = SDXSvcAdminGrpc.getListSupportedCloudPlatformsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getListSupportedCloudPlatformsMethod = SDXSvcAdminGrpc.getListSupportedCloudPlatformsMethod) == null) {
          SDXSvcAdminGrpc.getListSupportedCloudPlatformsMethod = getListSupportedCloudPlatformsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListSupportedCloudPlatforms"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("ListSupportedCloudPlatforms"))
              .build();
        }
      }
    }
    return getListSupportedCloudPlatformsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse> getAddServiceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AddService",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse> getAddServiceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse> getAddServiceMethod;
    if ((getAddServiceMethod = SDXSvcAdminGrpc.getAddServiceMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getAddServiceMethod = SDXSvcAdminGrpc.getAddServiceMethod) == null) {
          SDXSvcAdminGrpc.getAddServiceMethod = getAddServiceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AddService"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("AddService"))
              .build();
        }
      }
    }
    return getAddServiceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse> getRemoveServiceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RemoveService",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse> getRemoveServiceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse> getRemoveServiceMethod;
    if ((getRemoveServiceMethod = SDXSvcAdminGrpc.getRemoveServiceMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRemoveServiceMethod = SDXSvcAdminGrpc.getRemoveServiceMethod) == null) {
          SDXSvcAdminGrpc.getRemoveServiceMethod = getRemoveServiceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RemoveService"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RemoveService"))
              .build();
        }
      }
    }
    return getRemoveServiceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse> getDescribeServiceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeService",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse> getDescribeServiceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse> getDescribeServiceMethod;
    if ((getDescribeServiceMethod = SDXSvcAdminGrpc.getDescribeServiceMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getDescribeServiceMethod = SDXSvcAdminGrpc.getDescribeServiceMethod) == null) {
          SDXSvcAdminGrpc.getDescribeServiceMethod = getDescribeServiceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeService"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("DescribeService"))
              .build();
        }
      }
    }
    return getDescribeServiceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse> getRequestServiceRestartMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RequestServiceRestart",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse> getRequestServiceRestartMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse> getRequestServiceRestartMethod;
    if ((getRequestServiceRestartMethod = SDXSvcAdminGrpc.getRequestServiceRestartMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRequestServiceRestartMethod = SDXSvcAdminGrpc.getRequestServiceRestartMethod) == null) {
          SDXSvcAdminGrpc.getRequestServiceRestartMethod = getRequestServiceRestartMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RequestServiceRestart"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RequestServiceRestart"))
              .build();
        }
      }
    }
    return getRequestServiceRestartMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse> getRequestServiceSuspendMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RequestServiceSuspend",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse> getRequestServiceSuspendMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse> getRequestServiceSuspendMethod;
    if ((getRequestServiceSuspendMethod = SDXSvcAdminGrpc.getRequestServiceSuspendMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRequestServiceSuspendMethod = SDXSvcAdminGrpc.getRequestServiceSuspendMethod) == null) {
          SDXSvcAdminGrpc.getRequestServiceSuspendMethod = getRequestServiceSuspendMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RequestServiceSuspend"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RequestServiceSuspend"))
              .build();
        }
      }
    }
    return getRequestServiceSuspendMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse> getRequestServiceStartMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RequestServiceStart",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse> getRequestServiceStartMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse> getRequestServiceStartMethod;
    if ((getRequestServiceStartMethod = SDXSvcAdminGrpc.getRequestServiceStartMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRequestServiceStartMethod = SDXSvcAdminGrpc.getRequestServiceStartMethod) == null) {
          SDXSvcAdminGrpc.getRequestServiceStartMethod = getRequestServiceStartMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RequestServiceStart"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RequestServiceStart"))
              .build();
        }
      }
    }
    return getRequestServiceStartMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse> getRequestServiceUpgradeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RequestServiceUpgrade",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse> getRequestServiceUpgradeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse> getRequestServiceUpgradeMethod;
    if ((getRequestServiceUpgradeMethod = SDXSvcAdminGrpc.getRequestServiceUpgradeMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRequestServiceUpgradeMethod = SDXSvcAdminGrpc.getRequestServiceUpgradeMethod) == null) {
          SDXSvcAdminGrpc.getRequestServiceUpgradeMethod = getRequestServiceUpgradeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RequestServiceUpgrade"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RequestServiceUpgrade"))
              .build();
        }
      }
    }
    return getRequestServiceUpgradeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestResponse> getCheckServiceRequestMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CheckServiceRequest",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestResponse> getCheckServiceRequestMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestResponse> getCheckServiceRequestMethod;
    if ((getCheckServiceRequestMethod = SDXSvcAdminGrpc.getCheckServiceRequestMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getCheckServiceRequestMethod = SDXSvcAdminGrpc.getCheckServiceRequestMethod) == null) {
          SDXSvcAdminGrpc.getCheckServiceRequestMethod = getCheckServiceRequestMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CheckServiceRequest"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("CheckServiceRequest"))
              .build();
        }
      }
    }
    return getCheckServiceRequestMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse> getListServiceVersionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListServiceVersions",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse> getListServiceVersionsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse> getListServiceVersionsMethod;
    if ((getListServiceVersionsMethod = SDXSvcAdminGrpc.getListServiceVersionsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getListServiceVersionsMethod = SDXSvcAdminGrpc.getListServiceVersionsMethod) == null) {
          SDXSvcAdminGrpc.getListServiceVersionsMethod = getListServiceVersionsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListServiceVersions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("ListServiceVersions"))
              .build();
        }
      }
    }
    return getListServiceVersionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse> getCreateInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateInstance",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse> getCreateInstanceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse> getCreateInstanceMethod;
    if ((getCreateInstanceMethod = SDXSvcAdminGrpc.getCreateInstanceMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getCreateInstanceMethod = SDXSvcAdminGrpc.getCreateInstanceMethod) == null) {
          SDXSvcAdminGrpc.getCreateInstanceMethod = getCreateInstanceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateInstance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("CreateInstance"))
              .build();
        }
      }
    }
    return getCreateInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse> getRequestMoveInstanceToOperationalEnvironmentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RequestMoveInstanceToOperationalEnvironment",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse> getRequestMoveInstanceToOperationalEnvironmentMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse> getRequestMoveInstanceToOperationalEnvironmentMethod;
    if ((getRequestMoveInstanceToOperationalEnvironmentMethod = SDXSvcAdminGrpc.getRequestMoveInstanceToOperationalEnvironmentMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRequestMoveInstanceToOperationalEnvironmentMethod = SDXSvcAdminGrpc.getRequestMoveInstanceToOperationalEnvironmentMethod) == null) {
          SDXSvcAdminGrpc.getRequestMoveInstanceToOperationalEnvironmentMethod = getRequestMoveInstanceToOperationalEnvironmentMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RequestMoveInstanceToOperationalEnvironment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RequestMoveInstanceToOperationalEnvironment"))
              .build();
        }
      }
    }
    return getRequestMoveInstanceToOperationalEnvironmentMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceResponse> getPairEnvironmentToInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PairEnvironmentToInstance",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceResponse> getPairEnvironmentToInstanceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceResponse> getPairEnvironmentToInstanceMethod;
    if ((getPairEnvironmentToInstanceMethod = SDXSvcAdminGrpc.getPairEnvironmentToInstanceMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getPairEnvironmentToInstanceMethod = SDXSvcAdminGrpc.getPairEnvironmentToInstanceMethod) == null) {
          SDXSvcAdminGrpc.getPairEnvironmentToInstanceMethod = getPairEnvironmentToInstanceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PairEnvironmentToInstance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("PairEnvironmentToInstance"))
              .build();
        }
      }
    }
    return getPairEnvironmentToInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceResponse> getUnpairEnvironmentFromInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnpairEnvironmentFromInstance",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceResponse> getUnpairEnvironmentFromInstanceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceResponse> getUnpairEnvironmentFromInstanceMethod;
    if ((getUnpairEnvironmentFromInstanceMethod = SDXSvcAdminGrpc.getUnpairEnvironmentFromInstanceMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getUnpairEnvironmentFromInstanceMethod = SDXSvcAdminGrpc.getUnpairEnvironmentFromInstanceMethod) == null) {
          SDXSvcAdminGrpc.getUnpairEnvironmentFromInstanceMethod = getUnpairEnvironmentFromInstanceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnpairEnvironmentFromInstance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("UnpairEnvironmentFromInstance"))
              .build();
        }
      }
    }
    return getUnpairEnvironmentFromInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceResponse> getDeleteInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteInstance",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceResponse> getDeleteInstanceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceResponse> getDeleteInstanceMethod;
    if ((getDeleteInstanceMethod = SDXSvcAdminGrpc.getDeleteInstanceMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getDeleteInstanceMethod = SDXSvcAdminGrpc.getDeleteInstanceMethod) == null) {
          SDXSvcAdminGrpc.getDeleteInstanceMethod = getDeleteInstanceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteInstance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("DeleteInstance"))
              .build();
        }
      }
    }
    return getDeleteInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceResponse> getDescribeInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeInstance",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceResponse> getDescribeInstanceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceResponse> getDescribeInstanceMethod;
    if ((getDescribeInstanceMethod = SDXSvcAdminGrpc.getDescribeInstanceMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getDescribeInstanceMethod = SDXSvcAdminGrpc.getDescribeInstanceMethod) == null) {
          SDXSvcAdminGrpc.getDescribeInstanceMethod = getDescribeInstanceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeInstance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("DescribeInstance"))
              .build();
        }
      }
    }
    return getDescribeInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesResponse> getListInstancesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListInstances",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesResponse> getListInstancesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesResponse> getListInstancesMethod;
    if ((getListInstancesMethod = SDXSvcAdminGrpc.getListInstancesMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getListInstancesMethod = SDXSvcAdminGrpc.getListInstancesMethod) == null) {
          SDXSvcAdminGrpc.getListInstancesMethod = getListInstancesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListInstances"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("ListInstances"))
              .build();
        }
      }
    }
    return getListInstancesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesResponse> getFindInstancesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FindInstances",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesResponse> getFindInstancesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesResponse> getFindInstancesMethod;
    if ((getFindInstancesMethod = SDXSvcAdminGrpc.getFindInstancesMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getFindInstancesMethod = SDXSvcAdminGrpc.getFindInstancesMethod) == null) {
          SDXSvcAdminGrpc.getFindInstancesMethod = getFindInstancesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FindInstances"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("FindInstances"))
              .build();
        }
      }
    }
    return getFindInstancesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopResponse> getRequestInstanceStopMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RequestInstanceStop",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopResponse> getRequestInstanceStopMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopResponse> getRequestInstanceStopMethod;
    if ((getRequestInstanceStopMethod = SDXSvcAdminGrpc.getRequestInstanceStopMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRequestInstanceStopMethod = SDXSvcAdminGrpc.getRequestInstanceStopMethod) == null) {
          SDXSvcAdminGrpc.getRequestInstanceStopMethod = getRequestInstanceStopMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RequestInstanceStop"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RequestInstanceStop"))
              .build();
        }
      }
    }
    return getRequestInstanceStopMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartResponse> getRequestInstanceStartMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RequestInstanceStart",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartResponse> getRequestInstanceStartMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartResponse> getRequestInstanceStartMethod;
    if ((getRequestInstanceStartMethod = SDXSvcAdminGrpc.getRequestInstanceStartMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRequestInstanceStartMethod = SDXSvcAdminGrpc.getRequestInstanceStartMethod) == null) {
          SDXSvcAdminGrpc.getRequestInstanceStartMethod = getRequestInstanceStartMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RequestInstanceStart"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RequestInstanceStart"))
              .build();
        }
      }
    }
    return getRequestInstanceStartMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartResponse> getRequestInstanceRestartMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RequestInstanceRestart",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartResponse> getRequestInstanceRestartMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartResponse> getRequestInstanceRestartMethod;
    if ((getRequestInstanceRestartMethod = SDXSvcAdminGrpc.getRequestInstanceRestartMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRequestInstanceRestartMethod = SDXSvcAdminGrpc.getRequestInstanceRestartMethod) == null) {
          SDXSvcAdminGrpc.getRequestInstanceRestartMethod = getRequestInstanceRestartMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RequestInstanceRestart"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RequestInstanceRestart"))
              .build();
        }
      }
    }
    return getRequestInstanceRestartMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestResponse> getCheckInstanceRequestMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CheckInstanceRequest",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestResponse> getCheckInstanceRequestMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestResponse> getCheckInstanceRequestMethod;
    if ((getCheckInstanceRequestMethod = SDXSvcAdminGrpc.getCheckInstanceRequestMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getCheckInstanceRequestMethod = SDXSvcAdminGrpc.getCheckInstanceRequestMethod) == null) {
          SDXSvcAdminGrpc.getCheckInstanceRequestMethod = getCheckInstanceRequestMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CheckInstanceRequest"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("CheckInstanceRequest"))
              .build();
        }
      }
    }
    return getCheckInstanceRequestMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionResponse> getCreateAwsPrivatelinkConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateAwsPrivatelinkConnection",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionResponse> getCreateAwsPrivatelinkConnectionMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionResponse> getCreateAwsPrivatelinkConnectionMethod;
    if ((getCreateAwsPrivatelinkConnectionMethod = SDXSvcAdminGrpc.getCreateAwsPrivatelinkConnectionMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getCreateAwsPrivatelinkConnectionMethod = SDXSvcAdminGrpc.getCreateAwsPrivatelinkConnectionMethod) == null) {
          SDXSvcAdminGrpc.getCreateAwsPrivatelinkConnectionMethod = getCreateAwsPrivatelinkConnectionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateAwsPrivatelinkConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("CreateAwsPrivatelinkConnection"))
              .build();
        }
      }
    }
    return getCreateAwsPrivatelinkConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseResponse> getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateAwsPrivatelinkConnectionForHmsDatabase",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseResponse> getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseResponse> getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod;
    if ((getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod = SDXSvcAdminGrpc.getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod = SDXSvcAdminGrpc.getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod) == null) {
          SDXSvcAdminGrpc.getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod = getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateAwsPrivatelinkConnectionForHmsDatabase"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("CreateAwsPrivatelinkConnectionForHmsDatabase"))
              .build();
        }
      }
    }
    return getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionResponse> getDeleteConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteConnection",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionResponse> getDeleteConnectionMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionResponse> getDeleteConnectionMethod;
    if ((getDeleteConnectionMethod = SDXSvcAdminGrpc.getDeleteConnectionMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getDeleteConnectionMethod = SDXSvcAdminGrpc.getDeleteConnectionMethod) == null) {
          SDXSvcAdminGrpc.getDeleteConnectionMethod = getDeleteConnectionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("DeleteConnection"))
              .build();
        }
      }
    }
    return getDeleteConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionResponse> getDescribeConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeConnection",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionResponse> getDescribeConnectionMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionResponse> getDescribeConnectionMethod;
    if ((getDescribeConnectionMethod = SDXSvcAdminGrpc.getDescribeConnectionMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getDescribeConnectionMethod = SDXSvcAdminGrpc.getDescribeConnectionMethod) == null) {
          SDXSvcAdminGrpc.getDescribeConnectionMethod = getDescribeConnectionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("DescribeConnection"))
              .build();
        }
      }
    }
    return getDescribeConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsResponse> getListConnectionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListConnections",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsResponse> getListConnectionsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsResponse> getListConnectionsMethod;
    if ((getListConnectionsMethod = SDXSvcAdminGrpc.getListConnectionsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getListConnectionsMethod = SDXSvcAdminGrpc.getListConnectionsMethod) == null) {
          SDXSvcAdminGrpc.getListConnectionsMethod = getListConnectionsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListConnections"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("ListConnections"))
              .build();
        }
      }
    }
    return getListConnectionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentResponse> getRegisterOperationalEnvironmentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterOperationalEnvironment",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentResponse> getRegisterOperationalEnvironmentMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentResponse> getRegisterOperationalEnvironmentMethod;
    if ((getRegisterOperationalEnvironmentMethod = SDXSvcAdminGrpc.getRegisterOperationalEnvironmentMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRegisterOperationalEnvironmentMethod = SDXSvcAdminGrpc.getRegisterOperationalEnvironmentMethod) == null) {
          SDXSvcAdminGrpc.getRegisterOperationalEnvironmentMethod = getRegisterOperationalEnvironmentMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterOperationalEnvironment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RegisterOperationalEnvironment"))
              .build();
        }
      }
    }
    return getRegisterOperationalEnvironmentMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentResponse> getUnregisterOperationalEnvironmentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnregisterOperationalEnvironment",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentResponse> getUnregisterOperationalEnvironmentMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentResponse> getUnregisterOperationalEnvironmentMethod;
    if ((getUnregisterOperationalEnvironmentMethod = SDXSvcAdminGrpc.getUnregisterOperationalEnvironmentMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getUnregisterOperationalEnvironmentMethod = SDXSvcAdminGrpc.getUnregisterOperationalEnvironmentMethod) == null) {
          SDXSvcAdminGrpc.getUnregisterOperationalEnvironmentMethod = getUnregisterOperationalEnvironmentMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnregisterOperationalEnvironment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("UnregisterOperationalEnvironment"))
              .build();
        }
      }
    }
    return getUnregisterOperationalEnvironmentMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentResponse> getDescribeOperationalEnvironmentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeOperationalEnvironment",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentResponse> getDescribeOperationalEnvironmentMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentResponse> getDescribeOperationalEnvironmentMethod;
    if ((getDescribeOperationalEnvironmentMethod = SDXSvcAdminGrpc.getDescribeOperationalEnvironmentMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getDescribeOperationalEnvironmentMethod = SDXSvcAdminGrpc.getDescribeOperationalEnvironmentMethod) == null) {
          SDXSvcAdminGrpc.getDescribeOperationalEnvironmentMethod = getDescribeOperationalEnvironmentMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeOperationalEnvironment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("DescribeOperationalEnvironment"))
              .build();
        }
      }
    }
    return getDescribeOperationalEnvironmentMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsResponse> getListOperationalEnvironmentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListOperationalEnvironments",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsResponse> getListOperationalEnvironmentsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsResponse> getListOperationalEnvironmentsMethod;
    if ((getListOperationalEnvironmentsMethod = SDXSvcAdminGrpc.getListOperationalEnvironmentsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getListOperationalEnvironmentsMethod = SDXSvcAdminGrpc.getListOperationalEnvironmentsMethod) == null) {
          SDXSvcAdminGrpc.getListOperationalEnvironmentsMethod = getListOperationalEnvironmentsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListOperationalEnvironments"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("ListOperationalEnvironments"))
              .build();
        }
      }
    }
    return getListOperationalEnvironmentsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsResponse> getFindOperationalEnvironmentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FindOperationalEnvironments",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsResponse> getFindOperationalEnvironmentsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsResponse> getFindOperationalEnvironmentsMethod;
    if ((getFindOperationalEnvironmentsMethod = SDXSvcAdminGrpc.getFindOperationalEnvironmentsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getFindOperationalEnvironmentsMethod = SDXSvcAdminGrpc.getFindOperationalEnvironmentsMethod) == null) {
          SDXSvcAdminGrpc.getFindOperationalEnvironmentsMethod = getFindOperationalEnvironmentsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FindOperationalEnvironments"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("FindOperationalEnvironments"))
              .build();
        }
      }
    }
    return getFindOperationalEnvironmentsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityResponse> getSetOperationalEnvironmentAssignabilityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetOperationalEnvironmentAssignability",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityResponse> getSetOperationalEnvironmentAssignabilityMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityResponse> getSetOperationalEnvironmentAssignabilityMethod;
    if ((getSetOperationalEnvironmentAssignabilityMethod = SDXSvcAdminGrpc.getSetOperationalEnvironmentAssignabilityMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getSetOperationalEnvironmentAssignabilityMethod = SDXSvcAdminGrpc.getSetOperationalEnvironmentAssignabilityMethod) == null) {
          SDXSvcAdminGrpc.getSetOperationalEnvironmentAssignabilityMethod = getSetOperationalEnvironmentAssignabilityMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetOperationalEnvironmentAssignability"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("SetOperationalEnvironmentAssignability"))
              .build();
        }
      }
    }
    return getSetOperationalEnvironmentAssignabilityMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerResponse> getRegisterAwsOpenSearchIndexerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterAwsOpenSearchIndexer",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerResponse> getRegisterAwsOpenSearchIndexerMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerResponse> getRegisterAwsOpenSearchIndexerMethod;
    if ((getRegisterAwsOpenSearchIndexerMethod = SDXSvcAdminGrpc.getRegisterAwsOpenSearchIndexerMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRegisterAwsOpenSearchIndexerMethod = SDXSvcAdminGrpc.getRegisterAwsOpenSearchIndexerMethod) == null) {
          SDXSvcAdminGrpc.getRegisterAwsOpenSearchIndexerMethod = getRegisterAwsOpenSearchIndexerMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterAwsOpenSearchIndexer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RegisterAwsOpenSearchIndexer"))
              .build();
        }
      }
    }
    return getRegisterAwsOpenSearchIndexerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerResponse> getUnregisterIndexerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnregisterIndexer",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerResponse> getUnregisterIndexerMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerResponse> getUnregisterIndexerMethod;
    if ((getUnregisterIndexerMethod = SDXSvcAdminGrpc.getUnregisterIndexerMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getUnregisterIndexerMethod = SDXSvcAdminGrpc.getUnregisterIndexerMethod) == null) {
          SDXSvcAdminGrpc.getUnregisterIndexerMethod = getUnregisterIndexerMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnregisterIndexer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("UnregisterIndexer"))
              .build();
        }
      }
    }
    return getUnregisterIndexerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerResponse> getDescribeIndexerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeIndexer",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerResponse> getDescribeIndexerMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerResponse> getDescribeIndexerMethod;
    if ((getDescribeIndexerMethod = SDXSvcAdminGrpc.getDescribeIndexerMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getDescribeIndexerMethod = SDXSvcAdminGrpc.getDescribeIndexerMethod) == null) {
          SDXSvcAdminGrpc.getDescribeIndexerMethod = getDescribeIndexerMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeIndexer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("DescribeIndexer"))
              .build();
        }
      }
    }
    return getDescribeIndexerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersResponse> getListIndexersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListIndexers",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersResponse> getListIndexersMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersResponse> getListIndexersMethod;
    if ((getListIndexersMethod = SDXSvcAdminGrpc.getListIndexersMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getListIndexersMethod = SDXSvcAdminGrpc.getListIndexersMethod) == null) {
          SDXSvcAdminGrpc.getListIndexersMethod = getListIndexersMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListIndexers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("ListIndexers"))
              .build();
        }
      }
    }
    return getListIndexersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersResponse> getFindIndexersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FindIndexers",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersResponse> getFindIndexersMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersResponse> getFindIndexersMethod;
    if ((getFindIndexersMethod = SDXSvcAdminGrpc.getFindIndexersMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getFindIndexersMethod = SDXSvcAdminGrpc.getFindIndexersMethod) == null) {
          SDXSvcAdminGrpc.getFindIndexersMethod = getFindIndexersMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FindIndexers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("FindIndexers"))
              .build();
        }
      }
    }
    return getFindIndexersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityResponse> getSetIndexerAssignabilityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetIndexerAssignability",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityResponse> getSetIndexerAssignabilityMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityResponse> getSetIndexerAssignabilityMethod;
    if ((getSetIndexerAssignabilityMethod = SDXSvcAdminGrpc.getSetIndexerAssignabilityMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getSetIndexerAssignabilityMethod = SDXSvcAdminGrpc.getSetIndexerAssignabilityMethod) == null) {
          SDXSvcAdminGrpc.getSetIndexerAssignabilityMethod = getSetIndexerAssignabilityMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetIndexerAssignability"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("SetIndexerAssignability"))
              .build();
        }
      }
    }
    return getSetIndexerAssignabilityMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse> getRegisterAwsObjectStorageRootMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterAwsObjectStorageRoot",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse> getRegisterAwsObjectStorageRootMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse> getRegisterAwsObjectStorageRootMethod;
    if ((getRegisterAwsObjectStorageRootMethod = SDXSvcAdminGrpc.getRegisterAwsObjectStorageRootMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRegisterAwsObjectStorageRootMethod = SDXSvcAdminGrpc.getRegisterAwsObjectStorageRootMethod) == null) {
          SDXSvcAdminGrpc.getRegisterAwsObjectStorageRootMethod = getRegisterAwsObjectStorageRootMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterAwsObjectStorageRoot"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RegisterAwsObjectStorageRoot"))
              .build();
        }
      }
    }
    return getRegisterAwsObjectStorageRootMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootResponse> getUnregisterStorageRootMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnregisterStorageRoot",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootResponse> getUnregisterStorageRootMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootResponse> getUnregisterStorageRootMethod;
    if ((getUnregisterStorageRootMethod = SDXSvcAdminGrpc.getUnregisterStorageRootMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getUnregisterStorageRootMethod = SDXSvcAdminGrpc.getUnregisterStorageRootMethod) == null) {
          SDXSvcAdminGrpc.getUnregisterStorageRootMethod = getUnregisterStorageRootMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnregisterStorageRoot"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("UnregisterStorageRoot"))
              .build();
        }
      }
    }
    return getUnregisterStorageRootMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootResponse> getDescribeStorageRootMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeStorageRoot",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootResponse> getDescribeStorageRootMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootResponse> getDescribeStorageRootMethod;
    if ((getDescribeStorageRootMethod = SDXSvcAdminGrpc.getDescribeStorageRootMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getDescribeStorageRootMethod = SDXSvcAdminGrpc.getDescribeStorageRootMethod) == null) {
          SDXSvcAdminGrpc.getDescribeStorageRootMethod = getDescribeStorageRootMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeStorageRoot"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("DescribeStorageRoot"))
              .build();
        }
      }
    }
    return getDescribeStorageRootMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsResponse> getListStorageRootsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListStorageRoots",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsResponse> getListStorageRootsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsResponse> getListStorageRootsMethod;
    if ((getListStorageRootsMethod = SDXSvcAdminGrpc.getListStorageRootsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getListStorageRootsMethod = SDXSvcAdminGrpc.getListStorageRootsMethod) == null) {
          SDXSvcAdminGrpc.getListStorageRootsMethod = getListStorageRootsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListStorageRoots"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("ListStorageRoots"))
              .build();
        }
      }
    }
    return getListStorageRootsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsResponse> getFindStorageRootsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FindStorageRoots",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsResponse> getFindStorageRootsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsResponse> getFindStorageRootsMethod;
    if ((getFindStorageRootsMethod = SDXSvcAdminGrpc.getFindStorageRootsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getFindStorageRootsMethod = SDXSvcAdminGrpc.getFindStorageRootsMethod) == null) {
          SDXSvcAdminGrpc.getFindStorageRootsMethod = getFindStorageRootsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FindStorageRoots"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("FindStorageRoots"))
              .build();
        }
      }
    }
    return getFindStorageRootsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityResponse> getSetStorageRootAssignabilityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetStorageRootAssignability",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityResponse> getSetStorageRootAssignabilityMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityResponse> getSetStorageRootAssignabilityMethod;
    if ((getSetStorageRootAssignabilityMethod = SDXSvcAdminGrpc.getSetStorageRootAssignabilityMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getSetStorageRootAssignabilityMethod = SDXSvcAdminGrpc.getSetStorageRootAssignabilityMethod) == null) {
          SDXSvcAdminGrpc.getSetStorageRootAssignabilityMethod = getSetStorageRootAssignabilityMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetStorageRootAssignability"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("SetStorageRootAssignability"))
              .build();
        }
      }
    }
    return getSetStorageRootAssignabilityMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse> getRegisterAwsRdbmsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterAwsRdbms",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse> getRegisterAwsRdbmsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse> getRegisterAwsRdbmsMethod;
    if ((getRegisterAwsRdbmsMethod = SDXSvcAdminGrpc.getRegisterAwsRdbmsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRegisterAwsRdbmsMethod = SDXSvcAdminGrpc.getRegisterAwsRdbmsMethod) == null) {
          SDXSvcAdminGrpc.getRegisterAwsRdbmsMethod = getRegisterAwsRdbmsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterAwsRdbms"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RegisterAwsRdbms"))
              .build();
        }
      }
    }
    return getRegisterAwsRdbmsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsResponse> getUnregisterRdbmsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnregisterRdbms",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsResponse> getUnregisterRdbmsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsResponse> getUnregisterRdbmsMethod;
    if ((getUnregisterRdbmsMethod = SDXSvcAdminGrpc.getUnregisterRdbmsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getUnregisterRdbmsMethod = SDXSvcAdminGrpc.getUnregisterRdbmsMethod) == null) {
          SDXSvcAdminGrpc.getUnregisterRdbmsMethod = getUnregisterRdbmsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnregisterRdbms"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("UnregisterRdbms"))
              .build();
        }
      }
    }
    return getUnregisterRdbmsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsResponse> getDescribeRdbmsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeRdbms",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsResponse> getDescribeRdbmsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsResponse> getDescribeRdbmsMethod;
    if ((getDescribeRdbmsMethod = SDXSvcAdminGrpc.getDescribeRdbmsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getDescribeRdbmsMethod = SDXSvcAdminGrpc.getDescribeRdbmsMethod) == null) {
          SDXSvcAdminGrpc.getDescribeRdbmsMethod = getDescribeRdbmsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeRdbms"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("DescribeRdbms"))
              .build();
        }
      }
    }
    return getDescribeRdbmsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsResponse> getFindRdbmsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FindRdbms",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsResponse> getFindRdbmsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsResponse> getFindRdbmsMethod;
    if ((getFindRdbmsMethod = SDXSvcAdminGrpc.getFindRdbmsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getFindRdbmsMethod = SDXSvcAdminGrpc.getFindRdbmsMethod) == null) {
          SDXSvcAdminGrpc.getFindRdbmsMethod = getFindRdbmsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FindRdbms"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("FindRdbms"))
              .build();
        }
      }
    }
    return getFindRdbmsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsResponse> getListRdbmsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListRdbms",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsResponse> getListRdbmsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsResponse> getListRdbmsMethod;
    if ((getListRdbmsMethod = SDXSvcAdminGrpc.getListRdbmsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getListRdbmsMethod = SDXSvcAdminGrpc.getListRdbmsMethod) == null) {
          SDXSvcAdminGrpc.getListRdbmsMethod = getListRdbmsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListRdbms"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("ListRdbms"))
              .build();
        }
      }
    }
    return getListRdbmsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityResponse> getSetRdbmsAssignabilityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetRdbmsAssignability",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityResponse> getSetRdbmsAssignabilityMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityResponse> getSetRdbmsAssignabilityMethod;
    if ((getSetRdbmsAssignabilityMethod = SDXSvcAdminGrpc.getSetRdbmsAssignabilityMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getSetRdbmsAssignabilityMethod = SDXSvcAdminGrpc.getSetRdbmsAssignabilityMethod) == null) {
          SDXSvcAdminGrpc.getSetRdbmsAssignabilityMethod = getSetRdbmsAssignabilityMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetRdbmsAssignability"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("SetRdbmsAssignability"))
              .build();
        }
      }
    }
    return getSetRdbmsAssignabilityMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SDXSvcAdminStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SDXSvcAdminStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SDXSvcAdminStub>() {
        @java.lang.Override
        public SDXSvcAdminStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SDXSvcAdminStub(channel, callOptions);
        }
      };
    return SDXSvcAdminStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SDXSvcAdminBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SDXSvcAdminBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SDXSvcAdminBlockingStub>() {
        @java.lang.Override
        public SDXSvcAdminBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SDXSvcAdminBlockingStub(channel, callOptions);
        }
      };
    return SDXSvcAdminBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SDXSvcAdminFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SDXSvcAdminFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SDXSvcAdminFutureStub>() {
        @java.lang.Override
        public SDXSvcAdminFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SDXSvcAdminFutureStub(channel, callOptions);
        }
      };
    return SDXSvcAdminFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Cloudera :: Defining sdxsvcAdmin API service
   * </pre>
   */
  public static abstract class SDXSvcAdminImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * List the currently supported Cloud Regions. :: List the currently supported Cloud Regions.
     * </pre>
     */
    public void listSupportedCloudRegions(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListSupportedCloudRegionsMethod(), responseObserver);
    }

    /**
     * <pre>
     * List the currently supported Cloud Platforms. :: List the currently supported Cloud Platforms.
     * </pre>
     */
    public void listSupportedCloudPlatforms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListSupportedCloudPlatformsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Add an SDX Service instance to a running SDX Instance. :: Add an SDX Service instance to a running SDX Instance.
     * </pre>
     */
    public void addService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAddServiceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Remove an SDX Service instance from an SDX Instance. :: Remove an SDX Service instance from an SDX Instance.
     * </pre>
     */
    public void removeService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRemoveServiceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe an SDX Service instance in an SDX Instance. :: Describe an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public void describeService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeServiceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Restart an SDX Service instance in an SDX Instance. :: Restart an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public void requestServiceRestart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestServiceRestartMethod(), responseObserver);
    }

    /**
     * <pre>
     * Suspend an SDX Service instance in an SDX Instance. :: Suspend an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public void requestServiceSuspend(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestServiceSuspendMethod(), responseObserver);
    }

    /**
     * <pre>
     * Start an SDX Service instance in an SDX Instance. :: Start an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public void requestServiceStart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestServiceStartMethod(), responseObserver);
    }

    /**
     * <pre>
     * Upgrade an SDX Service instance in an SDX Instance. :: Upgrade an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public void requestServiceUpgrade(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestServiceUpgradeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Check a request on an SDX Service instance. :: Check a request on an SDX Service instance.
     * </pre>
     */
    public void checkServiceRequest(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckServiceRequestMethod(), responseObserver);
    }

    /**
     * <pre>
     * List all available versions of an SDX Service. :: List all available versions of an SDX Service.
     * </pre>
     */
    public void listServiceVersions(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListServiceVersionsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create an SDX Instance. :: Create an SDX Instance.
     * </pre>
     */
    public void createInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateInstanceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Request to move the SDX Instance to a different Operational Environment. :: Request to move the SDX Instance to a different Operational Environment.
     * </pre>
     */
    public void requestMoveInstanceToOperationalEnvironment(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestMoveInstanceToOperationalEnvironmentMethod(), responseObserver);
    }

    /**
     * <pre>
     * Pair a CDP Environment to an SDX Instance. :: Pair a CDP Environment to an SDX Instance.
     * </pre>
     */
    public void pairEnvironmentToInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPairEnvironmentToInstanceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unpair a CDP Environment from an SDX Instance. :: Unpair a CDP Environment from an SDX Instance.
     * </pre>
     */
    public void unpairEnvironmentFromInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnpairEnvironmentFromInstanceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete an SDX Instance. :: Delete an SDX Instance.
     * </pre>
     */
    public void deleteInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteInstanceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe an SDX Instance. :: Describe an SDX Instance.
     * </pre>
     */
    public void describeInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeInstanceMethod(), responseObserver);
    }

    /**
     * <pre>
     * List all SDX Instances. :: List all SDX Instances.
     * </pre>
     */
    public void listInstances(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListInstancesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Find SDX Instances based on criteria. :: Find SDX Instances based on criteria.
     * </pre>
     */
    public void findInstances(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFindInstancesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Request the stop of all SDX Services in an SDX Instance. :: Request the stop of all SDX Services in an SDX Instance.
     * </pre>
     */
    public void requestInstanceStop(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestInstanceStopMethod(), responseObserver);
    }

    /**
     * <pre>
     * Request the start of all SDX Services in an SDX Instance. :: Request the start of all SDX Services in an SDX Instance.
     * </pre>
     */
    public void requestInstanceStart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestInstanceStartMethod(), responseObserver);
    }

    /**
     * <pre>
     * Request the restart of all SDX Services in an SDX Instance. :: Request the restart of all SDX Services in an SDX Instance.
     * </pre>
     */
    public void requestInstanceRestart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestInstanceRestartMethod(), responseObserver);
    }

    /**
     * <pre>
     * Check a request on an SDX Instance.  :: Check a request on an SDX Instance.
     * </pre>
     */
    public void checkInstanceRequest(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckInstanceRequestMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create an AWS PrivateLink Connection. :: Create a connection to SDX as a Service via AWS PrivateLink.
     * </pre>
     */
    public void createAwsPrivatelinkConnection(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateAwsPrivatelinkConnectionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create an AWS PrivateLink Connection for the Hms Database. :: Create a connection to SDX as a Service via AWS PrivateLink.  This is a CDPaaS feature only and will not be used for PaaS.
     * </pre>
     */
    public void createAwsPrivatelinkConnectionForHmsDatabase(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete an SDX Connection. :: Delete an SDX Connection.
     * </pre>
     */
    public void deleteConnection(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteConnectionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe an SDX Connection. :: Describe an SDX Connection.
     * </pre>
     */
    public void describeConnection(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeConnectionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists all SDX Connections. :: Lists all SDX Connections.
     * </pre>
     */
    public void listConnections(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListConnectionsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Register an Operational Environment with the SDX Resource Manager. :: Register an Operational Environment with the SDX Resource Manager.
     * </pre>
     */
    public void registerOperationalEnvironment(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterOperationalEnvironmentMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unregister an Operational Environment from the SDX Resource Manager. :: Unregister an Operational Environment from the SDX Resource Manager.
     * </pre>
     */
    public void unregisterOperationalEnvironment(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnregisterOperationalEnvironmentMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe an Operational Environment. :: Describe an Operational Environment.
     * </pre>
     */
    public void describeOperationalEnvironment(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeOperationalEnvironmentMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists the registered Operational Environments. :: Lists the registered Operation Environments.
     * </pre>
     */
    public void listOperationalEnvironments(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListOperationalEnvironmentsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Find the registered Operational Environments based on criteria. :: Find the registered Operational Environments based on criteria.
     * </pre>
     */
    public void findOperationalEnvironments(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFindOperationalEnvironmentsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Set the assignability flag of an Operational Environment. :: Set the assignability flag of an Operational Environment.
     * </pre>
     */
    public void setOperationalEnvironmentAssignability(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetOperationalEnvironmentAssignabilityMethod(), responseObserver);
    }

    /**
     * <pre>
     * Register an AWS OpenSearch instance as a shared indexer. :: Register an AWS OpenSearch instance as a shared indexer.
     * </pre>
     */
    public void registerAwsOpenSearchIndexer(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterAwsOpenSearchIndexerMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unregister an Indexer instance as a shared indexer. :: Unregister an Indexer instance as a shared indexer.
     * </pre>
     */
    public void unregisterIndexer(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnregisterIndexerMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe an Indexer instance. :: Describe an Indexer instance.
     * </pre>
     */
    public void describeIndexer(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeIndexerMethod(), responseObserver);
    }

    /**
     * <pre>
     * List Indexer instances.  :: List Indexer instances.
     * </pre>
     */
    public void listIndexers(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListIndexersMethod(), responseObserver);
    }

    /**
     * <pre>
     * Find Indexer instances based on criteria. :: Find Indexer instances based on criteria.
     * </pre>
     */
    public void findIndexers(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFindIndexersMethod(), responseObserver);
    }

    /**
     * <pre>
     * Set the assignability flag of a shared Indexer. :: Set the assignability flag of a shared Indexer.
     * </pre>
     */
    public void setIndexerAssignability(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetIndexerAssignabilityMethod(), responseObserver);
    }

    /**
     * <pre>
     * Register an AWS Object Storage root. :: Register an AWS Object Storage root.
     * </pre>
     */
    public void registerAwsObjectStorageRoot(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterAwsObjectStorageRootMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unregister a Storage Root. :: Unregister a Storage Root.
     * </pre>
     */
    public void unregisterStorageRoot(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnregisterStorageRootMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe a Storage Root instance. :: Describe a Storage Root instance.
     * </pre>
     */
    public void describeStorageRoot(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeStorageRootMethod(), responseObserver);
    }

    /**
     * <pre>
     * List Storage Root instances.  :: List Storage Root instances.
     * </pre>
     */
    public void listStorageRoots(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListStorageRootsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Find Storage Root instances based on criteria. :: Find Storage Root instances based on criteria.
     * </pre>
     */
    public void findStorageRoots(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFindStorageRootsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Set the assignability flag of a Storage Root. :: Set the assignability flag of a Storage Root.
     * </pre>
     */
    public void setStorageRootAssignability(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetStorageRootAssignabilityMethod(), responseObserver);
    }

    /**
     * <pre>
     * Register an AWS RDBMS as a shared Database Storage Provider. :: Register an AWS RDBMS as a shared Database Storage Provider.
     * </pre>
     */
    public void registerAwsRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterAwsRdbmsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unregister a RDBMS as a shared Database Storage Provider. :: Unregister a RDBMS as a shared Database Storage Provider.
     * </pre>
     */
    public void unregisterRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnregisterRdbmsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe a registered RDBMS instance. :: Describe a registered RDBMS instance.
     * </pre>
     */
    public void describeRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeRdbmsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Find the registered RDBMS instances based on criteria. :: Find the registered RDBMS instances based on criteria.
     * </pre>
     */
    public void findRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFindRdbmsMethod(), responseObserver);
    }

    /**
     * <pre>
     * List the registered RDBMS instances. :: List the registered RDBMS instances.
     * </pre>
     */
    public void listRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListRdbmsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Set the assignability flag of a shared RDBMS instance. :: Set the assignability flag of a shared RDBMS instance.
     * </pre>
     */
    public void setRdbmsAssignability(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetRdbmsAssignabilityMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getListSupportedCloudRegionsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsResponse>(
                  this, METHODID_LIST_SUPPORTED_CLOUD_REGIONS)))
          .addMethod(
            getListSupportedCloudPlatformsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsResponse>(
                  this, METHODID_LIST_SUPPORTED_CLOUD_PLATFORMS)))
          .addMethod(
            getAddServiceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse>(
                  this, METHODID_ADD_SERVICE)))
          .addMethod(
            getRemoveServiceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse>(
                  this, METHODID_REMOVE_SERVICE)))
          .addMethod(
            getDescribeServiceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse>(
                  this, METHODID_DESCRIBE_SERVICE)))
          .addMethod(
            getRequestServiceRestartMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse>(
                  this, METHODID_REQUEST_SERVICE_RESTART)))
          .addMethod(
            getRequestServiceSuspendMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse>(
                  this, METHODID_REQUEST_SERVICE_SUSPEND)))
          .addMethod(
            getRequestServiceStartMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse>(
                  this, METHODID_REQUEST_SERVICE_START)))
          .addMethod(
            getRequestServiceUpgradeMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse>(
                  this, METHODID_REQUEST_SERVICE_UPGRADE)))
          .addMethod(
            getCheckServiceRequestMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestResponse>(
                  this, METHODID_CHECK_SERVICE_REQUEST)))
          .addMethod(
            getListServiceVersionsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse>(
                  this, METHODID_LIST_SERVICE_VERSIONS)))
          .addMethod(
            getCreateInstanceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse>(
                  this, METHODID_CREATE_INSTANCE)))
          .addMethod(
            getRequestMoveInstanceToOperationalEnvironmentMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse>(
                  this, METHODID_REQUEST_MOVE_INSTANCE_TO_OPERATIONAL_ENVIRONMENT)))
          .addMethod(
            getPairEnvironmentToInstanceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceResponse>(
                  this, METHODID_PAIR_ENVIRONMENT_TO_INSTANCE)))
          .addMethod(
            getUnpairEnvironmentFromInstanceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceResponse>(
                  this, METHODID_UNPAIR_ENVIRONMENT_FROM_INSTANCE)))
          .addMethod(
            getDeleteInstanceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceResponse>(
                  this, METHODID_DELETE_INSTANCE)))
          .addMethod(
            getDescribeInstanceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceResponse>(
                  this, METHODID_DESCRIBE_INSTANCE)))
          .addMethod(
            getListInstancesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesResponse>(
                  this, METHODID_LIST_INSTANCES)))
          .addMethod(
            getFindInstancesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesResponse>(
                  this, METHODID_FIND_INSTANCES)))
          .addMethod(
            getRequestInstanceStopMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopResponse>(
                  this, METHODID_REQUEST_INSTANCE_STOP)))
          .addMethod(
            getRequestInstanceStartMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartResponse>(
                  this, METHODID_REQUEST_INSTANCE_START)))
          .addMethod(
            getRequestInstanceRestartMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartResponse>(
                  this, METHODID_REQUEST_INSTANCE_RESTART)))
          .addMethod(
            getCheckInstanceRequestMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestResponse>(
                  this, METHODID_CHECK_INSTANCE_REQUEST)))
          .addMethod(
            getCreateAwsPrivatelinkConnectionMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionResponse>(
                  this, METHODID_CREATE_AWS_PRIVATELINK_CONNECTION)))
          .addMethod(
            getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseResponse>(
                  this, METHODID_CREATE_AWS_PRIVATELINK_CONNECTION_FOR_HMS_DATABASE)))
          .addMethod(
            getDeleteConnectionMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionResponse>(
                  this, METHODID_DELETE_CONNECTION)))
          .addMethod(
            getDescribeConnectionMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionResponse>(
                  this, METHODID_DESCRIBE_CONNECTION)))
          .addMethod(
            getListConnectionsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsResponse>(
                  this, METHODID_LIST_CONNECTIONS)))
          .addMethod(
            getRegisterOperationalEnvironmentMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentResponse>(
                  this, METHODID_REGISTER_OPERATIONAL_ENVIRONMENT)))
          .addMethod(
            getUnregisterOperationalEnvironmentMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentResponse>(
                  this, METHODID_UNREGISTER_OPERATIONAL_ENVIRONMENT)))
          .addMethod(
            getDescribeOperationalEnvironmentMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentResponse>(
                  this, METHODID_DESCRIBE_OPERATIONAL_ENVIRONMENT)))
          .addMethod(
            getListOperationalEnvironmentsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsResponse>(
                  this, METHODID_LIST_OPERATIONAL_ENVIRONMENTS)))
          .addMethod(
            getFindOperationalEnvironmentsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsResponse>(
                  this, METHODID_FIND_OPERATIONAL_ENVIRONMENTS)))
          .addMethod(
            getSetOperationalEnvironmentAssignabilityMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityResponse>(
                  this, METHODID_SET_OPERATIONAL_ENVIRONMENT_ASSIGNABILITY)))
          .addMethod(
            getRegisterAwsOpenSearchIndexerMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerResponse>(
                  this, METHODID_REGISTER_AWS_OPEN_SEARCH_INDEXER)))
          .addMethod(
            getUnregisterIndexerMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerResponse>(
                  this, METHODID_UNREGISTER_INDEXER)))
          .addMethod(
            getDescribeIndexerMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerResponse>(
                  this, METHODID_DESCRIBE_INDEXER)))
          .addMethod(
            getListIndexersMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersResponse>(
                  this, METHODID_LIST_INDEXERS)))
          .addMethod(
            getFindIndexersMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersResponse>(
                  this, METHODID_FIND_INDEXERS)))
          .addMethod(
            getSetIndexerAssignabilityMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityResponse>(
                  this, METHODID_SET_INDEXER_ASSIGNABILITY)))
          .addMethod(
            getRegisterAwsObjectStorageRootMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse>(
                  this, METHODID_REGISTER_AWS_OBJECT_STORAGE_ROOT)))
          .addMethod(
            getUnregisterStorageRootMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootResponse>(
                  this, METHODID_UNREGISTER_STORAGE_ROOT)))
          .addMethod(
            getDescribeStorageRootMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootResponse>(
                  this, METHODID_DESCRIBE_STORAGE_ROOT)))
          .addMethod(
            getListStorageRootsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsResponse>(
                  this, METHODID_LIST_STORAGE_ROOTS)))
          .addMethod(
            getFindStorageRootsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsResponse>(
                  this, METHODID_FIND_STORAGE_ROOTS)))
          .addMethod(
            getSetStorageRootAssignabilityMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityResponse>(
                  this, METHODID_SET_STORAGE_ROOT_ASSIGNABILITY)))
          .addMethod(
            getRegisterAwsRdbmsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse>(
                  this, METHODID_REGISTER_AWS_RDBMS)))
          .addMethod(
            getUnregisterRdbmsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsResponse>(
                  this, METHODID_UNREGISTER_RDBMS)))
          .addMethod(
            getDescribeRdbmsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsResponse>(
                  this, METHODID_DESCRIBE_RDBMS)))
          .addMethod(
            getFindRdbmsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsResponse>(
                  this, METHODID_FIND_RDBMS)))
          .addMethod(
            getListRdbmsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsResponse>(
                  this, METHODID_LIST_RDBMS)))
          .addMethod(
            getSetRdbmsAssignabilityMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityResponse>(
                  this, METHODID_SET_RDBMS_ASSIGNABILITY)))
          .build();
    }
  }

  /**
   * <pre>
   * Cloudera :: Defining sdxsvcAdmin API service
   * </pre>
   */
  public static final class SDXSvcAdminStub extends io.grpc.stub.AbstractAsyncStub<SDXSvcAdminStub> {
    private SDXSvcAdminStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SDXSvcAdminStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SDXSvcAdminStub(channel, callOptions);
    }

    /**
     * <pre>
     * List the currently supported Cloud Regions. :: List the currently supported Cloud Regions.
     * </pre>
     */
    public void listSupportedCloudRegions(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListSupportedCloudRegionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List the currently supported Cloud Platforms. :: List the currently supported Cloud Platforms.
     * </pre>
     */
    public void listSupportedCloudPlatforms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListSupportedCloudPlatformsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Add an SDX Service instance to a running SDX Instance. :: Add an SDX Service instance to a running SDX Instance.
     * </pre>
     */
    public void addService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAddServiceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Remove an SDX Service instance from an SDX Instance. :: Remove an SDX Service instance from an SDX Instance.
     * </pre>
     */
    public void removeService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRemoveServiceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe an SDX Service instance in an SDX Instance. :: Describe an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public void describeService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeServiceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Restart an SDX Service instance in an SDX Instance. :: Restart an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public void requestServiceRestart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestServiceRestartMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Suspend an SDX Service instance in an SDX Instance. :: Suspend an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public void requestServiceSuspend(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestServiceSuspendMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Start an SDX Service instance in an SDX Instance. :: Start an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public void requestServiceStart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestServiceStartMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Upgrade an SDX Service instance in an SDX Instance. :: Upgrade an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public void requestServiceUpgrade(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestServiceUpgradeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Check a request on an SDX Service instance. :: Check a request on an SDX Service instance.
     * </pre>
     */
    public void checkServiceRequest(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCheckServiceRequestMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List all available versions of an SDX Service. :: List all available versions of an SDX Service.
     * </pre>
     */
    public void listServiceVersions(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListServiceVersionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create an SDX Instance. :: Create an SDX Instance.
     * </pre>
     */
    public void createInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateInstanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Request to move the SDX Instance to a different Operational Environment. :: Request to move the SDX Instance to a different Operational Environment.
     * </pre>
     */
    public void requestMoveInstanceToOperationalEnvironment(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestMoveInstanceToOperationalEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Pair a CDP Environment to an SDX Instance. :: Pair a CDP Environment to an SDX Instance.
     * </pre>
     */
    public void pairEnvironmentToInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPairEnvironmentToInstanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unpair a CDP Environment from an SDX Instance. :: Unpair a CDP Environment from an SDX Instance.
     * </pre>
     */
    public void unpairEnvironmentFromInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnpairEnvironmentFromInstanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete an SDX Instance. :: Delete an SDX Instance.
     * </pre>
     */
    public void deleteInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteInstanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe an SDX Instance. :: Describe an SDX Instance.
     * </pre>
     */
    public void describeInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeInstanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List all SDX Instances. :: List all SDX Instances.
     * </pre>
     */
    public void listInstances(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListInstancesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Find SDX Instances based on criteria. :: Find SDX Instances based on criteria.
     * </pre>
     */
    public void findInstances(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFindInstancesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Request the stop of all SDX Services in an SDX Instance. :: Request the stop of all SDX Services in an SDX Instance.
     * </pre>
     */
    public void requestInstanceStop(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestInstanceStopMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Request the start of all SDX Services in an SDX Instance. :: Request the start of all SDX Services in an SDX Instance.
     * </pre>
     */
    public void requestInstanceStart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestInstanceStartMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Request the restart of all SDX Services in an SDX Instance. :: Request the restart of all SDX Services in an SDX Instance.
     * </pre>
     */
    public void requestInstanceRestart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestInstanceRestartMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Check a request on an SDX Instance.  :: Check a request on an SDX Instance.
     * </pre>
     */
    public void checkInstanceRequest(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCheckInstanceRequestMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create an AWS PrivateLink Connection. :: Create a connection to SDX as a Service via AWS PrivateLink.
     * </pre>
     */
    public void createAwsPrivatelinkConnection(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateAwsPrivatelinkConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create an AWS PrivateLink Connection for the Hms Database. :: Create a connection to SDX as a Service via AWS PrivateLink.  This is a CDPaaS feature only and will not be used for PaaS.
     * </pre>
     */
    public void createAwsPrivatelinkConnectionForHmsDatabase(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete an SDX Connection. :: Delete an SDX Connection.
     * </pre>
     */
    public void deleteConnection(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe an SDX Connection. :: Describe an SDX Connection.
     * </pre>
     */
    public void describeConnection(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists all SDX Connections. :: Lists all SDX Connections.
     * </pre>
     */
    public void listConnections(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListConnectionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Register an Operational Environment with the SDX Resource Manager. :: Register an Operational Environment with the SDX Resource Manager.
     * </pre>
     */
    public void registerOperationalEnvironment(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterOperationalEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unregister an Operational Environment from the SDX Resource Manager. :: Unregister an Operational Environment from the SDX Resource Manager.
     * </pre>
     */
    public void unregisterOperationalEnvironment(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnregisterOperationalEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe an Operational Environment. :: Describe an Operational Environment.
     * </pre>
     */
    public void describeOperationalEnvironment(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeOperationalEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists the registered Operational Environments. :: Lists the registered Operation Environments.
     * </pre>
     */
    public void listOperationalEnvironments(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListOperationalEnvironmentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Find the registered Operational Environments based on criteria. :: Find the registered Operational Environments based on criteria.
     * </pre>
     */
    public void findOperationalEnvironments(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFindOperationalEnvironmentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set the assignability flag of an Operational Environment. :: Set the assignability flag of an Operational Environment.
     * </pre>
     */
    public void setOperationalEnvironmentAssignability(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetOperationalEnvironmentAssignabilityMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Register an AWS OpenSearch instance as a shared indexer. :: Register an AWS OpenSearch instance as a shared indexer.
     * </pre>
     */
    public void registerAwsOpenSearchIndexer(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterAwsOpenSearchIndexerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unregister an Indexer instance as a shared indexer. :: Unregister an Indexer instance as a shared indexer.
     * </pre>
     */
    public void unregisterIndexer(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnregisterIndexerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe an Indexer instance. :: Describe an Indexer instance.
     * </pre>
     */
    public void describeIndexer(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeIndexerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List Indexer instances.  :: List Indexer instances.
     * </pre>
     */
    public void listIndexers(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListIndexersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Find Indexer instances based on criteria. :: Find Indexer instances based on criteria.
     * </pre>
     */
    public void findIndexers(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFindIndexersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set the assignability flag of a shared Indexer. :: Set the assignability flag of a shared Indexer.
     * </pre>
     */
    public void setIndexerAssignability(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetIndexerAssignabilityMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Register an AWS Object Storage root. :: Register an AWS Object Storage root.
     * </pre>
     */
    public void registerAwsObjectStorageRoot(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterAwsObjectStorageRootMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unregister a Storage Root. :: Unregister a Storage Root.
     * </pre>
     */
    public void unregisterStorageRoot(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnregisterStorageRootMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe a Storage Root instance. :: Describe a Storage Root instance.
     * </pre>
     */
    public void describeStorageRoot(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeStorageRootMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List Storage Root instances.  :: List Storage Root instances.
     * </pre>
     */
    public void listStorageRoots(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListStorageRootsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Find Storage Root instances based on criteria. :: Find Storage Root instances based on criteria.
     * </pre>
     */
    public void findStorageRoots(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFindStorageRootsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set the assignability flag of a Storage Root. :: Set the assignability flag of a Storage Root.
     * </pre>
     */
    public void setStorageRootAssignability(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetStorageRootAssignabilityMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Register an AWS RDBMS as a shared Database Storage Provider. :: Register an AWS RDBMS as a shared Database Storage Provider.
     * </pre>
     */
    public void registerAwsRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterAwsRdbmsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unregister a RDBMS as a shared Database Storage Provider. :: Unregister a RDBMS as a shared Database Storage Provider.
     * </pre>
     */
    public void unregisterRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnregisterRdbmsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe a registered RDBMS instance. :: Describe a registered RDBMS instance.
     * </pre>
     */
    public void describeRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeRdbmsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Find the registered RDBMS instances based on criteria. :: Find the registered RDBMS instances based on criteria.
     * </pre>
     */
    public void findRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFindRdbmsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List the registered RDBMS instances. :: List the registered RDBMS instances.
     * </pre>
     */
    public void listRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListRdbmsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set the assignability flag of a shared RDBMS instance. :: Set the assignability flag of a shared RDBMS instance.
     * </pre>
     */
    public void setRdbmsAssignability(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetRdbmsAssignabilityMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Cloudera :: Defining sdxsvcAdmin API service
   * </pre>
   */
  public static final class SDXSvcAdminBlockingStub extends io.grpc.stub.AbstractBlockingStub<SDXSvcAdminBlockingStub> {
    private SDXSvcAdminBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SDXSvcAdminBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SDXSvcAdminBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * List the currently supported Cloud Regions. :: List the currently supported Cloud Regions.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsResponse listSupportedCloudRegions(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListSupportedCloudRegionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List the currently supported Cloud Platforms. :: List the currently supported Cloud Platforms.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsResponse listSupportedCloudPlatforms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListSupportedCloudPlatformsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Add an SDX Service instance to a running SDX Instance. :: Add an SDX Service instance to a running SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse addService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAddServiceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Remove an SDX Service instance from an SDX Instance. :: Remove an SDX Service instance from an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse removeService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRemoveServiceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe an SDX Service instance in an SDX Instance. :: Describe an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse describeService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeServiceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Restart an SDX Service instance in an SDX Instance. :: Restart an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse requestServiceRestart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestServiceRestartMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Suspend an SDX Service instance in an SDX Instance. :: Suspend an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse requestServiceSuspend(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestServiceSuspendMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Start an SDX Service instance in an SDX Instance. :: Start an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse requestServiceStart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestServiceStartMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Upgrade an SDX Service instance in an SDX Instance. :: Upgrade an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse requestServiceUpgrade(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestServiceUpgradeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Check a request on an SDX Service instance. :: Check a request on an SDX Service instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestResponse checkServiceRequest(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCheckServiceRequestMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List all available versions of an SDX Service. :: List all available versions of an SDX Service.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse listServiceVersions(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListServiceVersionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create an SDX Instance. :: Create an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse createInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateInstanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Request to move the SDX Instance to a different Operational Environment. :: Request to move the SDX Instance to a different Operational Environment.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse requestMoveInstanceToOperationalEnvironment(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestMoveInstanceToOperationalEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Pair a CDP Environment to an SDX Instance. :: Pair a CDP Environment to an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceResponse pairEnvironmentToInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPairEnvironmentToInstanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unpair a CDP Environment from an SDX Instance. :: Unpair a CDP Environment from an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceResponse unpairEnvironmentFromInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnpairEnvironmentFromInstanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete an SDX Instance. :: Delete an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceResponse deleteInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteInstanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe an SDX Instance. :: Describe an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceResponse describeInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeInstanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List all SDX Instances. :: List all SDX Instances.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesResponse listInstances(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListInstancesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Find SDX Instances based on criteria. :: Find SDX Instances based on criteria.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesResponse findInstances(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFindInstancesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Request the stop of all SDX Services in an SDX Instance. :: Request the stop of all SDX Services in an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopResponse requestInstanceStop(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestInstanceStopMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Request the start of all SDX Services in an SDX Instance. :: Request the start of all SDX Services in an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartResponse requestInstanceStart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestInstanceStartMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Request the restart of all SDX Services in an SDX Instance. :: Request the restart of all SDX Services in an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartResponse requestInstanceRestart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestInstanceRestartMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Check a request on an SDX Instance.  :: Check a request on an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestResponse checkInstanceRequest(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCheckInstanceRequestMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create an AWS PrivateLink Connection. :: Create a connection to SDX as a Service via AWS PrivateLink.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionResponse createAwsPrivatelinkConnection(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateAwsPrivatelinkConnectionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create an AWS PrivateLink Connection for the Hms Database. :: Create a connection to SDX as a Service via AWS PrivateLink.  This is a CDPaaS feature only and will not be used for PaaS.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseResponse createAwsPrivatelinkConnectionForHmsDatabase(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete an SDX Connection. :: Delete an SDX Connection.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionResponse deleteConnection(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteConnectionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe an SDX Connection. :: Describe an SDX Connection.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionResponse describeConnection(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeConnectionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all SDX Connections. :: Lists all SDX Connections.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsResponse listConnections(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListConnectionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Register an Operational Environment with the SDX Resource Manager. :: Register an Operational Environment with the SDX Resource Manager.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentResponse registerOperationalEnvironment(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterOperationalEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unregister an Operational Environment from the SDX Resource Manager. :: Unregister an Operational Environment from the SDX Resource Manager.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentResponse unregisterOperationalEnvironment(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnregisterOperationalEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe an Operational Environment. :: Describe an Operational Environment.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentResponse describeOperationalEnvironment(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeOperationalEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists the registered Operational Environments. :: Lists the registered Operation Environments.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsResponse listOperationalEnvironments(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListOperationalEnvironmentsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Find the registered Operational Environments based on criteria. :: Find the registered Operational Environments based on criteria.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsResponse findOperationalEnvironments(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFindOperationalEnvironmentsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set the assignability flag of an Operational Environment. :: Set the assignability flag of an Operational Environment.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityResponse setOperationalEnvironmentAssignability(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetOperationalEnvironmentAssignabilityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Register an AWS OpenSearch instance as a shared indexer. :: Register an AWS OpenSearch instance as a shared indexer.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerResponse registerAwsOpenSearchIndexer(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterAwsOpenSearchIndexerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unregister an Indexer instance as a shared indexer. :: Unregister an Indexer instance as a shared indexer.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerResponse unregisterIndexer(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnregisterIndexerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe an Indexer instance. :: Describe an Indexer instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerResponse describeIndexer(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeIndexerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List Indexer instances.  :: List Indexer instances.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersResponse listIndexers(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListIndexersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Find Indexer instances based on criteria. :: Find Indexer instances based on criteria.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersResponse findIndexers(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFindIndexersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set the assignability flag of a shared Indexer. :: Set the assignability flag of a shared Indexer.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityResponse setIndexerAssignability(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetIndexerAssignabilityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Register an AWS Object Storage root. :: Register an AWS Object Storage root.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse registerAwsObjectStorageRoot(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterAwsObjectStorageRootMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unregister a Storage Root. :: Unregister a Storage Root.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootResponse unregisterStorageRoot(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnregisterStorageRootMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe a Storage Root instance. :: Describe a Storage Root instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootResponse describeStorageRoot(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeStorageRootMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List Storage Root instances.  :: List Storage Root instances.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsResponse listStorageRoots(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListStorageRootsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Find Storage Root instances based on criteria. :: Find Storage Root instances based on criteria.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsResponse findStorageRoots(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFindStorageRootsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set the assignability flag of a Storage Root. :: Set the assignability flag of a Storage Root.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityResponse setStorageRootAssignability(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetStorageRootAssignabilityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Register an AWS RDBMS as a shared Database Storage Provider. :: Register an AWS RDBMS as a shared Database Storage Provider.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse registerAwsRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterAwsRdbmsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unregister a RDBMS as a shared Database Storage Provider. :: Unregister a RDBMS as a shared Database Storage Provider.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsResponse unregisterRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnregisterRdbmsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe a registered RDBMS instance. :: Describe a registered RDBMS instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsResponse describeRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeRdbmsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Find the registered RDBMS instances based on criteria. :: Find the registered RDBMS instances based on criteria.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsResponse findRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFindRdbmsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List the registered RDBMS instances. :: List the registered RDBMS instances.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsResponse listRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListRdbmsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set the assignability flag of a shared RDBMS instance. :: Set the assignability flag of a shared RDBMS instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityResponse setRdbmsAssignability(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetRdbmsAssignabilityMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Cloudera :: Defining sdxsvcAdmin API service
   * </pre>
   */
  public static final class SDXSvcAdminFutureStub extends io.grpc.stub.AbstractFutureStub<SDXSvcAdminFutureStub> {
    private SDXSvcAdminFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SDXSvcAdminFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SDXSvcAdminFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * List the currently supported Cloud Regions. :: List the currently supported Cloud Regions.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsResponse> listSupportedCloudRegions(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListSupportedCloudRegionsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List the currently supported Cloud Platforms. :: List the currently supported Cloud Platforms.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsResponse> listSupportedCloudPlatforms(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListSupportedCloudPlatformsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Add an SDX Service instance to a running SDX Instance. :: Add an SDX Service instance to a running SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse> addService(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAddServiceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Remove an SDX Service instance from an SDX Instance. :: Remove an SDX Service instance from an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse> removeService(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRemoveServiceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe an SDX Service instance in an SDX Instance. :: Describe an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse> describeService(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeServiceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Restart an SDX Service instance in an SDX Instance. :: Restart an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse> requestServiceRestart(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestServiceRestartMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Suspend an SDX Service instance in an SDX Instance. :: Suspend an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse> requestServiceSuspend(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestServiceSuspendMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Start an SDX Service instance in an SDX Instance. :: Start an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse> requestServiceStart(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestServiceStartMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Upgrade an SDX Service instance in an SDX Instance. :: Upgrade an SDX Service instance in an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse> requestServiceUpgrade(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestServiceUpgradeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Check a request on an SDX Service instance. :: Check a request on an SDX Service instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestResponse> checkServiceRequest(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCheckServiceRequestMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List all available versions of an SDX Service. :: List all available versions of an SDX Service.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse> listServiceVersions(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListServiceVersionsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create an SDX Instance. :: Create an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse> createInstance(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateInstanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Request to move the SDX Instance to a different Operational Environment. :: Request to move the SDX Instance to a different Operational Environment.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse> requestMoveInstanceToOperationalEnvironment(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestMoveInstanceToOperationalEnvironmentMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Pair a CDP Environment to an SDX Instance. :: Pair a CDP Environment to an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceResponse> pairEnvironmentToInstance(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPairEnvironmentToInstanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unpair a CDP Environment from an SDX Instance. :: Unpair a CDP Environment from an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceResponse> unpairEnvironmentFromInstance(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnpairEnvironmentFromInstanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete an SDX Instance. :: Delete an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceResponse> deleteInstance(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteInstanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe an SDX Instance. :: Describe an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceResponse> describeInstance(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeInstanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List all SDX Instances. :: List all SDX Instances.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesResponse> listInstances(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListInstancesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Find SDX Instances based on criteria. :: Find SDX Instances based on criteria.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesResponse> findInstances(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFindInstancesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Request the stop of all SDX Services in an SDX Instance. :: Request the stop of all SDX Services in an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopResponse> requestInstanceStop(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestInstanceStopMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Request the start of all SDX Services in an SDX Instance. :: Request the start of all SDX Services in an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartResponse> requestInstanceStart(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestInstanceStartMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Request the restart of all SDX Services in an SDX Instance. :: Request the restart of all SDX Services in an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartResponse> requestInstanceRestart(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestInstanceRestartMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Check a request on an SDX Instance.  :: Check a request on an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestResponse> checkInstanceRequest(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCheckInstanceRequestMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create an AWS PrivateLink Connection. :: Create a connection to SDX as a Service via AWS PrivateLink.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionResponse> createAwsPrivatelinkConnection(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateAwsPrivatelinkConnectionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create an AWS PrivateLink Connection for the Hms Database. :: Create a connection to SDX as a Service via AWS PrivateLink.  This is a CDPaaS feature only and will not be used for PaaS.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseResponse> createAwsPrivatelinkConnectionForHmsDatabase(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete an SDX Connection. :: Delete an SDX Connection.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionResponse> deleteConnection(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteConnectionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe an SDX Connection. :: Describe an SDX Connection.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionResponse> describeConnection(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeConnectionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists all SDX Connections. :: Lists all SDX Connections.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsResponse> listConnections(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListConnectionsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Register an Operational Environment with the SDX Resource Manager. :: Register an Operational Environment with the SDX Resource Manager.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentResponse> registerOperationalEnvironment(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterOperationalEnvironmentMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unregister an Operational Environment from the SDX Resource Manager. :: Unregister an Operational Environment from the SDX Resource Manager.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentResponse> unregisterOperationalEnvironment(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnregisterOperationalEnvironmentMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe an Operational Environment. :: Describe an Operational Environment.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentResponse> describeOperationalEnvironment(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeOperationalEnvironmentMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists the registered Operational Environments. :: Lists the registered Operation Environments.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsResponse> listOperationalEnvironments(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListOperationalEnvironmentsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Find the registered Operational Environments based on criteria. :: Find the registered Operational Environments based on criteria.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsResponse> findOperationalEnvironments(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFindOperationalEnvironmentsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set the assignability flag of an Operational Environment. :: Set the assignability flag of an Operational Environment.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityResponse> setOperationalEnvironmentAssignability(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetOperationalEnvironmentAssignabilityMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Register an AWS OpenSearch instance as a shared indexer. :: Register an AWS OpenSearch instance as a shared indexer.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerResponse> registerAwsOpenSearchIndexer(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterAwsOpenSearchIndexerMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unregister an Indexer instance as a shared indexer. :: Unregister an Indexer instance as a shared indexer.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerResponse> unregisterIndexer(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnregisterIndexerMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe an Indexer instance. :: Describe an Indexer instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerResponse> describeIndexer(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeIndexerMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List Indexer instances.  :: List Indexer instances.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersResponse> listIndexers(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListIndexersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Find Indexer instances based on criteria. :: Find Indexer instances based on criteria.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersResponse> findIndexers(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFindIndexersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set the assignability flag of a shared Indexer. :: Set the assignability flag of a shared Indexer.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityResponse> setIndexerAssignability(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetIndexerAssignabilityMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Register an AWS Object Storage root. :: Register an AWS Object Storage root.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse> registerAwsObjectStorageRoot(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterAwsObjectStorageRootMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unregister a Storage Root. :: Unregister a Storage Root.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootResponse> unregisterStorageRoot(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnregisterStorageRootMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe a Storage Root instance. :: Describe a Storage Root instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootResponse> describeStorageRoot(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeStorageRootMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List Storage Root instances.  :: List Storage Root instances.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsResponse> listStorageRoots(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListStorageRootsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Find Storage Root instances based on criteria. :: Find Storage Root instances based on criteria.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsResponse> findStorageRoots(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFindStorageRootsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set the assignability flag of a Storage Root. :: Set the assignability flag of a Storage Root.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityResponse> setStorageRootAssignability(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetStorageRootAssignabilityMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Register an AWS RDBMS as a shared Database Storage Provider. :: Register an AWS RDBMS as a shared Database Storage Provider.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse> registerAwsRdbms(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterAwsRdbmsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unregister a RDBMS as a shared Database Storage Provider. :: Unregister a RDBMS as a shared Database Storage Provider.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsResponse> unregisterRdbms(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnregisterRdbmsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe a registered RDBMS instance. :: Describe a registered RDBMS instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsResponse> describeRdbms(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeRdbmsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Find the registered RDBMS instances based on criteria. :: Find the registered RDBMS instances based on criteria.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsResponse> findRdbms(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFindRdbmsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List the registered RDBMS instances. :: List the registered RDBMS instances.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsResponse> listRdbms(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListRdbmsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set the assignability flag of a shared RDBMS instance. :: Set the assignability flag of a shared RDBMS instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityResponse> setRdbmsAssignability(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetRdbmsAssignabilityMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_SUPPORTED_CLOUD_REGIONS = 0;
  private static final int METHODID_LIST_SUPPORTED_CLOUD_PLATFORMS = 1;
  private static final int METHODID_ADD_SERVICE = 2;
  private static final int METHODID_REMOVE_SERVICE = 3;
  private static final int METHODID_DESCRIBE_SERVICE = 4;
  private static final int METHODID_REQUEST_SERVICE_RESTART = 5;
  private static final int METHODID_REQUEST_SERVICE_SUSPEND = 6;
  private static final int METHODID_REQUEST_SERVICE_START = 7;
  private static final int METHODID_REQUEST_SERVICE_UPGRADE = 8;
  private static final int METHODID_CHECK_SERVICE_REQUEST = 9;
  private static final int METHODID_LIST_SERVICE_VERSIONS = 10;
  private static final int METHODID_CREATE_INSTANCE = 11;
  private static final int METHODID_REQUEST_MOVE_INSTANCE_TO_OPERATIONAL_ENVIRONMENT = 12;
  private static final int METHODID_PAIR_ENVIRONMENT_TO_INSTANCE = 13;
  private static final int METHODID_UNPAIR_ENVIRONMENT_FROM_INSTANCE = 14;
  private static final int METHODID_DELETE_INSTANCE = 15;
  private static final int METHODID_DESCRIBE_INSTANCE = 16;
  private static final int METHODID_LIST_INSTANCES = 17;
  private static final int METHODID_FIND_INSTANCES = 18;
  private static final int METHODID_REQUEST_INSTANCE_STOP = 19;
  private static final int METHODID_REQUEST_INSTANCE_START = 20;
  private static final int METHODID_REQUEST_INSTANCE_RESTART = 21;
  private static final int METHODID_CHECK_INSTANCE_REQUEST = 22;
  private static final int METHODID_CREATE_AWS_PRIVATELINK_CONNECTION = 23;
  private static final int METHODID_CREATE_AWS_PRIVATELINK_CONNECTION_FOR_HMS_DATABASE = 24;
  private static final int METHODID_DELETE_CONNECTION = 25;
  private static final int METHODID_DESCRIBE_CONNECTION = 26;
  private static final int METHODID_LIST_CONNECTIONS = 27;
  private static final int METHODID_REGISTER_OPERATIONAL_ENVIRONMENT = 28;
  private static final int METHODID_UNREGISTER_OPERATIONAL_ENVIRONMENT = 29;
  private static final int METHODID_DESCRIBE_OPERATIONAL_ENVIRONMENT = 30;
  private static final int METHODID_LIST_OPERATIONAL_ENVIRONMENTS = 31;
  private static final int METHODID_FIND_OPERATIONAL_ENVIRONMENTS = 32;
  private static final int METHODID_SET_OPERATIONAL_ENVIRONMENT_ASSIGNABILITY = 33;
  private static final int METHODID_REGISTER_AWS_OPEN_SEARCH_INDEXER = 34;
  private static final int METHODID_UNREGISTER_INDEXER = 35;
  private static final int METHODID_DESCRIBE_INDEXER = 36;
  private static final int METHODID_LIST_INDEXERS = 37;
  private static final int METHODID_FIND_INDEXERS = 38;
  private static final int METHODID_SET_INDEXER_ASSIGNABILITY = 39;
  private static final int METHODID_REGISTER_AWS_OBJECT_STORAGE_ROOT = 40;
  private static final int METHODID_UNREGISTER_STORAGE_ROOT = 41;
  private static final int METHODID_DESCRIBE_STORAGE_ROOT = 42;
  private static final int METHODID_LIST_STORAGE_ROOTS = 43;
  private static final int METHODID_FIND_STORAGE_ROOTS = 44;
  private static final int METHODID_SET_STORAGE_ROOT_ASSIGNABILITY = 45;
  private static final int METHODID_REGISTER_AWS_RDBMS = 46;
  private static final int METHODID_UNREGISTER_RDBMS = 47;
  private static final int METHODID_DESCRIBE_RDBMS = 48;
  private static final int METHODID_FIND_RDBMS = 49;
  private static final int METHODID_LIST_RDBMS = 50;
  private static final int METHODID_SET_RDBMS_ASSIGNABILITY = 51;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final SDXSvcAdminImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SDXSvcAdminImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_LIST_SUPPORTED_CLOUD_REGIONS:
          serviceImpl.listSupportedCloudRegions((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudRegionsResponse>) responseObserver);
          break;
        case METHODID_LIST_SUPPORTED_CLOUD_PLATFORMS:
          serviceImpl.listSupportedCloudPlatforms((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListSupportedCloudPlatformsResponse>) responseObserver);
          break;
        case METHODID_ADD_SERVICE:
          serviceImpl.addService((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse>) responseObserver);
          break;
        case METHODID_REMOVE_SERVICE:
          serviceImpl.removeService((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_SERVICE:
          serviceImpl.describeService((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse>) responseObserver);
          break;
        case METHODID_REQUEST_SERVICE_RESTART:
          serviceImpl.requestServiceRestart((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse>) responseObserver);
          break;
        case METHODID_REQUEST_SERVICE_SUSPEND:
          serviceImpl.requestServiceSuspend((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse>) responseObserver);
          break;
        case METHODID_REQUEST_SERVICE_START:
          serviceImpl.requestServiceStart((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse>) responseObserver);
          break;
        case METHODID_REQUEST_SERVICE_UPGRADE:
          serviceImpl.requestServiceUpgrade((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse>) responseObserver);
          break;
        case METHODID_CHECK_SERVICE_REQUEST:
          serviceImpl.checkServiceRequest((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckServiceRequestResponse>) responseObserver);
          break;
        case METHODID_LIST_SERVICE_VERSIONS:
          serviceImpl.listServiceVersions((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse>) responseObserver);
          break;
        case METHODID_CREATE_INSTANCE:
          serviceImpl.createInstance((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse>) responseObserver);
          break;
        case METHODID_REQUEST_MOVE_INSTANCE_TO_OPERATIONAL_ENVIRONMENT:
          serviceImpl.requestMoveInstanceToOperationalEnvironment((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse>) responseObserver);
          break;
        case METHODID_PAIR_ENVIRONMENT_TO_INSTANCE:
          serviceImpl.pairEnvironmentToInstance((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.PairEnvironmentToInstanceResponse>) responseObserver);
          break;
        case METHODID_UNPAIR_ENVIRONMENT_FROM_INSTANCE:
          serviceImpl.unpairEnvironmentFromInstance((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnpairEnvironmentFromInstanceResponse>) responseObserver);
          break;
        case METHODID_DELETE_INSTANCE:
          serviceImpl.deleteInstance((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteInstanceResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_INSTANCE:
          serviceImpl.describeInstance((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeInstanceResponse>) responseObserver);
          break;
        case METHODID_LIST_INSTANCES:
          serviceImpl.listInstances((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListInstancesResponse>) responseObserver);
          break;
        case METHODID_FIND_INSTANCES:
          serviceImpl.findInstances((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindInstancesResponse>) responseObserver);
          break;
        case METHODID_REQUEST_INSTANCE_STOP:
          serviceImpl.requestInstanceStop((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStopResponse>) responseObserver);
          break;
        case METHODID_REQUEST_INSTANCE_START:
          serviceImpl.requestInstanceStart((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceStartResponse>) responseObserver);
          break;
        case METHODID_REQUEST_INSTANCE_RESTART:
          serviceImpl.requestInstanceRestart((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestInstanceRestartResponse>) responseObserver);
          break;
        case METHODID_CHECK_INSTANCE_REQUEST:
          serviceImpl.checkInstanceRequest((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CheckInstanceRequestResponse>) responseObserver);
          break;
        case METHODID_CREATE_AWS_PRIVATELINK_CONNECTION:
          serviceImpl.createAwsPrivatelinkConnection((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionResponse>) responseObserver);
          break;
        case METHODID_CREATE_AWS_PRIVATELINK_CONNECTION_FOR_HMS_DATABASE:
          serviceImpl.createAwsPrivatelinkConnectionForHmsDatabase((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateAwsPrivatelinkConnectionForHmsDatabaseResponse>) responseObserver);
          break;
        case METHODID_DELETE_CONNECTION:
          serviceImpl.deleteConnection((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DeleteConnectionResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_CONNECTION:
          serviceImpl.describeConnection((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeConnectionResponse>) responseObserver);
          break;
        case METHODID_LIST_CONNECTIONS:
          serviceImpl.listConnections((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListConnectionsResponse>) responseObserver);
          break;
        case METHODID_REGISTER_OPERATIONAL_ENVIRONMENT:
          serviceImpl.registerOperationalEnvironment((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterOperationalEnvironmentResponse>) responseObserver);
          break;
        case METHODID_UNREGISTER_OPERATIONAL_ENVIRONMENT:
          serviceImpl.unregisterOperationalEnvironment((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterOperationalEnvironmentResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_OPERATIONAL_ENVIRONMENT:
          serviceImpl.describeOperationalEnvironment((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeOperationalEnvironmentResponse>) responseObserver);
          break;
        case METHODID_LIST_OPERATIONAL_ENVIRONMENTS:
          serviceImpl.listOperationalEnvironments((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListOperationalEnvironmentsResponse>) responseObserver);
          break;
        case METHODID_FIND_OPERATIONAL_ENVIRONMENTS:
          serviceImpl.findOperationalEnvironments((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindOperationalEnvironmentsResponse>) responseObserver);
          break;
        case METHODID_SET_OPERATIONAL_ENVIRONMENT_ASSIGNABILITY:
          serviceImpl.setOperationalEnvironmentAssignability((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetOperationalEnvironmentAssignabilityResponse>) responseObserver);
          break;
        case METHODID_REGISTER_AWS_OPEN_SEARCH_INDEXER:
          serviceImpl.registerAwsOpenSearchIndexer((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpenSearchIndexerResponse>) responseObserver);
          break;
        case METHODID_UNREGISTER_INDEXER:
          serviceImpl.unregisterIndexer((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterIndexerResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_INDEXER:
          serviceImpl.describeIndexer((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeIndexerResponse>) responseObserver);
          break;
        case METHODID_LIST_INDEXERS:
          serviceImpl.listIndexers((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListIndexersResponse>) responseObserver);
          break;
        case METHODID_FIND_INDEXERS:
          serviceImpl.findIndexers((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindIndexersResponse>) responseObserver);
          break;
        case METHODID_SET_INDEXER_ASSIGNABILITY:
          serviceImpl.setIndexerAssignability((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetIndexerAssignabilityResponse>) responseObserver);
          break;
        case METHODID_REGISTER_AWS_OBJECT_STORAGE_ROOT:
          serviceImpl.registerAwsObjectStorageRoot((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse>) responseObserver);
          break;
        case METHODID_UNREGISTER_STORAGE_ROOT:
          serviceImpl.unregisterStorageRoot((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterStorageRootResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_STORAGE_ROOT:
          serviceImpl.describeStorageRoot((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeStorageRootResponse>) responseObserver);
          break;
        case METHODID_LIST_STORAGE_ROOTS:
          serviceImpl.listStorageRoots((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListStorageRootsResponse>) responseObserver);
          break;
        case METHODID_FIND_STORAGE_ROOTS:
          serviceImpl.findStorageRoots((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindStorageRootsResponse>) responseObserver);
          break;
        case METHODID_SET_STORAGE_ROOT_ASSIGNABILITY:
          serviceImpl.setStorageRootAssignability((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetStorageRootAssignabilityResponse>) responseObserver);
          break;
        case METHODID_REGISTER_AWS_RDBMS:
          serviceImpl.registerAwsRdbms((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse>) responseObserver);
          break;
        case METHODID_UNREGISTER_RDBMS:
          serviceImpl.unregisterRdbms((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterRdbmsResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_RDBMS:
          serviceImpl.describeRdbms((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeRdbmsResponse>) responseObserver);
          break;
        case METHODID_FIND_RDBMS:
          serviceImpl.findRdbms((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.FindRdbmsResponse>) responseObserver);
          break;
        case METHODID_LIST_RDBMS:
          serviceImpl.listRdbms((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListRdbmsResponse>) responseObserver);
          break;
        case METHODID_SET_RDBMS_ASSIGNABILITY:
          serviceImpl.setRdbmsAssignability((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.SetRdbmsAssignabilityResponse>) responseObserver);
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

  private static abstract class SDXSvcAdminBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SDXSvcAdminBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SDXSvcAdmin");
    }
  }

  private static final class SDXSvcAdminFileDescriptorSupplier
      extends SDXSvcAdminBaseDescriptorSupplier {
    SDXSvcAdminFileDescriptorSupplier() {}
  }

  private static final class SDXSvcAdminMethodDescriptorSupplier
      extends SDXSvcAdminBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SDXSvcAdminMethodDescriptorSupplier(String methodName) {
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
      synchronized (SDXSvcAdminGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SDXSvcAdminFileDescriptorSupplier())
              .addMethod(getListSupportedCloudRegionsMethod())
              .addMethod(getListSupportedCloudPlatformsMethod())
              .addMethod(getAddServiceMethod())
              .addMethod(getRemoveServiceMethod())
              .addMethod(getDescribeServiceMethod())
              .addMethod(getRequestServiceRestartMethod())
              .addMethod(getRequestServiceSuspendMethod())
              .addMethod(getRequestServiceStartMethod())
              .addMethod(getRequestServiceUpgradeMethod())
              .addMethod(getCheckServiceRequestMethod())
              .addMethod(getListServiceVersionsMethod())
              .addMethod(getCreateInstanceMethod())
              .addMethod(getRequestMoveInstanceToOperationalEnvironmentMethod())
              .addMethod(getPairEnvironmentToInstanceMethod())
              .addMethod(getUnpairEnvironmentFromInstanceMethod())
              .addMethod(getDeleteInstanceMethod())
              .addMethod(getDescribeInstanceMethod())
              .addMethod(getListInstancesMethod())
              .addMethod(getFindInstancesMethod())
              .addMethod(getRequestInstanceStopMethod())
              .addMethod(getRequestInstanceStartMethod())
              .addMethod(getRequestInstanceRestartMethod())
              .addMethod(getCheckInstanceRequestMethod())
              .addMethod(getCreateAwsPrivatelinkConnectionMethod())
              .addMethod(getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod())
              .addMethod(getDeleteConnectionMethod())
              .addMethod(getDescribeConnectionMethod())
              .addMethod(getListConnectionsMethod())
              .addMethod(getRegisterOperationalEnvironmentMethod())
              .addMethod(getUnregisterOperationalEnvironmentMethod())
              .addMethod(getDescribeOperationalEnvironmentMethod())
              .addMethod(getListOperationalEnvironmentsMethod())
              .addMethod(getFindOperationalEnvironmentsMethod())
              .addMethod(getSetOperationalEnvironmentAssignabilityMethod())
              .addMethod(getRegisterAwsOpenSearchIndexerMethod())
              .addMethod(getUnregisterIndexerMethod())
              .addMethod(getDescribeIndexerMethod())
              .addMethod(getListIndexersMethod())
              .addMethod(getFindIndexersMethod())
              .addMethod(getSetIndexerAssignabilityMethod())
              .addMethod(getRegisterAwsObjectStorageRootMethod())
              .addMethod(getUnregisterStorageRootMethod())
              .addMethod(getDescribeStorageRootMethod())
              .addMethod(getListStorageRootsMethod())
              .addMethod(getFindStorageRootsMethod())
              .addMethod(getSetStorageRootAssignabilityMethod())
              .addMethod(getRegisterAwsRdbmsMethod())
              .addMethod(getUnregisterRdbmsMethod())
              .addMethod(getDescribeRdbmsMethod())
              .addMethod(getFindRdbmsMethod())
              .addMethod(getListRdbmsMethod())
              .addMethod(getSetRdbmsAssignabilityMethod())
              .build();
        }
      }
    }
    return result;
  }
}
