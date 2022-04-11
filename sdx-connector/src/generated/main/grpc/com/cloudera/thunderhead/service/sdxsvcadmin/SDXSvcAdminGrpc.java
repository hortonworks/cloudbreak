package com.cloudera.thunderhead.service.sdxsvcadmin;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Cloudera :: Defining sdxsvcAdmin API service
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.42.1)",
    comments = "Source: sdxsvcadmin.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class SDXSvcAdminGrpc {

  private SDXSvcAdminGrpc() {}

  public static final String SERVICE_NAME = "sdxsvcadmin.SDXSvcAdmin";

  // Static method descriptors that strictly reflect the proto.
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceResponse> getRegisterAwsEndpointServiceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterAwsEndpointService",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceResponse> getRegisterAwsEndpointServiceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceResponse> getRegisterAwsEndpointServiceMethod;
    if ((getRegisterAwsEndpointServiceMethod = SDXSvcAdminGrpc.getRegisterAwsEndpointServiceMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRegisterAwsEndpointServiceMethod = SDXSvcAdminGrpc.getRegisterAwsEndpointServiceMethod) == null) {
          SDXSvcAdminGrpc.getRegisterAwsEndpointServiceMethod = getRegisterAwsEndpointServiceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterAwsEndpointService"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RegisterAwsEndpointService"))
              .build();
        }
      }
    }
    return getRegisterAwsEndpointServiceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceResponse> getUnregisterAwsEndpointServiceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnregisterAwsEndpointService",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceResponse> getUnregisterAwsEndpointServiceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceResponse> getUnregisterAwsEndpointServiceMethod;
    if ((getUnregisterAwsEndpointServiceMethod = SDXSvcAdminGrpc.getUnregisterAwsEndpointServiceMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getUnregisterAwsEndpointServiceMethod = SDXSvcAdminGrpc.getUnregisterAwsEndpointServiceMethod) == null) {
          SDXSvcAdminGrpc.getUnregisterAwsEndpointServiceMethod = getUnregisterAwsEndpointServiceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnregisterAwsEndpointService"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("UnregisterAwsEndpointService"))
              .build();
        }
      }
    }
    return getUnregisterAwsEndpointServiceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyResponse> getRegisterAwsAlbTrafficSteeringProxyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterAwsAlbTrafficSteeringProxy",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyResponse> getRegisterAwsAlbTrafficSteeringProxyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyResponse> getRegisterAwsAlbTrafficSteeringProxyMethod;
    if ((getRegisterAwsAlbTrafficSteeringProxyMethod = SDXSvcAdminGrpc.getRegisterAwsAlbTrafficSteeringProxyMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRegisterAwsAlbTrafficSteeringProxyMethod = SDXSvcAdminGrpc.getRegisterAwsAlbTrafficSteeringProxyMethod) == null) {
          SDXSvcAdminGrpc.getRegisterAwsAlbTrafficSteeringProxyMethod = getRegisterAwsAlbTrafficSteeringProxyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterAwsAlbTrafficSteeringProxy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RegisterAwsAlbTrafficSteeringProxy"))
              .build();
        }
      }
    }
    return getRegisterAwsAlbTrafficSteeringProxyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyResponse> getUnregisterAwsAlbTrafficSteeringProxyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnregisterAwsAlbTrafficSteeringProxy",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyResponse> getUnregisterAwsAlbTrafficSteeringProxyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyResponse> getUnregisterAwsAlbTrafficSteeringProxyMethod;
    if ((getUnregisterAwsAlbTrafficSteeringProxyMethod = SDXSvcAdminGrpc.getUnregisterAwsAlbTrafficSteeringProxyMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getUnregisterAwsAlbTrafficSteeringProxyMethod = SDXSvcAdminGrpc.getUnregisterAwsAlbTrafficSteeringProxyMethod) == null) {
          SDXSvcAdminGrpc.getUnregisterAwsAlbTrafficSteeringProxyMethod = getUnregisterAwsAlbTrafficSteeringProxyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnregisterAwsAlbTrafficSteeringProxy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("UnregisterAwsAlbTrafficSteeringProxy"))
              .build();
        }
      }
    }
    return getUnregisterAwsAlbTrafficSteeringProxyMethod;
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsResponse> getUnregisterAwsRdbmsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnregisterAwsRdbms",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsResponse> getUnregisterAwsRdbmsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsResponse> getUnregisterAwsRdbmsMethod;
    if ((getUnregisterAwsRdbmsMethod = SDXSvcAdminGrpc.getUnregisterAwsRdbmsMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getUnregisterAwsRdbmsMethod = SDXSvcAdminGrpc.getUnregisterAwsRdbmsMethod) == null) {
          SDXSvcAdminGrpc.getUnregisterAwsRdbmsMethod = getUnregisterAwsRdbmsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnregisterAwsRdbms"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("UnregisterAwsRdbms"))
              .build();
        }
      }
    }
    return getUnregisterAwsRdbmsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchResponse> getRegisterAwsOpensearchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterAwsOpensearch",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchResponse> getRegisterAwsOpensearchMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchResponse> getRegisterAwsOpensearchMethod;
    if ((getRegisterAwsOpensearchMethod = SDXSvcAdminGrpc.getRegisterAwsOpensearchMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getRegisterAwsOpensearchMethod = SDXSvcAdminGrpc.getRegisterAwsOpensearchMethod) == null) {
          SDXSvcAdminGrpc.getRegisterAwsOpensearchMethod = getRegisterAwsOpensearchMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterAwsOpensearch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("RegisterAwsOpensearch"))
              .build();
        }
      }
    }
    return getRegisterAwsOpensearchMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchResponse> getUnregisterAwsOpensearchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnregisterAwsOpensearch",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchResponse> getUnregisterAwsOpensearchMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchResponse> getUnregisterAwsOpensearchMethod;
    if ((getUnregisterAwsOpensearchMethod = SDXSvcAdminGrpc.getUnregisterAwsOpensearchMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getUnregisterAwsOpensearchMethod = SDXSvcAdminGrpc.getUnregisterAwsOpensearchMethod) == null) {
          SDXSvcAdminGrpc.getUnregisterAwsOpensearchMethod = getUnregisterAwsOpensearchMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnregisterAwsOpensearch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("UnregisterAwsOpensearch"))
              .build();
        }
      }
    }
    return getUnregisterAwsOpensearchMethod;
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootResponse> getUnregisterAwsObjectStorageRootMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnregisterAwsObjectStorageRoot",
      requestType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootRequest.class,
      responseType = com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootRequest,
      com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootResponse> getUnregisterAwsObjectStorageRootMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootResponse> getUnregisterAwsObjectStorageRootMethod;
    if ((getUnregisterAwsObjectStorageRootMethod = SDXSvcAdminGrpc.getUnregisterAwsObjectStorageRootMethod) == null) {
      synchronized (SDXSvcAdminGrpc.class) {
        if ((getUnregisterAwsObjectStorageRootMethod = SDXSvcAdminGrpc.getUnregisterAwsObjectStorageRootMethod) == null) {
          SDXSvcAdminGrpc.getUnregisterAwsObjectStorageRootMethod = getUnregisterAwsObjectStorageRootMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootRequest, com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnregisterAwsObjectStorageRoot"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SDXSvcAdminMethodDescriptorSupplier("UnregisterAwsObjectStorageRoot"))
              .build();
        }
      }
    }
    return getUnregisterAwsObjectStorageRootMethod;
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
     * Add an SDX Service to a running SDX Instance. :: Add an SDX Service to a running SDX Instance.
     * </pre>
     */
    public void addService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAddServiceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Remove a SDX Service instance from an SDX Instance. :: Remove a SDX Service instance from an SDX Instance.
     * </pre>
     */
    public void removeService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRemoveServiceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe an SDX service. :: Describe an SDX service.
     * </pre>
     */
    public void describeService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeServiceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Restart an SDX Service in an Instance. :: Restart an SDX Service in an Instance.
     * </pre>
     */
    public void requestServiceRestart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestServiceRestartMethod(), responseObserver);
    }

    /**
     * <pre>
     * Suspend an SDX Service in an Instance. :: Suspend an SDX Service in an Instance.
     * </pre>
     */
    public void requestServiceSuspend(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestServiceSuspendMethod(), responseObserver);
    }

    /**
     * <pre>
     * Start an SDX Service in an Instance. :: Start an SDX Service in an Instance.
     * </pre>
     */
    public void requestServiceStart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestServiceStartMethod(), responseObserver);
    }

    /**
     * <pre>
     * Upgrade an SDX Service in an Instance. :: Upgrade an SDX Service in an Instance.
     * </pre>
     */
    public void requestServiceUpgrade(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestServiceUpgradeMethod(), responseObserver);
    }

    /**
     * <pre>
     * List all available versions of a SDX Service. :: List all available versions of a SDX Service.
     * </pre>
     */
    public void listServiceVersions(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListServiceVersionsMethod(), responseObserver);
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
     * Create an SDX Instance. :: Create an SDX Instance.
     * </pre>
     */
    public void createInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateInstanceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Pair a CDP Environment to an SDX Instance. :: Pair a CDP Environment to a SDX Instance.
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
     * List all SDX Connections. :: List all SDX Connections.
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
     * Register an AWS PrivateLink Endpoint Service with the SDX Resource Manager. :: Register an AWS PrivateLink Endpoint Service with the SDX Resource Manager.
     * </pre>
     */
    public void registerAwsEndpointService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterAwsEndpointServiceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unregister an AWS PrivateLink Endpoint Service with the SDX Resource Manager. :: Unregister an AWS PrivateLink Endpoint Service with the SDX Resource Manager.
     * </pre>
     */
    public void unregisterAwsEndpointService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnregisterAwsEndpointServiceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Register an AWS ALB as a Traffic Steering Proxy. :: Register an AWS ALB as a Traffic Steering Proxy.
     * </pre>
     */
    public void registerAwsAlbTrafficSteeringProxy(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterAwsAlbTrafficSteeringProxyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unregister an AWS ALB as a Traffic Steering Proxy. :: Unregister an AWS ALB as a Traffic Steering Proxy.
     * </pre>
     */
    public void unregisterAwsAlbTrafficSteeringProxy(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnregisterAwsAlbTrafficSteeringProxyMethod(), responseObserver);
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
     * Unregister an AWS RDBMS as a shared Database Storage Provider. :: Unregister an AWS RDBMS as a shared Database Storage Provider.
     * </pre>
     */
    public void unregisterAwsRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnregisterAwsRdbmsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Register an AWS Opensearch instance as a shared indexer. :: Register an AWS Opensearch instance as a shared indexer.
     * </pre>
     */
    public void registerAwsOpensearch(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterAwsOpensearchMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unregister an AWS Opensearch instance as a shared indexer. :: Unregister an AWS Opensearch instance as a shared indexer.
     * </pre>
     */
    public void unregisterAwsOpensearch(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnregisterAwsOpensearchMethod(), responseObserver);
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
     * Unregister an AWS Object Storage root. :: Unregister an AWS Object Storage root.
     * </pre>
     */
    public void unregisterAwsObjectStorageRoot(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnregisterAwsObjectStorageRootMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
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
            getListServiceVersionsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse>(
                  this, METHODID_LIST_SERVICE_VERSIONS)))
          .addMethod(
            getRequestMoveInstanceToOperationalEnvironmentMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse>(
                  this, METHODID_REQUEST_MOVE_INSTANCE_TO_OPERATIONAL_ENVIRONMENT)))
          .addMethod(
            getCreateInstanceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse>(
                  this, METHODID_CREATE_INSTANCE)))
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
            getRegisterAwsEndpointServiceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceResponse>(
                  this, METHODID_REGISTER_AWS_ENDPOINT_SERVICE)))
          .addMethod(
            getUnregisterAwsEndpointServiceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceResponse>(
                  this, METHODID_UNREGISTER_AWS_ENDPOINT_SERVICE)))
          .addMethod(
            getRegisterAwsAlbTrafficSteeringProxyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyResponse>(
                  this, METHODID_REGISTER_AWS_ALB_TRAFFIC_STEERING_PROXY)))
          .addMethod(
            getUnregisterAwsAlbTrafficSteeringProxyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyResponse>(
                  this, METHODID_UNREGISTER_AWS_ALB_TRAFFIC_STEERING_PROXY)))
          .addMethod(
            getRegisterAwsRdbmsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse>(
                  this, METHODID_REGISTER_AWS_RDBMS)))
          .addMethod(
            getUnregisterAwsRdbmsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsResponse>(
                  this, METHODID_UNREGISTER_AWS_RDBMS)))
          .addMethod(
            getRegisterAwsOpensearchMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchResponse>(
                  this, METHODID_REGISTER_AWS_OPENSEARCH)))
          .addMethod(
            getUnregisterAwsOpensearchMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchResponse>(
                  this, METHODID_UNREGISTER_AWS_OPENSEARCH)))
          .addMethod(
            getRegisterAwsObjectStorageRootMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse>(
                  this, METHODID_REGISTER_AWS_OBJECT_STORAGE_ROOT)))
          .addMethod(
            getUnregisterAwsObjectStorageRootMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootRequest,
                com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootResponse>(
                  this, METHODID_UNREGISTER_AWS_OBJECT_STORAGE_ROOT)))
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
     * Add an SDX Service to a running SDX Instance. :: Add an SDX Service to a running SDX Instance.
     * </pre>
     */
    public void addService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAddServiceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Remove a SDX Service instance from an SDX Instance. :: Remove a SDX Service instance from an SDX Instance.
     * </pre>
     */
    public void removeService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRemoveServiceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe an SDX service. :: Describe an SDX service.
     * </pre>
     */
    public void describeService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeServiceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Restart an SDX Service in an Instance. :: Restart an SDX Service in an Instance.
     * </pre>
     */
    public void requestServiceRestart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestServiceRestartMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Suspend an SDX Service in an Instance. :: Suspend an SDX Service in an Instance.
     * </pre>
     */
    public void requestServiceSuspend(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestServiceSuspendMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Start an SDX Service in an Instance. :: Start an SDX Service in an Instance.
     * </pre>
     */
    public void requestServiceStart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestServiceStartMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Upgrade an SDX Service in an Instance. :: Upgrade an SDX Service in an Instance.
     * </pre>
     */
    public void requestServiceUpgrade(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestServiceUpgradeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List all available versions of a SDX Service. :: List all available versions of a SDX Service.
     * </pre>
     */
    public void listServiceVersions(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListServiceVersionsMethod(), getCallOptions()), request, responseObserver);
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
     * Pair a CDP Environment to an SDX Instance. :: Pair a CDP Environment to a SDX Instance.
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
     * List all SDX Connections. :: List all SDX Connections.
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
     * Register an AWS PrivateLink Endpoint Service with the SDX Resource Manager. :: Register an AWS PrivateLink Endpoint Service with the SDX Resource Manager.
     * </pre>
     */
    public void registerAwsEndpointService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterAwsEndpointServiceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unregister an AWS PrivateLink Endpoint Service with the SDX Resource Manager. :: Unregister an AWS PrivateLink Endpoint Service with the SDX Resource Manager.
     * </pre>
     */
    public void unregisterAwsEndpointService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnregisterAwsEndpointServiceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Register an AWS ALB as a Traffic Steering Proxy. :: Register an AWS ALB as a Traffic Steering Proxy.
     * </pre>
     */
    public void registerAwsAlbTrafficSteeringProxy(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterAwsAlbTrafficSteeringProxyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unregister an AWS ALB as a Traffic Steering Proxy. :: Unregister an AWS ALB as a Traffic Steering Proxy.
     * </pre>
     */
    public void unregisterAwsAlbTrafficSteeringProxy(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnregisterAwsAlbTrafficSteeringProxyMethod(), getCallOptions()), request, responseObserver);
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
     * Unregister an AWS RDBMS as a shared Database Storage Provider. :: Unregister an AWS RDBMS as a shared Database Storage Provider.
     * </pre>
     */
    public void unregisterAwsRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnregisterAwsRdbmsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Register an AWS Opensearch instance as a shared indexer. :: Register an AWS Opensearch instance as a shared indexer.
     * </pre>
     */
    public void registerAwsOpensearch(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterAwsOpensearchMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unregister an AWS Opensearch instance as a shared indexer. :: Unregister an AWS Opensearch instance as a shared indexer.
     * </pre>
     */
    public void unregisterAwsOpensearch(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnregisterAwsOpensearchMethod(), getCallOptions()), request, responseObserver);
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
     * Unregister an AWS Object Storage root. :: Unregister an AWS Object Storage root.
     * </pre>
     */
    public void unregisterAwsObjectStorageRoot(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnregisterAwsObjectStorageRootMethod(), getCallOptions()), request, responseObserver);
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
     * Add an SDX Service to a running SDX Instance. :: Add an SDX Service to a running SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse addService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAddServiceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Remove a SDX Service instance from an SDX Instance. :: Remove a SDX Service instance from an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse removeService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRemoveServiceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe an SDX service. :: Describe an SDX service.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse describeService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeServiceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Restart an SDX Service in an Instance. :: Restart an SDX Service in an Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse requestServiceRestart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestServiceRestartMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Suspend an SDX Service in an Instance. :: Suspend an SDX Service in an Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse requestServiceSuspend(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestServiceSuspendMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Start an SDX Service in an Instance. :: Start an SDX Service in an Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse requestServiceStart(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestServiceStartMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Upgrade an SDX Service in an Instance. :: Upgrade an SDX Service in an Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse requestServiceUpgrade(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestServiceUpgradeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List all available versions of a SDX Service. :: List all available versions of a SDX Service.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse listServiceVersions(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListServiceVersionsMethod(), getCallOptions(), request);
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
     * Create an SDX Instance. :: Create an SDX Instance.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse createInstance(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateInstanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Pair a CDP Environment to an SDX Instance. :: Pair a CDP Environment to a SDX Instance.
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
     * List all SDX Connections. :: List all SDX Connections.
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
     * Register an AWS PrivateLink Endpoint Service with the SDX Resource Manager. :: Register an AWS PrivateLink Endpoint Service with the SDX Resource Manager.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceResponse registerAwsEndpointService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterAwsEndpointServiceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unregister an AWS PrivateLink Endpoint Service with the SDX Resource Manager. :: Unregister an AWS PrivateLink Endpoint Service with the SDX Resource Manager.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceResponse unregisterAwsEndpointService(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnregisterAwsEndpointServiceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Register an AWS ALB as a Traffic Steering Proxy. :: Register an AWS ALB as a Traffic Steering Proxy.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyResponse registerAwsAlbTrafficSteeringProxy(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterAwsAlbTrafficSteeringProxyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unregister an AWS ALB as a Traffic Steering Proxy. :: Unregister an AWS ALB as a Traffic Steering Proxy.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyResponse unregisterAwsAlbTrafficSteeringProxy(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnregisterAwsAlbTrafficSteeringProxyMethod(), getCallOptions(), request);
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
     * Unregister an AWS RDBMS as a shared Database Storage Provider. :: Unregister an AWS RDBMS as a shared Database Storage Provider.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsResponse unregisterAwsRdbms(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnregisterAwsRdbmsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Register an AWS Opensearch instance as a shared indexer. :: Register an AWS Opensearch instance as a shared indexer.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchResponse registerAwsOpensearch(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterAwsOpensearchMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unregister an AWS Opensearch instance as a shared indexer. :: Unregister an AWS Opensearch instance as a shared indexer.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchResponse unregisterAwsOpensearch(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnregisterAwsOpensearchMethod(), getCallOptions(), request);
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
     * Unregister an AWS Object Storage root. :: Unregister an AWS Object Storage root.
     * </pre>
     */
    public com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootResponse unregisterAwsObjectStorageRoot(com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnregisterAwsObjectStorageRootMethod(), getCallOptions(), request);
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
     * Add an SDX Service to a running SDX Instance. :: Add an SDX Service to a running SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceResponse> addService(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.AddServiceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAddServiceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Remove a SDX Service instance from an SDX Instance. :: Remove a SDX Service instance from an SDX Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceResponse> removeService(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RemoveServiceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRemoveServiceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe an SDX service. :: Describe an SDX service.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceResponse> describeService(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.DescribeServiceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeServiceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Restart an SDX Service in an Instance. :: Restart an SDX Service in an Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartResponse> requestServiceRestart(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceRestartRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestServiceRestartMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Suspend an SDX Service in an Instance. :: Suspend an SDX Service in an Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendResponse> requestServiceSuspend(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceSuspendRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestServiceSuspendMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Start an SDX Service in an Instance. :: Start an SDX Service in an Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartResponse> requestServiceStart(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceStartRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestServiceStartMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Upgrade an SDX Service in an Instance. :: Upgrade an SDX Service in an Instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeResponse> requestServiceUpgrade(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestServiceUpgradeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestServiceUpgradeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List all available versions of a SDX Service. :: List all available versions of a SDX Service.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse> listServiceVersions(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListServiceVersionsMethod(), getCallOptions()), request);
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
     * Pair a CDP Environment to an SDX Instance. :: Pair a CDP Environment to a SDX Instance.
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
     * List all SDX Connections. :: List all SDX Connections.
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
     * Register an AWS PrivateLink Endpoint Service with the SDX Resource Manager. :: Register an AWS PrivateLink Endpoint Service with the SDX Resource Manager.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceResponse> registerAwsEndpointService(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterAwsEndpointServiceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unregister an AWS PrivateLink Endpoint Service with the SDX Resource Manager. :: Unregister an AWS PrivateLink Endpoint Service with the SDX Resource Manager.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceResponse> unregisterAwsEndpointService(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnregisterAwsEndpointServiceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Register an AWS ALB as a Traffic Steering Proxy. :: Register an AWS ALB as a Traffic Steering Proxy.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyResponse> registerAwsAlbTrafficSteeringProxy(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterAwsAlbTrafficSteeringProxyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unregister an AWS ALB as a Traffic Steering Proxy. :: Unregister an AWS ALB as a Traffic Steering Proxy.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyResponse> unregisterAwsAlbTrafficSteeringProxy(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnregisterAwsAlbTrafficSteeringProxyMethod(), getCallOptions()), request);
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
     * Unregister an AWS RDBMS as a shared Database Storage Provider. :: Unregister an AWS RDBMS as a shared Database Storage Provider.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsResponse> unregisterAwsRdbms(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnregisterAwsRdbmsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Register an AWS Opensearch instance as a shared indexer. :: Register an AWS Opensearch instance as a shared indexer.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchResponse> registerAwsOpensearch(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterAwsOpensearchMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unregister an AWS Opensearch instance as a shared indexer. :: Unregister an AWS Opensearch instance as a shared indexer.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchResponse> unregisterAwsOpensearch(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnregisterAwsOpensearchMethod(), getCallOptions()), request);
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
     * Unregister an AWS Object Storage root. :: Unregister an AWS Object Storage root.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootResponse> unregisterAwsObjectStorageRoot(
        com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnregisterAwsObjectStorageRootMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ADD_SERVICE = 0;
  private static final int METHODID_REMOVE_SERVICE = 1;
  private static final int METHODID_DESCRIBE_SERVICE = 2;
  private static final int METHODID_REQUEST_SERVICE_RESTART = 3;
  private static final int METHODID_REQUEST_SERVICE_SUSPEND = 4;
  private static final int METHODID_REQUEST_SERVICE_START = 5;
  private static final int METHODID_REQUEST_SERVICE_UPGRADE = 6;
  private static final int METHODID_LIST_SERVICE_VERSIONS = 7;
  private static final int METHODID_REQUEST_MOVE_INSTANCE_TO_OPERATIONAL_ENVIRONMENT = 8;
  private static final int METHODID_CREATE_INSTANCE = 9;
  private static final int METHODID_PAIR_ENVIRONMENT_TO_INSTANCE = 10;
  private static final int METHODID_UNPAIR_ENVIRONMENT_FROM_INSTANCE = 11;
  private static final int METHODID_DELETE_INSTANCE = 12;
  private static final int METHODID_DESCRIBE_INSTANCE = 13;
  private static final int METHODID_LIST_INSTANCES = 14;
  private static final int METHODID_CREATE_AWS_PRIVATELINK_CONNECTION = 15;
  private static final int METHODID_CREATE_AWS_PRIVATELINK_CONNECTION_FOR_HMS_DATABASE = 16;
  private static final int METHODID_DELETE_CONNECTION = 17;
  private static final int METHODID_DESCRIBE_CONNECTION = 18;
  private static final int METHODID_LIST_CONNECTIONS = 19;
  private static final int METHODID_REGISTER_OPERATIONAL_ENVIRONMENT = 20;
  private static final int METHODID_UNREGISTER_OPERATIONAL_ENVIRONMENT = 21;
  private static final int METHODID_REGISTER_AWS_ENDPOINT_SERVICE = 22;
  private static final int METHODID_UNREGISTER_AWS_ENDPOINT_SERVICE = 23;
  private static final int METHODID_REGISTER_AWS_ALB_TRAFFIC_STEERING_PROXY = 24;
  private static final int METHODID_UNREGISTER_AWS_ALB_TRAFFIC_STEERING_PROXY = 25;
  private static final int METHODID_REGISTER_AWS_RDBMS = 26;
  private static final int METHODID_UNREGISTER_AWS_RDBMS = 27;
  private static final int METHODID_REGISTER_AWS_OPENSEARCH = 28;
  private static final int METHODID_UNREGISTER_AWS_OPENSEARCH = 29;
  private static final int METHODID_REGISTER_AWS_OBJECT_STORAGE_ROOT = 30;
  private static final int METHODID_UNREGISTER_AWS_OBJECT_STORAGE_ROOT = 31;

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
        case METHODID_LIST_SERVICE_VERSIONS:
          serviceImpl.listServiceVersions((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.ListServiceVersionsResponse>) responseObserver);
          break;
        case METHODID_REQUEST_MOVE_INSTANCE_TO_OPERATIONAL_ENVIRONMENT:
          serviceImpl.requestMoveInstanceToOperationalEnvironment((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RequestMoveInstanceToOperationalEnvironmentResponse>) responseObserver);
          break;
        case METHODID_CREATE_INSTANCE:
          serviceImpl.createInstance((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.CreateInstanceResponse>) responseObserver);
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
        case METHODID_REGISTER_AWS_ENDPOINT_SERVICE:
          serviceImpl.registerAwsEndpointService((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsEndpointServiceResponse>) responseObserver);
          break;
        case METHODID_UNREGISTER_AWS_ENDPOINT_SERVICE:
          serviceImpl.unregisterAwsEndpointService((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsEndpointServiceResponse>) responseObserver);
          break;
        case METHODID_REGISTER_AWS_ALB_TRAFFIC_STEERING_PROXY:
          serviceImpl.registerAwsAlbTrafficSteeringProxy((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsAlbTrafficSteeringProxyResponse>) responseObserver);
          break;
        case METHODID_UNREGISTER_AWS_ALB_TRAFFIC_STEERING_PROXY:
          serviceImpl.unregisterAwsAlbTrafficSteeringProxy((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsAlbTrafficSteeringProxyResponse>) responseObserver);
          break;
        case METHODID_REGISTER_AWS_RDBMS:
          serviceImpl.registerAwsRdbms((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsRdbmsResponse>) responseObserver);
          break;
        case METHODID_UNREGISTER_AWS_RDBMS:
          serviceImpl.unregisterAwsRdbms((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsRdbmsResponse>) responseObserver);
          break;
        case METHODID_REGISTER_AWS_OPENSEARCH:
          serviceImpl.registerAwsOpensearch((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsOpensearchResponse>) responseObserver);
          break;
        case METHODID_UNREGISTER_AWS_OPENSEARCH:
          serviceImpl.unregisterAwsOpensearch((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsOpensearchResponse>) responseObserver);
          break;
        case METHODID_REGISTER_AWS_OBJECT_STORAGE_ROOT:
          serviceImpl.registerAwsObjectStorageRoot((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.RegisterAwsObjectStorageRootResponse>) responseObserver);
          break;
        case METHODID_UNREGISTER_AWS_OBJECT_STORAGE_ROOT:
          serviceImpl.unregisterAwsObjectStorageRoot((com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto.UnregisterAwsObjectStorageRootResponse>) responseObserver);
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
              .addMethod(getAddServiceMethod())
              .addMethod(getRemoveServiceMethod())
              .addMethod(getDescribeServiceMethod())
              .addMethod(getRequestServiceRestartMethod())
              .addMethod(getRequestServiceSuspendMethod())
              .addMethod(getRequestServiceStartMethod())
              .addMethod(getRequestServiceUpgradeMethod())
              .addMethod(getListServiceVersionsMethod())
              .addMethod(getRequestMoveInstanceToOperationalEnvironmentMethod())
              .addMethod(getCreateInstanceMethod())
              .addMethod(getPairEnvironmentToInstanceMethod())
              .addMethod(getUnpairEnvironmentFromInstanceMethod())
              .addMethod(getDeleteInstanceMethod())
              .addMethod(getDescribeInstanceMethod())
              .addMethod(getListInstancesMethod())
              .addMethod(getCreateAwsPrivatelinkConnectionMethod())
              .addMethod(getCreateAwsPrivatelinkConnectionForHmsDatabaseMethod())
              .addMethod(getDeleteConnectionMethod())
              .addMethod(getDescribeConnectionMethod())
              .addMethod(getListConnectionsMethod())
              .addMethod(getRegisterOperationalEnvironmentMethod())
              .addMethod(getUnregisterOperationalEnvironmentMethod())
              .addMethod(getRegisterAwsEndpointServiceMethod())
              .addMethod(getUnregisterAwsEndpointServiceMethod())
              .addMethod(getRegisterAwsAlbTrafficSteeringProxyMethod())
              .addMethod(getUnregisterAwsAlbTrafficSteeringProxyMethod())
              .addMethod(getRegisterAwsRdbmsMethod())
              .addMethod(getUnregisterAwsRdbmsMethod())
              .addMethod(getRegisterAwsOpensearchMethod())
              .addMethod(getUnregisterAwsOpensearchMethod())
              .addMethod(getRegisterAwsObjectStorageRootMethod())
              .addMethod(getUnregisterAwsObjectStorageRootMethod())
              .build();
        }
      }
    }
    return result;
  }
}
