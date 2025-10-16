package com.cloudera.thunderhead.service.onpremises;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * OnPremises Service :: OnPremises Service is a web service to manage the on-prem clusters and control planes.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class OnPremisesApiGrpc {

  private OnPremisesApiGrpc() {}

  public static final java.lang.String SERVICE_NAME = "onpremises.OnPremisesApi";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse> getListClustersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListClusters",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse> getListClustersMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse> getListClustersMethod;
    if ((getListClustersMethod = OnPremisesApiGrpc.getListClustersMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getListClustersMethod = OnPremisesApiGrpc.getListClustersMethod) == null) {
          OnPremisesApiGrpc.getListClustersMethod = getListClustersMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListClusters"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("ListClusters"))
              .build();
        }
      }
    }
    return getListClustersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse> getRegisterClusterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterCluster",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse> getRegisterClusterMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse> getRegisterClusterMethod;
    if ((getRegisterClusterMethod = OnPremisesApiGrpc.getRegisterClusterMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getRegisterClusterMethod = OnPremisesApiGrpc.getRegisterClusterMethod) == null) {
          OnPremisesApiGrpc.getRegisterClusterMethod = getRegisterClusterMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterCluster"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("RegisterCluster"))
              .build();
        }
      }
    }
    return getRegisterClusterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse> getDescribeClusterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeCluster",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse> getDescribeClusterMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse> getDescribeClusterMethod;
    if ((getDescribeClusterMethod = OnPremisesApiGrpc.getDescribeClusterMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getDescribeClusterMethod = OnPremisesApiGrpc.getDescribeClusterMethod) == null) {
          OnPremisesApiGrpc.getDescribeClusterMethod = getDescribeClusterMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeCluster"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("DescribeCluster"))
              .build();
        }
      }
    }
    return getDescribeClusterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse> getUpdateClusterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateCluster",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse> getUpdateClusterMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse> getUpdateClusterMethod;
    if ((getUpdateClusterMethod = OnPremisesApiGrpc.getUpdateClusterMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getUpdateClusterMethod = OnPremisesApiGrpc.getUpdateClusterMethod) == null) {
          OnPremisesApiGrpc.getUpdateClusterMethod = getUpdateClusterMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateCluster"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("UpdateCluster"))
              .build();
        }
      }
    }
    return getUpdateClusterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse> getExtractClustersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExtractClusters",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse> getExtractClustersMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse> getExtractClustersMethod;
    if ((getExtractClustersMethod = OnPremisesApiGrpc.getExtractClustersMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getExtractClustersMethod = OnPremisesApiGrpc.getExtractClustersMethod) == null) {
          OnPremisesApiGrpc.getExtractClustersMethod = getExtractClustersMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExtractClusters"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("ExtractClusters"))
              .build();
        }
      }
    }
    return getExtractClustersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse> getListClusterServicesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListClusterServices",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse> getListClusterServicesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse> getListClusterServicesMethod;
    if ((getListClusterServicesMethod = OnPremisesApiGrpc.getListClusterServicesMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getListClusterServicesMethod = OnPremisesApiGrpc.getListClusterServicesMethod) == null) {
          OnPremisesApiGrpc.getListClusterServicesMethod = getListClusterServicesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListClusterServices"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("ListClusterServices"))
              .build();
        }
      }
    }
    return getListClusterServicesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse> getListLocationsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListLocations",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse> getListLocationsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse> getListLocationsMethod;
    if ((getListLocationsMethod = OnPremisesApiGrpc.getListLocationsMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getListLocationsMethod = OnPremisesApiGrpc.getListLocationsMethod) == null) {
          OnPremisesApiGrpc.getListLocationsMethod = getListLocationsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListLocations"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("ListLocations"))
              .build();
        }
      }
    }
    return getListLocationsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse> getSyncClusterDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SyncClusterData",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse> getSyncClusterDataMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse> getSyncClusterDataMethod;
    if ((getSyncClusterDataMethod = OnPremisesApiGrpc.getSyncClusterDataMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getSyncClusterDataMethod = OnPremisesApiGrpc.getSyncClusterDataMethod) == null) {
          OnPremisesApiGrpc.getSyncClusterDataMethod = getSyncClusterDataMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SyncClusterData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("SyncClusterData"))
              .build();
        }
      }
    }
    return getSyncClusterDataMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse> getDeleteClusterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteCluster",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse> getDeleteClusterMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse> getDeleteClusterMethod;
    if ((getDeleteClusterMethod = OnPremisesApiGrpc.getDeleteClusterMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getDeleteClusterMethod = OnPremisesApiGrpc.getDeleteClusterMethod) == null) {
          OnPremisesApiGrpc.getDeleteClusterMethod = getDeleteClusterMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteCluster"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("DeleteCluster"))
              .build();
        }
      }
    }
    return getDeleteClusterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse> getListPartiallyRegisteredClustersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListPartiallyRegisteredClusters",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse> getListPartiallyRegisteredClustersMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse> getListPartiallyRegisteredClustersMethod;
    if ((getListPartiallyRegisteredClustersMethod = OnPremisesApiGrpc.getListPartiallyRegisteredClustersMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getListPartiallyRegisteredClustersMethod = OnPremisesApiGrpc.getListPartiallyRegisteredClustersMethod) == null) {
          OnPremisesApiGrpc.getListPartiallyRegisteredClustersMethod = getListPartiallyRegisteredClustersMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListPartiallyRegisteredClusters"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("ListPartiallyRegisteredClusters"))
              .build();
        }
      }
    }
    return getListPartiallyRegisteredClustersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse> getSetupClusterConnectivityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetupClusterConnectivity",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse> getSetupClusterConnectivityMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse> getSetupClusterConnectivityMethod;
    if ((getSetupClusterConnectivityMethod = OnPremisesApiGrpc.getSetupClusterConnectivityMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getSetupClusterConnectivityMethod = OnPremisesApiGrpc.getSetupClusterConnectivityMethod) == null) {
          OnPremisesApiGrpc.getSetupClusterConnectivityMethod = getSetupClusterConnectivityMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetupClusterConnectivity"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("SetupClusterConnectivity"))
              .build();
        }
      }
    }
    return getSetupClusterConnectivityMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse> getDescribePartiallyRegisteredClusterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribePartiallyRegisteredCluster",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse> getDescribePartiallyRegisteredClusterMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse> getDescribePartiallyRegisteredClusterMethod;
    if ((getDescribePartiallyRegisteredClusterMethod = OnPremisesApiGrpc.getDescribePartiallyRegisteredClusterMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getDescribePartiallyRegisteredClusterMethod = OnPremisesApiGrpc.getDescribePartiallyRegisteredClusterMethod) == null) {
          OnPremisesApiGrpc.getDescribePartiallyRegisteredClusterMethod = getDescribePartiallyRegisteredClusterMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribePartiallyRegisteredCluster"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("DescribePartiallyRegisteredCluster"))
              .build();
        }
      }
    }
    return getDescribePartiallyRegisteredClusterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse> getRegisterAgentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterAgent",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse> getRegisterAgentMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse> getRegisterAgentMethod;
    if ((getRegisterAgentMethod = OnPremisesApiGrpc.getRegisterAgentMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getRegisterAgentMethod = OnPremisesApiGrpc.getRegisterAgentMethod) == null) {
          OnPremisesApiGrpc.getRegisterAgentMethod = getRegisterAgentMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterAgent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("RegisterAgent"))
              .build();
        }
      }
    }
    return getRegisterAgentMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse> getExtractSetupScriptMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExtractSetupScript",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse> getExtractSetupScriptMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse> getExtractSetupScriptMethod;
    if ((getExtractSetupScriptMethod = OnPremisesApiGrpc.getExtractSetupScriptMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getExtractSetupScriptMethod = OnPremisesApiGrpc.getExtractSetupScriptMethod) == null) {
          OnPremisesApiGrpc.getExtractSetupScriptMethod = getExtractSetupScriptMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExtractSetupScript"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("ExtractSetupScript"))
              .build();
        }
      }
    }
    return getExtractSetupScriptMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse> getUpdateClusterStateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateClusterState",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse> getUpdateClusterStateMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse> getUpdateClusterStateMethod;
    if ((getUpdateClusterStateMethod = OnPremisesApiGrpc.getUpdateClusterStateMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getUpdateClusterStateMethod = OnPremisesApiGrpc.getUpdateClusterStateMethod) == null) {
          OnPremisesApiGrpc.getUpdateClusterStateMethod = getUpdateClusterStateMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateClusterState"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("UpdateClusterState"))
              .build();
        }
      }
    }
    return getUpdateClusterStateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse> getCheckClusterConnectivityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CheckClusterConnectivity",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse> getCheckClusterConnectivityMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse> getCheckClusterConnectivityMethod;
    if ((getCheckClusterConnectivityMethod = OnPremisesApiGrpc.getCheckClusterConnectivityMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getCheckClusterConnectivityMethod = OnPremisesApiGrpc.getCheckClusterConnectivityMethod) == null) {
          OnPremisesApiGrpc.getCheckClusterConnectivityMethod = getCheckClusterConnectivityMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CheckClusterConnectivity"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("CheckClusterConnectivity"))
              .build();
        }
      }
    }
    return getCheckClusterConnectivityMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse> getDeletePartiallyRegisteredClusterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeletePartiallyRegisteredCluster",
      requestType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest.class,
      responseType = com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest,
      com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse> getDeletePartiallyRegisteredClusterMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse> getDeletePartiallyRegisteredClusterMethod;
    if ((getDeletePartiallyRegisteredClusterMethod = OnPremisesApiGrpc.getDeletePartiallyRegisteredClusterMethod) == null) {
      synchronized (OnPremisesApiGrpc.class) {
        if ((getDeletePartiallyRegisteredClusterMethod = OnPremisesApiGrpc.getDeletePartiallyRegisteredClusterMethod) == null) {
          OnPremisesApiGrpc.getDeletePartiallyRegisteredClusterMethod = getDeletePartiallyRegisteredClusterMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest, com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeletePartiallyRegisteredCluster"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OnPremisesApiMethodDescriptorSupplier("DeletePartiallyRegisteredCluster"))
              .build();
        }
      }
    }
    return getDeletePartiallyRegisteredClusterMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static OnPremisesApiStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<OnPremisesApiStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<OnPremisesApiStub>() {
        @java.lang.Override
        public OnPremisesApiStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new OnPremisesApiStub(channel, callOptions);
        }
      };
    return OnPremisesApiStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static OnPremisesApiBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<OnPremisesApiBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<OnPremisesApiBlockingV2Stub>() {
        @java.lang.Override
        public OnPremisesApiBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new OnPremisesApiBlockingV2Stub(channel, callOptions);
        }
      };
    return OnPremisesApiBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static OnPremisesApiBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<OnPremisesApiBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<OnPremisesApiBlockingStub>() {
        @java.lang.Override
        public OnPremisesApiBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new OnPremisesApiBlockingStub(channel, callOptions);
        }
      };
    return OnPremisesApiBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static OnPremisesApiFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<OnPremisesApiFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<OnPremisesApiFutureStub>() {
        @java.lang.Override
        public OnPremisesApiFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new OnPremisesApiFutureStub(channel, callOptions);
        }
      };
    return OnPremisesApiFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * OnPremises Service :: OnPremises Service is a web service to manage the on-prem clusters and control planes.
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Lists all registered clusters. :: Lists all registered on-premise clusters.
     * </pre>
     */
    default void listClusters(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListClustersMethod(), responseObserver);
    }

    /**
     * <pre>
     * Register an existing cluster :: Register an existing cluster as an on-premise cluster.
     * </pre>
     */
    default void registerCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterClusterMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get cluster details. :: Get details of a registered on-premise cluster.
     * </pre>
     */
    default void describeCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeClusterMethod(), responseObserver);
    }

    /**
     * <pre>
     * Update an existing cluster :: Update registration of an on-premise cluster.
     * </pre>
     */
    default void updateCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateClusterMethod(), responseObserver);
    }

    /**
     * <pre>
     * Extract clusters :: Extracts cluster information by using authenticated access to the cluster-manager.
     * </pre>
     */
    default void extractClusters(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getExtractClustersMethod(), responseObserver);
    }

    /**
     * <pre>
     * List service details of a cluster. :: Gets the service details of a cluster.
     * </pre>
     */
    default void listClusterServices(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListClusterServicesMethod(), responseObserver);
    }

    /**
     * <pre>
     * List the locations and its details. :: List the locations and its details.
     * </pre>
     */
    default void listLocations(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListLocationsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Syncs the remote cluster data with the latest cluster service details. :: Syncs the remote cluster data with the latest cluster service details.
     * </pre>
     */
    default void syncClusterData(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSyncClusterDataMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete a cluster. :: Delete a cluster.
     * </pre>
     */
    default void deleteCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteClusterMethod(), responseObserver);
    }

    /**
     * <pre>
     * List partially registered clusters. :: API to list all the partially registered clusters.
     * </pre>
     */
    default void listPartiallyRegisteredClusters(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListPartiallyRegisteredClustersMethod(), responseObserver);
    }

    /**
     * <pre>
     * Set-up Cluster Connectivity. :: Sets the cluster connectivity for the unregistered cluster.
     * </pre>
     */
    default void setupClusterConnectivity(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetupClusterConnectivityMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describes the partially registered cluster. :: Describes the partially registered cluster.
     * </pre>
     */
    default void describePartiallyRegisteredCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribePartiallyRegisteredClusterMethod(), responseObserver);
    }

    /**
     * <pre>
     * Register agent API call. :: Registers the agent with cloudera manager.
     * </pre>
     */
    default void registerAgent(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterAgentMethod(), responseObserver);
    }

    /**
     * <pre>
     * Extract the setup script. :: Extract the setup script for CCM_V2 installation.
     * </pre>
     */
    default void extractSetupScript(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getExtractSetupScriptMethod(), responseObserver);
    }

    /**
     * <pre>
     * Updates the status of partially registered cluster . :: Updates the status of partially registered cluster
     * </pre>
     */
    default void updateClusterState(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateClusterStateMethod(), responseObserver);
    }

    /**
     * <pre>
     * Check cluster connectivity. :: Checks the connection between CDP and Cloudera Manager.
     * </pre>
     */
    default void checkClusterConnectivity(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckClusterConnectivityMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete partially registered cluster. :: Delete the partially registered cluster.
     * </pre>
     */
    default void deletePartiallyRegisteredCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeletePartiallyRegisteredClusterMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service OnPremisesApi.
   * <pre>
   * OnPremises Service :: OnPremises Service is a web service to manage the on-prem clusters and control planes.
   * </pre>
   */
  public static abstract class OnPremisesApiImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return OnPremisesApiGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service OnPremisesApi.
   * <pre>
   * OnPremises Service :: OnPremises Service is a web service to manage the on-prem clusters and control planes.
   * </pre>
   */
  public static final class OnPremisesApiStub
      extends io.grpc.stub.AbstractAsyncStub<OnPremisesApiStub> {
    private OnPremisesApiStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected OnPremisesApiStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new OnPremisesApiStub(channel, callOptions);
    }

    /**
     * <pre>
     * Lists all registered clusters. :: Lists all registered on-premise clusters.
     * </pre>
     */
    public void listClusters(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListClustersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Register an existing cluster :: Register an existing cluster as an on-premise cluster.
     * </pre>
     */
    public void registerCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterClusterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get cluster details. :: Get details of a registered on-premise cluster.
     * </pre>
     */
    public void describeCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeClusterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Update an existing cluster :: Update registration of an on-premise cluster.
     * </pre>
     */
    public void updateCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateClusterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Extract clusters :: Extracts cluster information by using authenticated access to the cluster-manager.
     * </pre>
     */
    public void extractClusters(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getExtractClustersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List service details of a cluster. :: Gets the service details of a cluster.
     * </pre>
     */
    public void listClusterServices(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListClusterServicesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List the locations and its details. :: List the locations and its details.
     * </pre>
     */
    public void listLocations(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListLocationsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Syncs the remote cluster data with the latest cluster service details. :: Syncs the remote cluster data with the latest cluster service details.
     * </pre>
     */
    public void syncClusterData(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSyncClusterDataMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete a cluster. :: Delete a cluster.
     * </pre>
     */
    public void deleteCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteClusterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List partially registered clusters. :: API to list all the partially registered clusters.
     * </pre>
     */
    public void listPartiallyRegisteredClusters(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListPartiallyRegisteredClustersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set-up Cluster Connectivity. :: Sets the cluster connectivity for the unregistered cluster.
     * </pre>
     */
    public void setupClusterConnectivity(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetupClusterConnectivityMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describes the partially registered cluster. :: Describes the partially registered cluster.
     * </pre>
     */
    public void describePartiallyRegisteredCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribePartiallyRegisteredClusterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Register agent API call. :: Registers the agent with cloudera manager.
     * </pre>
     */
    public void registerAgent(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterAgentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Extract the setup script. :: Extract the setup script for CCM_V2 installation.
     * </pre>
     */
    public void extractSetupScript(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getExtractSetupScriptMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Updates the status of partially registered cluster . :: Updates the status of partially registered cluster
     * </pre>
     */
    public void updateClusterState(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateClusterStateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Check cluster connectivity. :: Checks the connection between CDP and Cloudera Manager.
     * </pre>
     */
    public void checkClusterConnectivity(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCheckClusterConnectivityMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete partially registered cluster. :: Delete the partially registered cluster.
     * </pre>
     */
    public void deletePartiallyRegisteredCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeletePartiallyRegisteredClusterMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service OnPremisesApi.
   * <pre>
   * OnPremises Service :: OnPremises Service is a web service to manage the on-prem clusters and control planes.
   * </pre>
   */
  public static final class OnPremisesApiBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<OnPremisesApiBlockingV2Stub> {
    private OnPremisesApiBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected OnPremisesApiBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new OnPremisesApiBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Lists all registered clusters. :: Lists all registered on-premise clusters.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse listClusters(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListClustersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Register an existing cluster :: Register an existing cluster as an on-premise cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse registerCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRegisterClusterMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get cluster details. :: Get details of a registered on-premise cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse describeCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDescribeClusterMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update an existing cluster :: Update registration of an on-premise cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse updateCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUpdateClusterMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Extract clusters :: Extracts cluster information by using authenticated access to the cluster-manager.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse extractClusters(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getExtractClustersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List service details of a cluster. :: Gets the service details of a cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse listClusterServices(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListClusterServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List the locations and its details. :: List the locations and its details.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse listLocations(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListLocationsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Syncs the remote cluster data with the latest cluster service details. :: Syncs the remote cluster data with the latest cluster service details.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse syncClusterData(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getSyncClusterDataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a cluster. :: Delete a cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse deleteCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteClusterMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List partially registered clusters. :: API to list all the partially registered clusters.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse listPartiallyRegisteredClusters(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListPartiallyRegisteredClustersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set-up Cluster Connectivity. :: Sets the cluster connectivity for the unregistered cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse setupClusterConnectivity(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getSetupClusterConnectivityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describes the partially registered cluster. :: Describes the partially registered cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse describePartiallyRegisteredCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDescribePartiallyRegisteredClusterMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Register agent API call. :: Registers the agent with cloudera manager.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse registerAgent(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRegisterAgentMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Extract the setup script. :: Extract the setup script for CCM_V2 installation.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse extractSetupScript(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getExtractSetupScriptMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Updates the status of partially registered cluster . :: Updates the status of partially registered cluster
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse updateClusterState(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUpdateClusterStateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Check cluster connectivity. :: Checks the connection between CDP and Cloudera Manager.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse checkClusterConnectivity(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCheckClusterConnectivityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete partially registered cluster. :: Delete the partially registered cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse deletePartiallyRegisteredCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeletePartiallyRegisteredClusterMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service OnPremisesApi.
   * <pre>
   * OnPremises Service :: OnPremises Service is a web service to manage the on-prem clusters and control planes.
   * </pre>
   */
  public static final class OnPremisesApiBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<OnPremisesApiBlockingStub> {
    private OnPremisesApiBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected OnPremisesApiBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new OnPremisesApiBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Lists all registered clusters. :: Lists all registered on-premise clusters.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse listClusters(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListClustersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Register an existing cluster :: Register an existing cluster as an on-premise cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse registerCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterClusterMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get cluster details. :: Get details of a registered on-premise cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse describeCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeClusterMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update an existing cluster :: Update registration of an on-premise cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse updateCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateClusterMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Extract clusters :: Extracts cluster information by using authenticated access to the cluster-manager.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse extractClusters(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getExtractClustersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List service details of a cluster. :: Gets the service details of a cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse listClusterServices(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListClusterServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List the locations and its details. :: List the locations and its details.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse listLocations(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListLocationsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Syncs the remote cluster data with the latest cluster service details. :: Syncs the remote cluster data with the latest cluster service details.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse syncClusterData(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSyncClusterDataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a cluster. :: Delete a cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse deleteCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteClusterMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List partially registered clusters. :: API to list all the partially registered clusters.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse listPartiallyRegisteredClusters(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListPartiallyRegisteredClustersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set-up Cluster Connectivity. :: Sets the cluster connectivity for the unregistered cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse setupClusterConnectivity(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetupClusterConnectivityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describes the partially registered cluster. :: Describes the partially registered cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse describePartiallyRegisteredCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribePartiallyRegisteredClusterMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Register agent API call. :: Registers the agent with cloudera manager.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse registerAgent(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterAgentMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Extract the setup script. :: Extract the setup script for CCM_V2 installation.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse extractSetupScript(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getExtractSetupScriptMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Updates the status of partially registered cluster . :: Updates the status of partially registered cluster
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse updateClusterState(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateClusterStateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Check cluster connectivity. :: Checks the connection between CDP and Cloudera Manager.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse checkClusterConnectivity(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCheckClusterConnectivityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete partially registered cluster. :: Delete the partially registered cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse deletePartiallyRegisteredCluster(com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeletePartiallyRegisteredClusterMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service OnPremisesApi.
   * <pre>
   * OnPremises Service :: OnPremises Service is a web service to manage the on-prem clusters and control planes.
   * </pre>
   */
  public static final class OnPremisesApiFutureStub
      extends io.grpc.stub.AbstractFutureStub<OnPremisesApiFutureStub> {
    private OnPremisesApiFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected OnPremisesApiFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new OnPremisesApiFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Lists all registered clusters. :: Lists all registered on-premise clusters.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse> listClusters(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListClustersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Register an existing cluster :: Register an existing cluster as an on-premise cluster.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse> registerCluster(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterClusterMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get cluster details. :: Get details of a registered on-premise cluster.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse> describeCluster(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeClusterMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Update an existing cluster :: Update registration of an on-premise cluster.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse> updateCluster(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateClusterMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Extract clusters :: Extracts cluster information by using authenticated access to the cluster-manager.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse> extractClusters(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getExtractClustersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List service details of a cluster. :: Gets the service details of a cluster.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse> listClusterServices(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListClusterServicesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List the locations and its details. :: List the locations and its details.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse> listLocations(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListLocationsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Syncs the remote cluster data with the latest cluster service details. :: Syncs the remote cluster data with the latest cluster service details.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse> syncClusterData(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSyncClusterDataMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete a cluster. :: Delete a cluster.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse> deleteCluster(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteClusterMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List partially registered clusters. :: API to list all the partially registered clusters.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse> listPartiallyRegisteredClusters(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListPartiallyRegisteredClustersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set-up Cluster Connectivity. :: Sets the cluster connectivity for the unregistered cluster.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse> setupClusterConnectivity(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetupClusterConnectivityMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describes the partially registered cluster. :: Describes the partially registered cluster.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse> describePartiallyRegisteredCluster(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribePartiallyRegisteredClusterMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Register agent API call. :: Registers the agent with cloudera manager.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse> registerAgent(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterAgentMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Extract the setup script. :: Extract the setup script for CCM_V2 installation.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse> extractSetupScript(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getExtractSetupScriptMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Updates the status of partially registered cluster . :: Updates the status of partially registered cluster
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse> updateClusterState(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateClusterStateMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Check cluster connectivity. :: Checks the connection between CDP and Cloudera Manager.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse> checkClusterConnectivity(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCheckClusterConnectivityMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete partially registered cluster. :: Delete the partially registered cluster.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse> deletePartiallyRegisteredCluster(
        com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeletePartiallyRegisteredClusterMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_CLUSTERS = 0;
  private static final int METHODID_REGISTER_CLUSTER = 1;
  private static final int METHODID_DESCRIBE_CLUSTER = 2;
  private static final int METHODID_UPDATE_CLUSTER = 3;
  private static final int METHODID_EXTRACT_CLUSTERS = 4;
  private static final int METHODID_LIST_CLUSTER_SERVICES = 5;
  private static final int METHODID_LIST_LOCATIONS = 6;
  private static final int METHODID_SYNC_CLUSTER_DATA = 7;
  private static final int METHODID_DELETE_CLUSTER = 8;
  private static final int METHODID_LIST_PARTIALLY_REGISTERED_CLUSTERS = 9;
  private static final int METHODID_SETUP_CLUSTER_CONNECTIVITY = 10;
  private static final int METHODID_DESCRIBE_PARTIALLY_REGISTERED_CLUSTER = 11;
  private static final int METHODID_REGISTER_AGENT = 12;
  private static final int METHODID_EXTRACT_SETUP_SCRIPT = 13;
  private static final int METHODID_UPDATE_CLUSTER_STATE = 14;
  private static final int METHODID_CHECK_CLUSTER_CONNECTIVITY = 15;
  private static final int METHODID_DELETE_PARTIALLY_REGISTERED_CLUSTER = 16;

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
        case METHODID_LIST_CLUSTERS:
          serviceImpl.listClusters((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse>) responseObserver);
          break;
        case METHODID_REGISTER_CLUSTER:
          serviceImpl.registerCluster((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_CLUSTER:
          serviceImpl.describeCluster((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse>) responseObserver);
          break;
        case METHODID_UPDATE_CLUSTER:
          serviceImpl.updateCluster((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse>) responseObserver);
          break;
        case METHODID_EXTRACT_CLUSTERS:
          serviceImpl.extractClusters((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse>) responseObserver);
          break;
        case METHODID_LIST_CLUSTER_SERVICES:
          serviceImpl.listClusterServices((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse>) responseObserver);
          break;
        case METHODID_LIST_LOCATIONS:
          serviceImpl.listLocations((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse>) responseObserver);
          break;
        case METHODID_SYNC_CLUSTER_DATA:
          serviceImpl.syncClusterData((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse>) responseObserver);
          break;
        case METHODID_DELETE_CLUSTER:
          serviceImpl.deleteCluster((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse>) responseObserver);
          break;
        case METHODID_LIST_PARTIALLY_REGISTERED_CLUSTERS:
          serviceImpl.listPartiallyRegisteredClusters((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse>) responseObserver);
          break;
        case METHODID_SETUP_CLUSTER_CONNECTIVITY:
          serviceImpl.setupClusterConnectivity((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_PARTIALLY_REGISTERED_CLUSTER:
          serviceImpl.describePartiallyRegisteredCluster((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse>) responseObserver);
          break;
        case METHODID_REGISTER_AGENT:
          serviceImpl.registerAgent((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse>) responseObserver);
          break;
        case METHODID_EXTRACT_SETUP_SCRIPT:
          serviceImpl.extractSetupScript((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse>) responseObserver);
          break;
        case METHODID_UPDATE_CLUSTER_STATE:
          serviceImpl.updateClusterState((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse>) responseObserver);
          break;
        case METHODID_CHECK_CLUSTER_CONNECTIVITY:
          serviceImpl.checkClusterConnectivity((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse>) responseObserver);
          break;
        case METHODID_DELETE_PARTIALLY_REGISTERED_CLUSTER:
          serviceImpl.deletePartiallyRegisteredCluster((com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse>) responseObserver);
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
          getListClustersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClustersResponse>(
                service, METHODID_LIST_CLUSTERS)))
        .addMethod(
          getRegisterClusterMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterClusterResponse>(
                service, METHODID_REGISTER_CLUSTER)))
        .addMethod(
          getDescribeClusterMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribeClusterResponse>(
                service, METHODID_DESCRIBE_CLUSTER)))
        .addMethod(
          getUpdateClusterMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterResponse>(
                service, METHODID_UPDATE_CLUSTER)))
        .addMethod(
          getExtractClustersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractClustersResponse>(
                service, METHODID_EXTRACT_CLUSTERS)))
        .addMethod(
          getListClusterServicesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListClusterServicesResponse>(
                service, METHODID_LIST_CLUSTER_SERVICES)))
        .addMethod(
          getListLocationsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListLocationsResponse>(
                service, METHODID_LIST_LOCATIONS)))
        .addMethod(
          getSyncClusterDataMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SyncClusterDataResponse>(
                service, METHODID_SYNC_CLUSTER_DATA)))
        .addMethod(
          getDeleteClusterMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeleteClusterResponse>(
                service, METHODID_DELETE_CLUSTER)))
        .addMethod(
          getListPartiallyRegisteredClustersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ListPartiallyRegisteredClustersResponse>(
                service, METHODID_LIST_PARTIALLY_REGISTERED_CLUSTERS)))
        .addMethod(
          getSetupClusterConnectivityMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.SetupClusterConnectivityResponse>(
                service, METHODID_SETUP_CLUSTER_CONNECTIVITY)))
        .addMethod(
          getDescribePartiallyRegisteredClusterMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DescribePartiallyRegisteredClusterResponse>(
                service, METHODID_DESCRIBE_PARTIALLY_REGISTERED_CLUSTER)))
        .addMethod(
          getRegisterAgentMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.RegisterAgentResponse>(
                service, METHODID_REGISTER_AGENT)))
        .addMethod(
          getExtractSetupScriptMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.ExtractSetupScriptResponse>(
                service, METHODID_EXTRACT_SETUP_SCRIPT)))
        .addMethod(
          getUpdateClusterStateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.UpdateClusterStateResponse>(
                service, METHODID_UPDATE_CLUSTER_STATE)))
        .addMethod(
          getCheckClusterConnectivityMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.CheckClusterConnectivityResponse>(
                service, METHODID_CHECK_CLUSTER_CONNECTIVITY)))
        .addMethod(
          getDeletePartiallyRegisteredClusterMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterRequest,
              com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.DeletePartiallyRegisteredClusterResponse>(
                service, METHODID_DELETE_PARTIALLY_REGISTERED_CLUSTER)))
        .build();
  }

  private static abstract class OnPremisesApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    OnPremisesApiBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("OnPremisesApi");
    }
  }

  private static final class OnPremisesApiFileDescriptorSupplier
      extends OnPremisesApiBaseDescriptorSupplier {
    OnPremisesApiFileDescriptorSupplier() {}
  }

  private static final class OnPremisesApiMethodDescriptorSupplier
      extends OnPremisesApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    OnPremisesApiMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (OnPremisesApiGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new OnPremisesApiFileDescriptorSupplier())
              .addMethod(getListClustersMethod())
              .addMethod(getRegisterClusterMethod())
              .addMethod(getDescribeClusterMethod())
              .addMethod(getUpdateClusterMethod())
              .addMethod(getExtractClustersMethod())
              .addMethod(getListClusterServicesMethod())
              .addMethod(getListLocationsMethod())
              .addMethod(getSyncClusterDataMethod())
              .addMethod(getDeleteClusterMethod())
              .addMethod(getListPartiallyRegisteredClustersMethod())
              .addMethod(getSetupClusterConnectivityMethod())
              .addMethod(getDescribePartiallyRegisteredClusterMethod())
              .addMethod(getRegisterAgentMethod())
              .addMethod(getExtractSetupScriptMethod())
              .addMethod(getUpdateClusterStateMethod())
              .addMethod(getCheckClusterConnectivityMethod())
              .addMethod(getDeletePartiallyRegisteredClusterMethod())
              .build();
        }
      }
    }
    return result;
  }
}
