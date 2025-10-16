package com.cloudera.cdp.servicediscovery;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 **
 * Basic functions of the SDX Service Discovery
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class ServiceDiscoveryGrpc {

  private ServiceDiscoveryGrpc() {}

  public static final java.lang.String SERVICE_NAME = "servicediscovery.ServiceDiscovery";

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
    if ((getGetVersionMethod = ServiceDiscoveryGrpc.getGetVersionMethod) == null) {
      synchronized (ServiceDiscoveryGrpc.class) {
        if ((getGetVersionMethod = ServiceDiscoveryGrpc.getGetVersionMethod) == null) {
          ServiceDiscoveryGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceDiscoveryMethodDescriptorSupplier("GetVersion"))
              .build();
        }
      }
    }
    return getGetVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse> getDescribeDatalakeServicesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeDatalakeServices",
      requestType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest.class,
      responseType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse> getDescribeDatalakeServicesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse> getDescribeDatalakeServicesMethod;
    if ((getDescribeDatalakeServicesMethod = ServiceDiscoveryGrpc.getDescribeDatalakeServicesMethod) == null) {
      synchronized (ServiceDiscoveryGrpc.class) {
        if ((getDescribeDatalakeServicesMethod = ServiceDiscoveryGrpc.getDescribeDatalakeServicesMethod) == null) {
          ServiceDiscoveryGrpc.getDescribeDatalakeServicesMethod = getDescribeDatalakeServicesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeDatalakeServices"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceDiscoveryMethodDescriptorSupplier("DescribeDatalakeServices"))
              .build();
        }
      }
    }
    return getDescribeDatalakeServicesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse> getDescribeEnvironmentServicesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeEnvironmentServices",
      requestType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest.class,
      responseType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse> getDescribeEnvironmentServicesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse> getDescribeEnvironmentServicesMethod;
    if ((getDescribeEnvironmentServicesMethod = ServiceDiscoveryGrpc.getDescribeEnvironmentServicesMethod) == null) {
      synchronized (ServiceDiscoveryGrpc.class) {
        if ((getDescribeEnvironmentServicesMethod = ServiceDiscoveryGrpc.getDescribeEnvironmentServicesMethod) == null) {
          ServiceDiscoveryGrpc.getDescribeEnvironmentServicesMethod = getDescribeEnvironmentServicesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeEnvironmentServices"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceDiscoveryMethodDescriptorSupplier("DescribeEnvironmentServices"))
              .build();
        }
      }
    }
    return getDescribeEnvironmentServicesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse> getListDatalakesForEnvMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListDatalakesForEnv",
      requestType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest.class,
      responseType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse> getListDatalakesForEnvMethod() {
    io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse> getListDatalakesForEnvMethod;
    if ((getListDatalakesForEnvMethod = ServiceDiscoveryGrpc.getListDatalakesForEnvMethod) == null) {
      synchronized (ServiceDiscoveryGrpc.class) {
        if ((getListDatalakesForEnvMethod = ServiceDiscoveryGrpc.getListDatalakesForEnvMethod) == null) {
          ServiceDiscoveryGrpc.getListDatalakesForEnvMethod = getListDatalakesForEnvMethod =
              io.grpc.MethodDescriptor.<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListDatalakesForEnv"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceDiscoveryMethodDescriptorSupplier("ListDatalakesForEnv"))
              .build();
        }
      }
    }
    return getListDatalakesForEnvMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse> getDescribeWarehouseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeWarehouse",
      requestType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest.class,
      responseType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse> getDescribeWarehouseMethod() {
    io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse> getDescribeWarehouseMethod;
    if ((getDescribeWarehouseMethod = ServiceDiscoveryGrpc.getDescribeWarehouseMethod) == null) {
      synchronized (ServiceDiscoveryGrpc.class) {
        if ((getDescribeWarehouseMethod = ServiceDiscoveryGrpc.getDescribeWarehouseMethod) == null) {
          ServiceDiscoveryGrpc.getDescribeWarehouseMethod = getDescribeWarehouseMethod =
              io.grpc.MethodDescriptor.<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeWarehouse"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceDiscoveryMethodDescriptorSupplier("DescribeWarehouse"))
              .build();
        }
      }
    }
    return getDescribeWarehouseMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse> getListWarehousesForEnvMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListWarehousesForEnv",
      requestType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest.class,
      responseType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse> getListWarehousesForEnvMethod() {
    io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse> getListWarehousesForEnvMethod;
    if ((getListWarehousesForEnvMethod = ServiceDiscoveryGrpc.getListWarehousesForEnvMethod) == null) {
      synchronized (ServiceDiscoveryGrpc.class) {
        if ((getListWarehousesForEnvMethod = ServiceDiscoveryGrpc.getListWarehousesForEnvMethod) == null) {
          ServiceDiscoveryGrpc.getListWarehousesForEnvMethod = getListWarehousesForEnvMethod =
              io.grpc.MethodDescriptor.<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListWarehousesForEnv"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceDiscoveryMethodDescriptorSupplier("ListWarehousesForEnv"))
              .build();
        }
      }
    }
    return getListWarehousesForEnvMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse> getListVirtualWarehousesForEnvMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListVirtualWarehousesForEnv",
      requestType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest.class,
      responseType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse> getListVirtualWarehousesForEnvMethod() {
    io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse> getListVirtualWarehousesForEnvMethod;
    if ((getListVirtualWarehousesForEnvMethod = ServiceDiscoveryGrpc.getListVirtualWarehousesForEnvMethod) == null) {
      synchronized (ServiceDiscoveryGrpc.class) {
        if ((getListVirtualWarehousesForEnvMethod = ServiceDiscoveryGrpc.getListVirtualWarehousesForEnvMethod) == null) {
          ServiceDiscoveryGrpc.getListVirtualWarehousesForEnvMethod = getListVirtualWarehousesForEnvMethod =
              io.grpc.MethodDescriptor.<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListVirtualWarehousesForEnv"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceDiscoveryMethodDescriptorSupplier("ListVirtualWarehousesForEnv"))
              .build();
        }
      }
    }
    return getListVirtualWarehousesForEnvMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse> getDescribeVirtualWarehouseServicesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeVirtualWarehouseServices",
      requestType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest.class,
      responseType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse> getDescribeVirtualWarehouseServicesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse> getDescribeVirtualWarehouseServicesMethod;
    if ((getDescribeVirtualWarehouseServicesMethod = ServiceDiscoveryGrpc.getDescribeVirtualWarehouseServicesMethod) == null) {
      synchronized (ServiceDiscoveryGrpc.class) {
        if ((getDescribeVirtualWarehouseServicesMethod = ServiceDiscoveryGrpc.getDescribeVirtualWarehouseServicesMethod) == null) {
          ServiceDiscoveryGrpc.getDescribeVirtualWarehouseServicesMethod = getDescribeVirtualWarehouseServicesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeVirtualWarehouseServices"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceDiscoveryMethodDescriptorSupplier("DescribeVirtualWarehouseServices"))
              .build();
        }
      }
    }
    return getDescribeVirtualWarehouseServicesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse> getListOpdbsForEnvMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListOpdbsForEnv",
      requestType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest.class,
      responseType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse> getListOpdbsForEnvMethod() {
    io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse> getListOpdbsForEnvMethod;
    if ((getListOpdbsForEnvMethod = ServiceDiscoveryGrpc.getListOpdbsForEnvMethod) == null) {
      synchronized (ServiceDiscoveryGrpc.class) {
        if ((getListOpdbsForEnvMethod = ServiceDiscoveryGrpc.getListOpdbsForEnvMethod) == null) {
          ServiceDiscoveryGrpc.getListOpdbsForEnvMethod = getListOpdbsForEnvMethod =
              io.grpc.MethodDescriptor.<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListOpdbsForEnv"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceDiscoveryMethodDescriptorSupplier("ListOpdbsForEnv"))
              .build();
        }
      }
    }
    return getListOpdbsForEnvMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse> getDescribeOpdbServicesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeOpdbServices",
      requestType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest.class,
      responseType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse> getDescribeOpdbServicesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse> getDescribeOpdbServicesMethod;
    if ((getDescribeOpdbServicesMethod = ServiceDiscoveryGrpc.getDescribeOpdbServicesMethod) == null) {
      synchronized (ServiceDiscoveryGrpc.class) {
        if ((getDescribeOpdbServicesMethod = ServiceDiscoveryGrpc.getDescribeOpdbServicesMethod) == null) {
          ServiceDiscoveryGrpc.getDescribeOpdbServicesMethod = getDescribeOpdbServicesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeOpdbServices"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceDiscoveryMethodDescriptorSupplier("DescribeOpdbServices"))
              .build();
        }
      }
    }
    return getDescribeOpdbServicesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse> getDescribeDatalakeAsApiRemoteDataContextMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeDatalakeAsApiRemoteDataContext",
      requestType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest.class,
      responseType = com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest,
      com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse> getDescribeDatalakeAsApiRemoteDataContextMethod() {
    io.grpc.MethodDescriptor<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse> getDescribeDatalakeAsApiRemoteDataContextMethod;
    if ((getDescribeDatalakeAsApiRemoteDataContextMethod = ServiceDiscoveryGrpc.getDescribeDatalakeAsApiRemoteDataContextMethod) == null) {
      synchronized (ServiceDiscoveryGrpc.class) {
        if ((getDescribeDatalakeAsApiRemoteDataContextMethod = ServiceDiscoveryGrpc.getDescribeDatalakeAsApiRemoteDataContextMethod) == null) {
          ServiceDiscoveryGrpc.getDescribeDatalakeAsApiRemoteDataContextMethod = getDescribeDatalakeAsApiRemoteDataContextMethod =
              io.grpc.MethodDescriptor.<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest, com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeDatalakeAsApiRemoteDataContext"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceDiscoveryMethodDescriptorSupplier("DescribeDatalakeAsApiRemoteDataContext"))
              .build();
        }
      }
    }
    return getDescribeDatalakeAsApiRemoteDataContextMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ServiceDiscoveryStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ServiceDiscoveryStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ServiceDiscoveryStub>() {
        @java.lang.Override
        public ServiceDiscoveryStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ServiceDiscoveryStub(channel, callOptions);
        }
      };
    return ServiceDiscoveryStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static ServiceDiscoveryBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ServiceDiscoveryBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ServiceDiscoveryBlockingV2Stub>() {
        @java.lang.Override
        public ServiceDiscoveryBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ServiceDiscoveryBlockingV2Stub(channel, callOptions);
        }
      };
    return ServiceDiscoveryBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ServiceDiscoveryBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ServiceDiscoveryBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ServiceDiscoveryBlockingStub>() {
        @java.lang.Override
        public ServiceDiscoveryBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ServiceDiscoveryBlockingStub(channel, callOptions);
        }
      };
    return ServiceDiscoveryBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ServiceDiscoveryFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ServiceDiscoveryFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ServiceDiscoveryFutureStub>() {
        @java.lang.Override
        public ServiceDiscoveryFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ServiceDiscoveryFutureStub(channel, callOptions);
        }
      };
    return ServiceDiscoveryFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   **
   * Basic functions of the SDX Service Discovery
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
     * Get the Services for a specific Datalake cluster
     * Get the Services for a specific Datalake cluster
     * </pre>
     */
    default void describeDatalakeServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeDatalakeServicesMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Get the services for an Environment
     * </pre>
     */
    default void describeEnvironmentServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeEnvironmentServicesMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Get the list of Datalakes available for an Environment
     * </pre>
     */
    default void listDatalakesForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListDatalakesForEnvMethod(), responseObserver);
    }

    /**
     * <pre>
     * Deprecated - Please use ListVirtualWarehousesForEnv as replacement.
     * </pre>
     */
    default void describeWarehouse(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeWarehouseMethod(), responseObserver);
    }

    /**
     * <pre>
     * Deprecated - Please use ListVirtualWarehousesForEnv as replacement.
     * </pre>
     */
    default void listWarehousesForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListWarehousesForEnvMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * List the Virtual Warehouses for a specific Environment.
     * </pre>
     */
    default void listVirtualWarehousesForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListVirtualWarehousesForEnvMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Describe services for a specific Virtual Warehouse.
     * </pre>
     */
    default void describeVirtualWarehouseServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeVirtualWarehouseServicesMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * List the Operational Databases (OpDBs) for a specific environment.
     * </pre>
     */
    default void listOpdbsForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListOpdbsForEnvMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Describe the services for an Operational Database (OpDBs) in a specific environment.
     * </pre>
     */
    default void describeOpdbServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeOpdbServicesMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Describe a specific datalake as ApiRemoteDataContext.
     * </pre>
     */
    default void describeDatalakeAsApiRemoteDataContext(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeDatalakeAsApiRemoteDataContextMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ServiceDiscovery.
   * <pre>
   **
   * Basic functions of the SDX Service Discovery
   * </pre>
   */
  public static abstract class ServiceDiscoveryImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ServiceDiscoveryGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ServiceDiscovery.
   * <pre>
   **
   * Basic functions of the SDX Service Discovery
   * </pre>
   */
  public static final class ServiceDiscoveryStub
      extends io.grpc.stub.AbstractAsyncStub<ServiceDiscoveryStub> {
    private ServiceDiscoveryStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ServiceDiscoveryStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ServiceDiscoveryStub(channel, callOptions);
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
     * Get the Services for a specific Datalake cluster
     * Get the Services for a specific Datalake cluster
     * </pre>
     */
    public void describeDatalakeServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeDatalakeServicesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Get the services for an Environment
     * </pre>
     */
    public void describeEnvironmentServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeEnvironmentServicesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Get the list of Datalakes available for an Environment
     * </pre>
     */
    public void listDatalakesForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListDatalakesForEnvMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deprecated - Please use ListVirtualWarehousesForEnv as replacement.
     * </pre>
     */
    public void describeWarehouse(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeWarehouseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deprecated - Please use ListVirtualWarehousesForEnv as replacement.
     * </pre>
     */
    public void listWarehousesForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListWarehousesForEnvMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * List the Virtual Warehouses for a specific Environment.
     * </pre>
     */
    public void listVirtualWarehousesForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListVirtualWarehousesForEnvMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Describe services for a specific Virtual Warehouse.
     * </pre>
     */
    public void describeVirtualWarehouseServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeVirtualWarehouseServicesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * List the Operational Databases (OpDBs) for a specific environment.
     * </pre>
     */
    public void listOpdbsForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListOpdbsForEnvMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Describe the services for an Operational Database (OpDBs) in a specific environment.
     * </pre>
     */
    public void describeOpdbServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeOpdbServicesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * Describe a specific datalake as ApiRemoteDataContext.
     * </pre>
     */
    public void describeDatalakeAsApiRemoteDataContext(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeDatalakeAsApiRemoteDataContextMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ServiceDiscovery.
   * <pre>
   **
   * Basic functions of the SDX Service Discovery
   * </pre>
   */
  public static final class ServiceDiscoveryBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<ServiceDiscoveryBlockingV2Stub> {
    private ServiceDiscoveryBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ServiceDiscoveryBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ServiceDiscoveryBlockingV2Stub(channel, callOptions);
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
     * Get the Services for a specific Datalake cluster
     * Get the Services for a specific Datalake cluster
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse describeDatalakeServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDescribeDatalakeServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Get the services for an Environment
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse describeEnvironmentServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDescribeEnvironmentServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Get the list of Datalakes available for an Environment
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse listDatalakesForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListDatalakesForEnvMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deprecated - Please use ListVirtualWarehousesForEnv as replacement.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse describeWarehouse(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDescribeWarehouseMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deprecated - Please use ListVirtualWarehousesForEnv as replacement.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse listWarehousesForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListWarehousesForEnvMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * List the Virtual Warehouses for a specific Environment.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse listVirtualWarehousesForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListVirtualWarehousesForEnvMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Describe services for a specific Virtual Warehouse.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse describeVirtualWarehouseServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDescribeVirtualWarehouseServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * List the Operational Databases (OpDBs) for a specific environment.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse listOpdbsForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListOpdbsForEnvMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Describe the services for an Operational Database (OpDBs) in a specific environment.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse describeOpdbServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDescribeOpdbServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Describe a specific datalake as ApiRemoteDataContext.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse describeDatalakeAsApiRemoteDataContext(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDescribeDatalakeAsApiRemoteDataContextMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service ServiceDiscovery.
   * <pre>
   **
   * Basic functions of the SDX Service Discovery
   * </pre>
   */
  public static final class ServiceDiscoveryBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ServiceDiscoveryBlockingStub> {
    private ServiceDiscoveryBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ServiceDiscoveryBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ServiceDiscoveryBlockingStub(channel, callOptions);
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
     * Get the Services for a specific Datalake cluster
     * Get the Services for a specific Datalake cluster
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse describeDatalakeServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeDatalakeServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Get the services for an Environment
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse describeEnvironmentServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeEnvironmentServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Get the list of Datalakes available for an Environment
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse listDatalakesForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListDatalakesForEnvMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deprecated - Please use ListVirtualWarehousesForEnv as replacement.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse describeWarehouse(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeWarehouseMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deprecated - Please use ListVirtualWarehousesForEnv as replacement.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse listWarehousesForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListWarehousesForEnvMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * List the Virtual Warehouses for a specific Environment.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse listVirtualWarehousesForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListVirtualWarehousesForEnvMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Describe services for a specific Virtual Warehouse.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse describeVirtualWarehouseServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeVirtualWarehouseServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * List the Operational Databases (OpDBs) for a specific environment.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse listOpdbsForEnv(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListOpdbsForEnvMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Describe the services for an Operational Database (OpDBs) in a specific environment.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse describeOpdbServices(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeOpdbServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * Describe a specific datalake as ApiRemoteDataContext.
     * </pre>
     */
    public com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse describeDatalakeAsApiRemoteDataContext(com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeDatalakeAsApiRemoteDataContextMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ServiceDiscovery.
   * <pre>
   **
   * Basic functions of the SDX Service Discovery
   * </pre>
   */
  public static final class ServiceDiscoveryFutureStub
      extends io.grpc.stub.AbstractFutureStub<ServiceDiscoveryFutureStub> {
    private ServiceDiscoveryFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ServiceDiscoveryFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ServiceDiscoveryFutureStub(channel, callOptions);
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
     * Get the Services for a specific Datalake cluster
     * Get the Services for a specific Datalake cluster
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse> describeDatalakeServices(
        com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeDatalakeServicesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Get the services for an Environment
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse> describeEnvironmentServices(
        com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeEnvironmentServicesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Get the list of Datalakes available for an Environment
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse> listDatalakesForEnv(
        com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListDatalakesForEnvMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deprecated - Please use ListVirtualWarehousesForEnv as replacement.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse> describeWarehouse(
        com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeWarehouseMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deprecated - Please use ListVirtualWarehousesForEnv as replacement.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse> listWarehousesForEnv(
        com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListWarehousesForEnvMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * List the Virtual Warehouses for a specific Environment.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse> listVirtualWarehousesForEnv(
        com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListVirtualWarehousesForEnvMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Describe services for a specific Virtual Warehouse.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse> describeVirtualWarehouseServices(
        com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeVirtualWarehouseServicesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * List the Operational Databases (OpDBs) for a specific environment.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse> listOpdbsForEnv(
        com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListOpdbsForEnvMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Describe the services for an Operational Database (OpDBs) in a specific environment.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse> describeOpdbServices(
        com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeOpdbServicesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * Describe a specific datalake as ApiRemoteDataContext.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse> describeDatalakeAsApiRemoteDataContext(
        com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeDatalakeAsApiRemoteDataContextMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_DESCRIBE_DATALAKE_SERVICES = 1;
  private static final int METHODID_DESCRIBE_ENVIRONMENT_SERVICES = 2;
  private static final int METHODID_LIST_DATALAKES_FOR_ENV = 3;
  private static final int METHODID_DESCRIBE_WAREHOUSE = 4;
  private static final int METHODID_LIST_WAREHOUSES_FOR_ENV = 5;
  private static final int METHODID_LIST_VIRTUAL_WAREHOUSES_FOR_ENV = 6;
  private static final int METHODID_DESCRIBE_VIRTUAL_WAREHOUSE_SERVICES = 7;
  private static final int METHODID_LIST_OPDBS_FOR_ENV = 8;
  private static final int METHODID_DESCRIBE_OPDB_SERVICES = 9;
  private static final int METHODID_DESCRIBE_DATALAKE_AS_API_REMOTE_DATA_CONTEXT = 10;

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
        case METHODID_DESCRIBE_DATALAKE_SERVICES:
          serviceImpl.describeDatalakeServices((com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_ENVIRONMENT_SERVICES:
          serviceImpl.describeEnvironmentServices((com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse>) responseObserver);
          break;
        case METHODID_LIST_DATALAKES_FOR_ENV:
          serviceImpl.listDatalakesForEnv((com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_WAREHOUSE:
          serviceImpl.describeWarehouse((com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse>) responseObserver);
          break;
        case METHODID_LIST_WAREHOUSES_FOR_ENV:
          serviceImpl.listWarehousesForEnv((com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse>) responseObserver);
          break;
        case METHODID_LIST_VIRTUAL_WAREHOUSES_FOR_ENV:
          serviceImpl.listVirtualWarehousesForEnv((com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_VIRTUAL_WAREHOUSE_SERVICES:
          serviceImpl.describeVirtualWarehouseServices((com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse>) responseObserver);
          break;
        case METHODID_LIST_OPDBS_FOR_ENV:
          serviceImpl.listOpdbsForEnv((com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_OPDB_SERVICES:
          serviceImpl.describeOpdbServices((com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_DATALAKE_AS_API_REMOTE_DATA_CONTEXT:
          serviceImpl.describeDatalakeAsApiRemoteDataContext((com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse>) responseObserver);
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
          getDescribeDatalakeServicesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesRequest,
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeServicesResponse>(
                service, METHODID_DESCRIBE_DATALAKE_SERVICES)))
        .addMethod(
          getDescribeEnvironmentServicesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesRequest,
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeEnvironmentServicesResponse>(
                service, METHODID_DESCRIBE_ENVIRONMENT_SERVICES)))
        .addMethod(
          getListDatalakesForEnvMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvRequest,
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListDatalakesForEnvResponse>(
                service, METHODID_LIST_DATALAKES_FOR_ENV)))
        .addMethod(
          getDescribeWarehouseMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseRequest,
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeWarehouseResponse>(
                service, METHODID_DESCRIBE_WAREHOUSE)))
        .addMethod(
          getListWarehousesForEnvMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvRequest,
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListWarehousesForEnvResponse>(
                service, METHODID_LIST_WAREHOUSES_FOR_ENV)))
        .addMethod(
          getListVirtualWarehousesForEnvMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvRequest,
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListVirtualWarehousesForEnvResponse>(
                service, METHODID_LIST_VIRTUAL_WAREHOUSES_FOR_ENV)))
        .addMethod(
          getDescribeVirtualWarehouseServicesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesRequest,
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeVirtualWarehouseServicesResponse>(
                service, METHODID_DESCRIBE_VIRTUAL_WAREHOUSE_SERVICES)))
        .addMethod(
          getListOpdbsForEnvMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvRequest,
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.ListOpdbsForEnvResponse>(
                service, METHODID_LIST_OPDBS_FOR_ENV)))
        .addMethod(
          getDescribeOpdbServicesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesRequest,
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeOpdbServicesResponse>(
                service, METHODID_DESCRIBE_OPDB_SERVICES)))
        .addMethod(
          getDescribeDatalakeAsApiRemoteDataContextMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest,
              com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse>(
                service, METHODID_DESCRIBE_DATALAKE_AS_API_REMOTE_DATA_CONTEXT)))
        .build();
  }

  private static abstract class ServiceDiscoveryBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ServiceDiscoveryBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ServiceDiscovery");
    }
  }

  private static final class ServiceDiscoveryFileDescriptorSupplier
      extends ServiceDiscoveryBaseDescriptorSupplier {
    ServiceDiscoveryFileDescriptorSupplier() {}
  }

  private static final class ServiceDiscoveryMethodDescriptorSupplier
      extends ServiceDiscoveryBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ServiceDiscoveryMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (ServiceDiscoveryGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ServiceDiscoveryFileDescriptorSupplier())
              .addMethod(getGetVersionMethod())
              .addMethod(getDescribeDatalakeServicesMethod())
              .addMethod(getDescribeEnvironmentServicesMethod())
              .addMethod(getListDatalakesForEnvMethod())
              .addMethod(getDescribeWarehouseMethod())
              .addMethod(getListWarehousesForEnvMethod())
              .addMethod(getListVirtualWarehousesForEnvMethod())
              .addMethod(getDescribeVirtualWarehouseServicesMethod())
              .addMethod(getListOpdbsForEnvMethod())
              .addMethod(getDescribeOpdbServicesMethod())
              .addMethod(getDescribeDatalakeAsApiRemoteDataContextMethod())
              .build();
        }
      }
    }
    return result;
  }
}
