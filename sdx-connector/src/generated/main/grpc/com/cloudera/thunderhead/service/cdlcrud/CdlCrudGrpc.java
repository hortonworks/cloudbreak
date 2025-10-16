package com.cloudera.thunderhead.service.cdlcrud;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class CdlCrudGrpc {

  private CdlCrudGrpc() {}

  public static final java.lang.String SERVICE_NAME = "cdlcrud.CdlCrud";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse> getCreateDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateDatalake",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse> getCreateDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse> getCreateDatalakeMethod;
    if ((getCreateDatalakeMethod = CdlCrudGrpc.getCreateDatalakeMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getCreateDatalakeMethod = CdlCrudGrpc.getCreateDatalakeMethod) == null) {
          CdlCrudGrpc.getCreateDatalakeMethod = getCreateDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("CreateDatalake"))
              .build();
        }
      }
    }
    return getCreateDatalakeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse> getDeleteDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteDatalake",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse> getDeleteDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse> getDeleteDatalakeMethod;
    if ((getDeleteDatalakeMethod = CdlCrudGrpc.getDeleteDatalakeMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getDeleteDatalakeMethod = CdlCrudGrpc.getDeleteDatalakeMethod) == null) {
          CdlCrudGrpc.getDeleteDatalakeMethod = getDeleteDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("DeleteDatalake"))
              .build();
        }
      }
    }
    return getDeleteDatalakeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse> getDescribeDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeDatalake",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse> getDescribeDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse> getDescribeDatalakeMethod;
    if ((getDescribeDatalakeMethod = CdlCrudGrpc.getDescribeDatalakeMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getDescribeDatalakeMethod = CdlCrudGrpc.getDescribeDatalakeMethod) == null) {
          CdlCrudGrpc.getDescribeDatalakeMethod = getDescribeDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("DescribeDatalake"))
              .build();
        }
      }
    }
    return getDescribeDatalakeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse> getFindDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FindDatalake",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse> getFindDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse> getFindDatalakeMethod;
    if ((getFindDatalakeMethod = CdlCrudGrpc.getFindDatalakeMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getFindDatalakeMethod = CdlCrudGrpc.getFindDatalakeMethod) == null) {
          CdlCrudGrpc.getFindDatalakeMethod = getFindDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FindDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("FindDatalake"))
              .build();
        }
      }
    }
    return getFindDatalakeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse> getDescribeServicesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeServices",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse> getDescribeServicesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse> getDescribeServicesMethod;
    if ((getDescribeServicesMethod = CdlCrudGrpc.getDescribeServicesMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getDescribeServicesMethod = CdlCrudGrpc.getDescribeServicesMethod) == null) {
          CdlCrudGrpc.getDescribeServicesMethod = getDescribeServicesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeServices"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("DescribeServices"))
              .build();
        }
      }
    }
    return getDescribeServicesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse> getCollectDatalakeDiagnosticsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CollectDatalakeDiagnostics",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse> getCollectDatalakeDiagnosticsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse> getCollectDatalakeDiagnosticsMethod;
    if ((getCollectDatalakeDiagnosticsMethod = CdlCrudGrpc.getCollectDatalakeDiagnosticsMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getCollectDatalakeDiagnosticsMethod = CdlCrudGrpc.getCollectDatalakeDiagnosticsMethod) == null) {
          CdlCrudGrpc.getCollectDatalakeDiagnosticsMethod = getCollectDatalakeDiagnosticsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CollectDatalakeDiagnostics"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("CollectDatalakeDiagnostics"))
              .build();
        }
      }
    }
    return getCollectDatalakeDiagnosticsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse> getListDatalakeDiagnosticsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListDatalakeDiagnostics",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse> getListDatalakeDiagnosticsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse> getListDatalakeDiagnosticsMethod;
    if ((getListDatalakeDiagnosticsMethod = CdlCrudGrpc.getListDatalakeDiagnosticsMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getListDatalakeDiagnosticsMethod = CdlCrudGrpc.getListDatalakeDiagnosticsMethod) == null) {
          CdlCrudGrpc.getListDatalakeDiagnosticsMethod = getListDatalakeDiagnosticsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListDatalakeDiagnostics"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("ListDatalakeDiagnostics"))
              .build();
        }
      }
    }
    return getListDatalakeDiagnosticsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse> getCancelDatalakeDiagnosticsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CancelDatalakeDiagnostics",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse> getCancelDatalakeDiagnosticsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse> getCancelDatalakeDiagnosticsMethod;
    if ((getCancelDatalakeDiagnosticsMethod = CdlCrudGrpc.getCancelDatalakeDiagnosticsMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getCancelDatalakeDiagnosticsMethod = CdlCrudGrpc.getCancelDatalakeDiagnosticsMethod) == null) {
          CdlCrudGrpc.getCancelDatalakeDiagnosticsMethod = getCancelDatalakeDiagnosticsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CancelDatalakeDiagnostics"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("CancelDatalakeDiagnostics"))
              .build();
        }
      }
    }
    return getCancelDatalakeDiagnosticsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse> getListDatalakesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListDatalakes",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse> getListDatalakesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse> getListDatalakesMethod;
    if ((getListDatalakesMethod = CdlCrudGrpc.getListDatalakesMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getListDatalakesMethod = CdlCrudGrpc.getListDatalakesMethod) == null) {
          CdlCrudGrpc.getListDatalakesMethod = getListDatalakesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListDatalakes"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("ListDatalakes"))
              .build();
        }
      }
    }
    return getListDatalakesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse> getUpgradeDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpgradeDatalake",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse> getUpgradeDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse> getUpgradeDatalakeMethod;
    if ((getUpgradeDatalakeMethod = CdlCrudGrpc.getUpgradeDatalakeMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getUpgradeDatalakeMethod = CdlCrudGrpc.getUpgradeDatalakeMethod) == null) {
          CdlCrudGrpc.getUpgradeDatalakeMethod = getUpgradeDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpgradeDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("UpgradeDatalake"))
              .build();
        }
      }
    }
    return getUpgradeDatalakeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse> getSyncIDBrokerMappingsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SyncIDBrokerMappings",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse> getSyncIDBrokerMappingsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse> getSyncIDBrokerMappingsMethod;
    if ((getSyncIDBrokerMappingsMethod = CdlCrudGrpc.getSyncIDBrokerMappingsMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getSyncIDBrokerMappingsMethod = CdlCrudGrpc.getSyncIDBrokerMappingsMethod) == null) {
          CdlCrudGrpc.getSyncIDBrokerMappingsMethod = getSyncIDBrokerMappingsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SyncIDBrokerMappings"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("SyncIDBrokerMappings"))
              .build();
        }
      }
    }
    return getSyncIDBrokerMappingsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse> getBackupRestoreDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "backupRestoreDatalake",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse> getBackupRestoreDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse> getBackupRestoreDatalakeMethod;
    if ((getBackupRestoreDatalakeMethod = CdlCrudGrpc.getBackupRestoreDatalakeMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getBackupRestoreDatalakeMethod = CdlCrudGrpc.getBackupRestoreDatalakeMethod) == null) {
          CdlCrudGrpc.getBackupRestoreDatalakeMethod = getBackupRestoreDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "backupRestoreDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("backupRestoreDatalake"))
              .build();
        }
      }
    }
    return getBackupRestoreDatalakeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse> getGetDatalakeOperationStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getDatalakeOperationStatus",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse> getGetDatalakeOperationStatusMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse> getGetDatalakeOperationStatusMethod;
    if ((getGetDatalakeOperationStatusMethod = CdlCrudGrpc.getGetDatalakeOperationStatusMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getGetDatalakeOperationStatusMethod = CdlCrudGrpc.getGetDatalakeOperationStatusMethod) == null) {
          CdlCrudGrpc.getGetDatalakeOperationStatusMethod = getGetDatalakeOperationStatusMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getDatalakeOperationStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("getDatalakeOperationStatus"))
              .build();
        }
      }
    }
    return getGetDatalakeOperationStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse> getStopDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StopDatalake",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse> getStopDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse> getStopDatalakeMethod;
    if ((getStopDatalakeMethod = CdlCrudGrpc.getStopDatalakeMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getStopDatalakeMethod = CdlCrudGrpc.getStopDatalakeMethod) == null) {
          CdlCrudGrpc.getStopDatalakeMethod = getStopDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StopDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("StopDatalake"))
              .build();
        }
      }
    }
    return getStopDatalakeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse> getStartDatalakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StartDatalake",
      requestType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest.class,
      responseType = com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest,
      com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse> getStartDatalakeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse> getStartDatalakeMethod;
    if ((getStartDatalakeMethod = CdlCrudGrpc.getStartDatalakeMethod) == null) {
      synchronized (CdlCrudGrpc.class) {
        if ((getStartDatalakeMethod = CdlCrudGrpc.getStartDatalakeMethod) == null) {
          CdlCrudGrpc.getStartDatalakeMethod = getStartDatalakeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest, com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StartDatalake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CdlCrudMethodDescriptorSupplier("StartDatalake"))
              .build();
        }
      }
    }
    return getStartDatalakeMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CdlCrudStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CdlCrudStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CdlCrudStub>() {
        @java.lang.Override
        public CdlCrudStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CdlCrudStub(channel, callOptions);
        }
      };
    return CdlCrudStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static CdlCrudBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CdlCrudBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CdlCrudBlockingV2Stub>() {
        @java.lang.Override
        public CdlCrudBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CdlCrudBlockingV2Stub(channel, callOptions);
        }
      };
    return CdlCrudBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CdlCrudBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CdlCrudBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CdlCrudBlockingStub>() {
        @java.lang.Override
        public CdlCrudBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CdlCrudBlockingStub(channel, callOptions);
        }
      };
    return CdlCrudBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static CdlCrudFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CdlCrudFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CdlCrudFutureStub>() {
        @java.lang.Override
        public CdlCrudFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CdlCrudFutureStub(channel, callOptions);
        }
      };
    return CdlCrudFutureStub.newStub(factory, channel);
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
     * Create a Datalake
     * </pre>
     */
    default void createDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateDatalakeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete a Datalake
     * </pre>
     */
    default void deleteDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteDatalakeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    default void describeDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeDatalakeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    default void findDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFindDatalakeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe Datalake Services
     * </pre>
     */
    default void describeServices(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeServicesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Start DataLake diagnostics collection
     * </pre>
     */
    default void collectDatalakeDiagnostics(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCollectDatalakeDiagnosticsMethod(), responseObserver);
    }

    /**
     * <pre>
     * List recent Datalake diagnostics collections
     * </pre>
     */
    default void listDatalakeDiagnostics(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListDatalakeDiagnosticsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Cancel running Datalake diagnostics collections
     * </pre>
     */
    default void cancelDatalakeDiagnostics(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCancelDatalakeDiagnosticsMethod(), responseObserver);
    }

    /**
     * <pre>
     * List all Datalakes of an environment or datalake
     * </pre>
     */
    default void listDatalakes(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListDatalakesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Upgrades specified Datalake to a later version of its layout
     * </pre>
     */
    default void upgradeDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpgradeDatalakeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Synchronize IBBroker mappings
     * </pre>
     */
    default void syncIDBrokerMappings(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSyncIDBrokerMappingsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Backup or restore database of a Datalake
     * </pre>
     */
    default void backupRestoreDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBackupRestoreDatalakeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get operation status and status reason
     * </pre>
     */
    default void getDatalakeOperationStatus(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetDatalakeOperationStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     * Stop a datalake
     * </pre>
     */
    default void stopDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getStopDatalakeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Start a datalake which is in Stopped state
     * </pre>
     */
    default void startDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getStartDatalakeMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service CdlCrud.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class CdlCrudImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return CdlCrudGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service CdlCrud.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class CdlCrudStub
      extends io.grpc.stub.AbstractAsyncStub<CdlCrudStub> {
    private CdlCrudStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CdlCrudStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CdlCrudStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a Datalake
     * </pre>
     */
    public void createDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateDatalakeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete a Datalake
     * </pre>
     */
    public void deleteDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteDatalakeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    public void describeDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeDatalakeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    public void findDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFindDatalakeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe Datalake Services
     * </pre>
     */
    public void describeServices(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeServicesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Start DataLake diagnostics collection
     * </pre>
     */
    public void collectDatalakeDiagnostics(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCollectDatalakeDiagnosticsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List recent Datalake diagnostics collections
     * </pre>
     */
    public void listDatalakeDiagnostics(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListDatalakeDiagnosticsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Cancel running Datalake diagnostics collections
     * </pre>
     */
    public void cancelDatalakeDiagnostics(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCancelDatalakeDiagnosticsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List all Datalakes of an environment or datalake
     * </pre>
     */
    public void listDatalakes(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListDatalakesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Upgrades specified Datalake to a later version of its layout
     * </pre>
     */
    public void upgradeDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpgradeDatalakeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Synchronize IBBroker mappings
     * </pre>
     */
    public void syncIDBrokerMappings(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSyncIDBrokerMappingsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Backup or restore database of a Datalake
     * </pre>
     */
    public void backupRestoreDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBackupRestoreDatalakeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get operation status and status reason
     * </pre>
     */
    public void getDatalakeOperationStatus(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetDatalakeOperationStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Stop a datalake
     * </pre>
     */
    public void stopDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getStopDatalakeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Start a datalake which is in Stopped state
     * </pre>
     */
    public void startDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getStartDatalakeMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service CdlCrud.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class CdlCrudBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<CdlCrudBlockingV2Stub> {
    private CdlCrudBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CdlCrudBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CdlCrudBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse createDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse deleteDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse describeDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDescribeDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse findDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getFindDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe Datalake Services
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse describeServices(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDescribeServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Start DataLake diagnostics collection
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse collectDatalakeDiagnostics(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCollectDatalakeDiagnosticsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List recent Datalake diagnostics collections
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse listDatalakeDiagnostics(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListDatalakeDiagnosticsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Cancel running Datalake diagnostics collections
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse cancelDatalakeDiagnostics(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCancelDatalakeDiagnosticsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List all Datalakes of an environment or datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse listDatalakes(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListDatalakesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Upgrades specified Datalake to a later version of its layout
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse upgradeDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUpgradeDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Synchronize IBBroker mappings
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse syncIDBrokerMappings(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getSyncIDBrokerMappingsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Backup or restore database of a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse backupRestoreDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getBackupRestoreDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get operation status and status reason
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse getDatalakeOperationStatus(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetDatalakeOperationStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Stop a datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse stopDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getStopDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Start a datalake which is in Stopped state
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse startDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getStartDatalakeMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service CdlCrud.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class CdlCrudBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<CdlCrudBlockingStub> {
    private CdlCrudBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CdlCrudBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CdlCrudBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse createDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse deleteDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse describeDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse findDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFindDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe Datalake Services
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse describeServices(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Start DataLake diagnostics collection
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse collectDatalakeDiagnostics(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCollectDatalakeDiagnosticsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List recent Datalake diagnostics collections
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse listDatalakeDiagnostics(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListDatalakeDiagnosticsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Cancel running Datalake diagnostics collections
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse cancelDatalakeDiagnostics(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCancelDatalakeDiagnosticsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List all Datalakes of an environment or datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse listDatalakes(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListDatalakesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Upgrades specified Datalake to a later version of its layout
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse upgradeDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpgradeDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Synchronize IBBroker mappings
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse syncIDBrokerMappings(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSyncIDBrokerMappingsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Backup or restore database of a Datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse backupRestoreDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBackupRestoreDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get operation status and status reason
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse getDatalakeOperationStatus(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetDatalakeOperationStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Stop a datalake
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse stopDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getStopDatalakeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Start a datalake which is in Stopped state
     * </pre>
     */
    public com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse startDatalake(com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getStartDatalakeMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service CdlCrud.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class CdlCrudFutureStub
      extends io.grpc.stub.AbstractFutureStub<CdlCrudFutureStub> {
    private CdlCrudFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CdlCrudFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CdlCrudFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a Datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse> createDatalake(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateDatalakeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete a Datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse> deleteDatalake(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteDatalakeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse> describeDatalake(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeDatalakeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe a Datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse> findDatalake(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFindDatalakeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe Datalake Services
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse> describeServices(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeServicesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Start DataLake diagnostics collection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse> collectDatalakeDiagnostics(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCollectDatalakeDiagnosticsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List recent Datalake diagnostics collections
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse> listDatalakeDiagnostics(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListDatalakeDiagnosticsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Cancel running Datalake diagnostics collections
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse> cancelDatalakeDiagnostics(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCancelDatalakeDiagnosticsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List all Datalakes of an environment or datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse> listDatalakes(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListDatalakesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Upgrades specified Datalake to a later version of its layout
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse> upgradeDatalake(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpgradeDatalakeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Synchronize IBBroker mappings
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse> syncIDBrokerMappings(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSyncIDBrokerMappingsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Backup or restore database of a Datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse> backupRestoreDatalake(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBackupRestoreDatalakeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get operation status and status reason
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse> getDatalakeOperationStatus(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetDatalakeOperationStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Stop a datalake
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse> stopDatalake(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getStopDatalakeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Start a datalake which is in Stopped state
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse> startDatalake(
        com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getStartDatalakeMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_DATALAKE = 0;
  private static final int METHODID_DELETE_DATALAKE = 1;
  private static final int METHODID_DESCRIBE_DATALAKE = 2;
  private static final int METHODID_FIND_DATALAKE = 3;
  private static final int METHODID_DESCRIBE_SERVICES = 4;
  private static final int METHODID_COLLECT_DATALAKE_DIAGNOSTICS = 5;
  private static final int METHODID_LIST_DATALAKE_DIAGNOSTICS = 6;
  private static final int METHODID_CANCEL_DATALAKE_DIAGNOSTICS = 7;
  private static final int METHODID_LIST_DATALAKES = 8;
  private static final int METHODID_UPGRADE_DATALAKE = 9;
  private static final int METHODID_SYNC_IDBROKER_MAPPINGS = 10;
  private static final int METHODID_BACKUP_RESTORE_DATALAKE = 11;
  private static final int METHODID_GET_DATALAKE_OPERATION_STATUS = 12;
  private static final int METHODID_STOP_DATALAKE = 13;
  private static final int METHODID_START_DATALAKE = 14;

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
        case METHODID_CREATE_DATALAKE:
          serviceImpl.createDatalake((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse>) responseObserver);
          break;
        case METHODID_DELETE_DATALAKE:
          serviceImpl.deleteDatalake((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_DATALAKE:
          serviceImpl.describeDatalake((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse>) responseObserver);
          break;
        case METHODID_FIND_DATALAKE:
          serviceImpl.findDatalake((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_SERVICES:
          serviceImpl.describeServices((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse>) responseObserver);
          break;
        case METHODID_COLLECT_DATALAKE_DIAGNOSTICS:
          serviceImpl.collectDatalakeDiagnostics((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse>) responseObserver);
          break;
        case METHODID_LIST_DATALAKE_DIAGNOSTICS:
          serviceImpl.listDatalakeDiagnostics((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse>) responseObserver);
          break;
        case METHODID_CANCEL_DATALAKE_DIAGNOSTICS:
          serviceImpl.cancelDatalakeDiagnostics((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse>) responseObserver);
          break;
        case METHODID_LIST_DATALAKES:
          serviceImpl.listDatalakes((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse>) responseObserver);
          break;
        case METHODID_UPGRADE_DATALAKE:
          serviceImpl.upgradeDatalake((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse>) responseObserver);
          break;
        case METHODID_SYNC_IDBROKER_MAPPINGS:
          serviceImpl.syncIDBrokerMappings((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse>) responseObserver);
          break;
        case METHODID_BACKUP_RESTORE_DATALAKE:
          serviceImpl.backupRestoreDatalake((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse>) responseObserver);
          break;
        case METHODID_GET_DATALAKE_OPERATION_STATUS:
          serviceImpl.getDatalakeOperationStatus((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse>) responseObserver);
          break;
        case METHODID_STOP_DATALAKE:
          serviceImpl.stopDatalake((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse>) responseObserver);
          break;
        case METHODID_START_DATALAKE:
          serviceImpl.startDatalake((com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse>) responseObserver);
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
          getCreateDatalakeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CreateDatalakeResponse>(
                service, METHODID_CREATE_DATALAKE)))
        .addMethod(
          getDeleteDatalakeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DeleteDatalakeResponse>(
                service, METHODID_DELETE_DATALAKE)))
        .addMethod(
          getDescribeDatalakeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeDatalakeResponse>(
                service, METHODID_DESCRIBE_DATALAKE)))
        .addMethod(
          getFindDatalakeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.FindDatalakeRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeResponse>(
                service, METHODID_FIND_DATALAKE)))
        .addMethod(
          getDescribeServicesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DescribeServicesResponse>(
                service, METHODID_DESCRIBE_SERVICES)))
        .addMethod(
          getCollectDatalakeDiagnosticsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CollectDatalakeDiagnosticsResponse>(
                service, METHODID_COLLECT_DATALAKE_DIAGNOSTICS)))
        .addMethod(
          getListDatalakeDiagnosticsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakeDiagnosticsResponse>(
                service, METHODID_LIST_DATALAKE_DIAGNOSTICS)))
        .addMethod(
          getCancelDatalakeDiagnosticsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.CancelDatalakeDiagnosticsResponse>(
                service, METHODID_CANCEL_DATALAKE_DIAGNOSTICS)))
        .addMethod(
          getListDatalakesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.ListDatalakesResponse>(
                service, METHODID_LIST_DATALAKES)))
        .addMethod(
          getUpgradeDatalakeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.UpgradeDatalakeResponse>(
                service, METHODID_UPGRADE_DATALAKE)))
        .addMethod(
          getSyncIDBrokerMappingsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.SyncIDBrokerMappingsResponse>(
                service, METHODID_SYNC_IDBROKER_MAPPINGS)))
        .addMethod(
          getBackupRestoreDatalakeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.BackupRestoreDatalakeResponse>(
                service, METHODID_BACKUP_RESTORE_DATALAKE)))
        .addMethod(
          getGetDatalakeOperationStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.DatalakeOperationStatusResponse>(
                service, METHODID_GET_DATALAKE_OPERATION_STATUS)))
        .addMethod(
          getStopDatalakeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StopDatalakeResponse>(
                service, METHODID_STOP_DATALAKE)))
        .addMethod(
          getStartDatalakeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeRequest,
              com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.StartDatalakeResponse>(
                service, METHODID_START_DATALAKE)))
        .build();
  }

  private static abstract class CdlCrudBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    CdlCrudBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("CdlCrud");
    }
  }

  private static final class CdlCrudFileDescriptorSupplier
      extends CdlCrudBaseDescriptorSupplier {
    CdlCrudFileDescriptorSupplier() {}
  }

  private static final class CdlCrudMethodDescriptorSupplier
      extends CdlCrudBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    CdlCrudMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (CdlCrudGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CdlCrudFileDescriptorSupplier())
              .addMethod(getCreateDatalakeMethod())
              .addMethod(getDeleteDatalakeMethod())
              .addMethod(getDescribeDatalakeMethod())
              .addMethod(getFindDatalakeMethod())
              .addMethod(getDescribeServicesMethod())
              .addMethod(getCollectDatalakeDiagnosticsMethod())
              .addMethod(getListDatalakeDiagnosticsMethod())
              .addMethod(getCancelDatalakeDiagnosticsMethod())
              .addMethod(getListDatalakesMethod())
              .addMethod(getUpgradeDatalakeMethod())
              .addMethod(getSyncIDBrokerMappingsMethod())
              .addMethod(getBackupRestoreDatalakeMethod())
              .addMethod(getGetDatalakeOperationStatusMethod())
              .addMethod(getStopDatalakeMethod())
              .addMethod(getStartDatalakeMethod())
              .build();
        }
      }
    }
    return result;
  }
}
