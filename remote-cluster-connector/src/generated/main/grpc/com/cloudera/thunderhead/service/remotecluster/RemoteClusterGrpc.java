package com.cloudera.thunderhead.service.remotecluster;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * RemoteCluster Service :: RemoteCluster Service is a web service to manage the on-prem clusters.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.64.0)",
    comments = "Source: remotecluster.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class RemoteClusterGrpc {

  private RemoteClusterGrpc() {}

  public static final java.lang.String SERVICE_NAME = "remotecluster.RemoteCluster";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneResponse> getRegisterPvcControlPlaneMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterPvcControlPlane",
      requestType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneRequest.class,
      responseType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneResponse> getRegisterPvcControlPlaneMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneResponse> getRegisterPvcControlPlaneMethod;
    if ((getRegisterPvcControlPlaneMethod = RemoteClusterGrpc.getRegisterPvcControlPlaneMethod) == null) {
      synchronized (RemoteClusterGrpc.class) {
        if ((getRegisterPvcControlPlaneMethod = RemoteClusterGrpc.getRegisterPvcControlPlaneMethod) == null) {
          RemoteClusterGrpc.getRegisterPvcControlPlaneMethod = getRegisterPvcControlPlaneMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterPvcControlPlane"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RemoteClusterMethodDescriptorSupplier("RegisterPvcControlPlane"))
              .build();
        }
      }
    }
    return getRegisterPvcControlPlaneMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneResponse> getUnregisterPvcControlPlaneMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnregisterPvcControlPlane",
      requestType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneRequest.class,
      responseType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneResponse> getUnregisterPvcControlPlaneMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneResponse> getUnregisterPvcControlPlaneMethod;
    if ((getUnregisterPvcControlPlaneMethod = RemoteClusterGrpc.getUnregisterPvcControlPlaneMethod) == null) {
      synchronized (RemoteClusterGrpc.class) {
        if ((getUnregisterPvcControlPlaneMethod = RemoteClusterGrpc.getUnregisterPvcControlPlaneMethod) == null) {
          RemoteClusterGrpc.getUnregisterPvcControlPlaneMethod = getUnregisterPvcControlPlaneMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnregisterPvcControlPlane"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RemoteClusterMethodDescriptorSupplier("UnregisterPvcControlPlane"))
              .build();
        }
      }
    }
    return getUnregisterPvcControlPlaneMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse> getDescribePvcControlPlaneMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribePvcControlPlane",
      requestType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest.class,
      responseType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse> getDescribePvcControlPlaneMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse> getDescribePvcControlPlaneMethod;
    if ((getDescribePvcControlPlaneMethod = RemoteClusterGrpc.getDescribePvcControlPlaneMethod) == null) {
      synchronized (RemoteClusterGrpc.class) {
        if ((getDescribePvcControlPlaneMethod = RemoteClusterGrpc.getDescribePvcControlPlaneMethod) == null) {
          RemoteClusterGrpc.getDescribePvcControlPlaneMethod = getDescribePvcControlPlaneMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribePvcControlPlane"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RemoteClusterMethodDescriptorSupplier("DescribePvcControlPlane"))
              .build();
        }
      }
    }
    return getDescribePvcControlPlaneMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse> getListPvcControlPlanesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListPvcControlPlanes",
      requestType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest.class,
      responseType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse> getListPvcControlPlanesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse> getListPvcControlPlanesMethod;
    if ((getListPvcControlPlanesMethod = RemoteClusterGrpc.getListPvcControlPlanesMethod) == null) {
      synchronized (RemoteClusterGrpc.class) {
        if ((getListPvcControlPlanesMethod = RemoteClusterGrpc.getListPvcControlPlanesMethod) == null) {
          RemoteClusterGrpc.getListPvcControlPlanesMethod = getListPvcControlPlanesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListPvcControlPlanes"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RemoteClusterMethodDescriptorSupplier("ListPvcControlPlanes"))
              .build();
        }
      }
    }
    return getListPvcControlPlanesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityResponse> getTestPvcControlPlaneConnectivityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "TestPvcControlPlaneConnectivity",
      requestType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityRequest.class,
      responseType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityResponse> getTestPvcControlPlaneConnectivityMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityResponse> getTestPvcControlPlaneConnectivityMethod;
    if ((getTestPvcControlPlaneConnectivityMethod = RemoteClusterGrpc.getTestPvcControlPlaneConnectivityMethod) == null) {
      synchronized (RemoteClusterGrpc.class) {
        if ((getTestPvcControlPlaneConnectivityMethod = RemoteClusterGrpc.getTestPvcControlPlaneConnectivityMethod) == null) {
          RemoteClusterGrpc.getTestPvcControlPlaneConnectivityMethod = getTestPvcControlPlaneConnectivityMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "TestPvcControlPlaneConnectivity"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RemoteClusterMethodDescriptorSupplier("TestPvcControlPlaneConnectivity"))
              .build();
        }
      }
    }
    return getTestPvcControlPlaneConnectivityMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersResponse> getListRemoteClustersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListRemoteClusters",
      requestType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersRequest.class,
      responseType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersResponse> getListRemoteClustersMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersResponse> getListRemoteClustersMethod;
    if ((getListRemoteClustersMethod = RemoteClusterGrpc.getListRemoteClustersMethod) == null) {
      synchronized (RemoteClusterGrpc.class) {
        if ((getListRemoteClustersMethod = RemoteClusterGrpc.getListRemoteClustersMethod) == null) {
          RemoteClusterGrpc.getListRemoteClustersMethod = getListRemoteClustersMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListRemoteClusters"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RemoteClusterMethodDescriptorSupplier("ListRemoteClusters"))
              .build();
        }
      }
    }
    return getListRemoteClustersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterResponse> getGetRemoteClusterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetRemoteCluster",
      requestType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterRequest.class,
      responseType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterResponse> getGetRemoteClusterMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterResponse> getGetRemoteClusterMethod;
    if ((getGetRemoteClusterMethod = RemoteClusterGrpc.getGetRemoteClusterMethod) == null) {
      synchronized (RemoteClusterGrpc.class) {
        if ((getGetRemoteClusterMethod = RemoteClusterGrpc.getGetRemoteClusterMethod) == null) {
          RemoteClusterGrpc.getGetRemoteClusterMethod = getGetRemoteClusterMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetRemoteCluster"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RemoteClusterMethodDescriptorSupplier("GetRemoteCluster"))
              .build();
        }
      }
    }
    return getGetRemoteClusterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsResponse> getGetServiceDetailsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetServiceDetails",
      requestType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsRequest.class,
      responseType = com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsRequest,
      com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsResponse> getGetServiceDetailsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsResponse> getGetServiceDetailsMethod;
    if ((getGetServiceDetailsMethod = RemoteClusterGrpc.getGetServiceDetailsMethod) == null) {
      synchronized (RemoteClusterGrpc.class) {
        if ((getGetServiceDetailsMethod = RemoteClusterGrpc.getGetServiceDetailsMethod) == null) {
          RemoteClusterGrpc.getGetServiceDetailsMethod = getGetServiceDetailsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsRequest, com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetServiceDetails"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RemoteClusterMethodDescriptorSupplier("GetServiceDetails"))
              .build();
        }
      }
    }
    return getGetServiceDetailsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RemoteClusterStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RemoteClusterStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RemoteClusterStub>() {
        @java.lang.Override
        public RemoteClusterStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RemoteClusterStub(channel, callOptions);
        }
      };
    return RemoteClusterStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RemoteClusterBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RemoteClusterBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RemoteClusterBlockingStub>() {
        @java.lang.Override
        public RemoteClusterBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RemoteClusterBlockingStub(channel, callOptions);
        }
      };
    return RemoteClusterBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RemoteClusterFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RemoteClusterFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RemoteClusterFutureStub>() {
        @java.lang.Override
        public RemoteClusterFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RemoteClusterFutureStub(channel, callOptions);
        }
      };
    return RemoteClusterFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * RemoteCluster Service :: RemoteCluster Service is a web service to manage the on-prem clusters.
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Register a Private Cloud control plane. :: Register a Private Cloud control plane in Public Cloud.
     * </pre>
     */
    default void registerPvcControlPlane(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterPvcControlPlaneMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unregister a Private Cloud control plane. :: Unregister a Private Cloud control plane from the Public Cloud.
     * </pre>
     */
    default void unregisterPvcControlPlane(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnregisterPvcControlPlaneMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe a registered Private Cloud control plane configuration. :: Describe a registered Private Cloud control plane configuration.
     * </pre>
     */
    default void describePvcControlPlane(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribePvcControlPlaneMethod(), responseObserver);
    }

    /**
     * <pre>
     * List registered Private Cloud control plane configurations. :: List registered Private Cloud control plane configurations.
     * </pre>
     */
    default void listPvcControlPlanes(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListPvcControlPlanesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Test the connectivity to the Private Cloud control plane. :: Test the connectivity from Public Cloud control plane to the registered Private Cloud control plane.
     * </pre>
     */
    default void testPvcControlPlaneConnectivity(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getTestPvcControlPlaneConnectivityMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists all the registered remote clusters. :: Lists all the registered remote clusters.
     * </pre>
     */
    default void listRemoteClusters(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListRemoteClustersMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the remote cluster details. :: Get the remote cluster details.
     * </pre>
     */
    default void getRemoteCluster(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetRemoteClusterMethod(), responseObserver);
    }

    /**
     * <pre>
     * Gets the service details of the cluster. :: Gets the service details of the cluster.
     * </pre>
     */
    default void getServiceDetails(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetServiceDetailsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service RemoteCluster.
   * <pre>
   * RemoteCluster Service :: RemoteCluster Service is a web service to manage the on-prem clusters.
   * </pre>
   */
  public static abstract class RemoteClusterImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return RemoteClusterGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service RemoteCluster.
   * <pre>
   * RemoteCluster Service :: RemoteCluster Service is a web service to manage the on-prem clusters.
   * </pre>
   */
  public static final class RemoteClusterStub
      extends io.grpc.stub.AbstractAsyncStub<RemoteClusterStub> {
    private RemoteClusterStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RemoteClusterStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RemoteClusterStub(channel, callOptions);
    }

    /**
     * <pre>
     * Register a Private Cloud control plane. :: Register a Private Cloud control plane in Public Cloud.
     * </pre>
     */
    public void registerPvcControlPlane(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterPvcControlPlaneMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unregister a Private Cloud control plane. :: Unregister a Private Cloud control plane from the Public Cloud.
     * </pre>
     */
    public void unregisterPvcControlPlane(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnregisterPvcControlPlaneMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe a registered Private Cloud control plane configuration. :: Describe a registered Private Cloud control plane configuration.
     * </pre>
     */
    public void describePvcControlPlane(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribePvcControlPlaneMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List registered Private Cloud control plane configurations. :: List registered Private Cloud control plane configurations.
     * </pre>
     */
    public void listPvcControlPlanes(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListPvcControlPlanesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Test the connectivity to the Private Cloud control plane. :: Test the connectivity from Public Cloud control plane to the registered Private Cloud control plane.
     * </pre>
     */
    public void testPvcControlPlaneConnectivity(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getTestPvcControlPlaneConnectivityMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists all the registered remote clusters. :: Lists all the registered remote clusters.
     * </pre>
     */
    public void listRemoteClusters(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListRemoteClustersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the remote cluster details. :: Get the remote cluster details.
     * </pre>
     */
    public void getRemoteCluster(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetRemoteClusterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Gets the service details of the cluster. :: Gets the service details of the cluster.
     * </pre>
     */
    public void getServiceDetails(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetServiceDetailsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service RemoteCluster.
   * <pre>
   * RemoteCluster Service :: RemoteCluster Service is a web service to manage the on-prem clusters.
   * </pre>
   */
  public static final class RemoteClusterBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<RemoteClusterBlockingStub> {
    private RemoteClusterBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RemoteClusterBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RemoteClusterBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Register a Private Cloud control plane. :: Register a Private Cloud control plane in Public Cloud.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneResponse registerPvcControlPlane(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterPvcControlPlaneMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unregister a Private Cloud control plane. :: Unregister a Private Cloud control plane from the Public Cloud.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneResponse unregisterPvcControlPlane(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnregisterPvcControlPlaneMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe a registered Private Cloud control plane configuration. :: Describe a registered Private Cloud control plane configuration.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse describePvcControlPlane(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribePvcControlPlaneMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List registered Private Cloud control plane configurations. :: List registered Private Cloud control plane configurations.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse listPvcControlPlanes(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListPvcControlPlanesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Test the connectivity to the Private Cloud control plane. :: Test the connectivity from Public Cloud control plane to the registered Private Cloud control plane.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityResponse testPvcControlPlaneConnectivity(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getTestPvcControlPlaneConnectivityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the registered remote clusters. :: Lists all the registered remote clusters.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersResponse listRemoteClusters(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListRemoteClustersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the remote cluster details. :: Get the remote cluster details.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterResponse getRemoteCluster(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetRemoteClusterMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Gets the service details of the cluster. :: Gets the service details of the cluster.
     * </pre>
     */
    public com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsResponse getServiceDetails(com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetServiceDetailsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service RemoteCluster.
   * <pre>
   * RemoteCluster Service :: RemoteCluster Service is a web service to manage the on-prem clusters.
   * </pre>
   */
  public static final class RemoteClusterFutureStub
      extends io.grpc.stub.AbstractFutureStub<RemoteClusterFutureStub> {
    private RemoteClusterFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RemoteClusterFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RemoteClusterFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Register a Private Cloud control plane. :: Register a Private Cloud control plane in Public Cloud.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneResponse> registerPvcControlPlane(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterPvcControlPlaneMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unregister a Private Cloud control plane. :: Unregister a Private Cloud control plane from the Public Cloud.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneResponse> unregisterPvcControlPlane(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnregisterPvcControlPlaneMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe a registered Private Cloud control plane configuration. :: Describe a registered Private Cloud control plane configuration.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse> describePvcControlPlane(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribePvcControlPlaneMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List registered Private Cloud control plane configurations. :: List registered Private Cloud control plane configurations.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse> listPvcControlPlanes(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListPvcControlPlanesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Test the connectivity to the Private Cloud control plane. :: Test the connectivity from Public Cloud control plane to the registered Private Cloud control plane.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityResponse> testPvcControlPlaneConnectivity(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getTestPvcControlPlaneConnectivityMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists all the registered remote clusters. :: Lists all the registered remote clusters.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersResponse> listRemoteClusters(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListRemoteClustersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the remote cluster details. :: Get the remote cluster details.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterResponse> getRemoteCluster(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetRemoteClusterMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Gets the service details of the cluster. :: Gets the service details of the cluster.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsResponse> getServiceDetails(
        com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetServiceDetailsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REGISTER_PVC_CONTROL_PLANE = 0;
  private static final int METHODID_UNREGISTER_PVC_CONTROL_PLANE = 1;
  private static final int METHODID_DESCRIBE_PVC_CONTROL_PLANE = 2;
  private static final int METHODID_LIST_PVC_CONTROL_PLANES = 3;
  private static final int METHODID_TEST_PVC_CONTROL_PLANE_CONNECTIVITY = 4;
  private static final int METHODID_LIST_REMOTE_CLUSTERS = 5;
  private static final int METHODID_GET_REMOTE_CLUSTER = 6;
  private static final int METHODID_GET_SERVICE_DETAILS = 7;

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
        case METHODID_REGISTER_PVC_CONTROL_PLANE:
          serviceImpl.registerPvcControlPlane((com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneResponse>) responseObserver);
          break;
        case METHODID_UNREGISTER_PVC_CONTROL_PLANE:
          serviceImpl.unregisterPvcControlPlane((com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_PVC_CONTROL_PLANE:
          serviceImpl.describePvcControlPlane((com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse>) responseObserver);
          break;
        case METHODID_LIST_PVC_CONTROL_PLANES:
          serviceImpl.listPvcControlPlanes((com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse>) responseObserver);
          break;
        case METHODID_TEST_PVC_CONTROL_PLANE_CONNECTIVITY:
          serviceImpl.testPvcControlPlaneConnectivity((com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityResponse>) responseObserver);
          break;
        case METHODID_LIST_REMOTE_CLUSTERS:
          serviceImpl.listRemoteClusters((com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersResponse>) responseObserver);
          break;
        case METHODID_GET_REMOTE_CLUSTER:
          serviceImpl.getRemoteCluster((com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterResponse>) responseObserver);
          break;
        case METHODID_GET_SERVICE_DETAILS:
          serviceImpl.getServiceDetails((com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsResponse>) responseObserver);
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
          getRegisterPvcControlPlaneMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneRequest,
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.RegisterPvcControlPlaneResponse>(
                service, METHODID_REGISTER_PVC_CONTROL_PLANE)))
        .addMethod(
          getUnregisterPvcControlPlaneMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneRequest,
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.UnregisterPvcControlPlaneResponse>(
                service, METHODID_UNREGISTER_PVC_CONTROL_PLANE)))
        .addMethod(
          getDescribePvcControlPlaneMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest,
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse>(
                service, METHODID_DESCRIBE_PVC_CONTROL_PLANE)))
        .addMethod(
          getListPvcControlPlanesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest,
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse>(
                service, METHODID_LIST_PVC_CONTROL_PLANES)))
        .addMethod(
          getTestPvcControlPlaneConnectivityMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityRequest,
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.TestPvcControlPlaneConnectivityResponse>(
                service, METHODID_TEST_PVC_CONTROL_PLANE_CONNECTIVITY)))
        .addMethod(
          getListRemoteClustersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersRequest,
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListRemoteClustersResponse>(
                service, METHODID_LIST_REMOTE_CLUSTERS)))
        .addMethod(
          getGetRemoteClusterMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterRequest,
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetRemoteClusterResponse>(
                service, METHODID_GET_REMOTE_CLUSTER)))
        .addMethod(
          getGetServiceDetailsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsRequest,
              com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.GetServiceDetailsResponse>(
                service, METHODID_GET_SERVICE_DETAILS)))
        .build();
  }

  private static abstract class RemoteClusterBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RemoteClusterBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RemoteCluster");
    }
  }

  private static final class RemoteClusterFileDescriptorSupplier
      extends RemoteClusterBaseDescriptorSupplier {
    RemoteClusterFileDescriptorSupplier() {}
  }

  private static final class RemoteClusterMethodDescriptorSupplier
      extends RemoteClusterBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    RemoteClusterMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (RemoteClusterGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RemoteClusterFileDescriptorSupplier())
              .addMethod(getRegisterPvcControlPlaneMethod())
              .addMethod(getUnregisterPvcControlPlaneMethod())
              .addMethod(getDescribePvcControlPlaneMethod())
              .addMethod(getListPvcControlPlanesMethod())
              .addMethod(getTestPvcControlPlaneConnectivityMethod())
              .addMethod(getListRemoteClustersMethod())
              .addMethod(getGetRemoteClusterMethod())
              .addMethod(getGetServiceDetailsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
