package com.cloudera.thunderhead.service.minasshdmanagement;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class MinaSshdManagementGrpc {

  private MinaSshdManagementGrpc() {}

  public static final java.lang.String SERVICE_NAME = "minasshdmanagement.MinaSshdManagement";

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
    if ((getGetVersionMethod = MinaSshdManagementGrpc.getGetVersionMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getGetVersionMethod = MinaSshdManagementGrpc.getGetVersionMethod) == null) {
          MinaSshdManagementGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse> getAcquireMinaSshdServiceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AcquireMinaSshdService",
      requestType = com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest.class,
      responseType = com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse> getAcquireMinaSshdServiceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse> getAcquireMinaSshdServiceMethod;
    if ((getAcquireMinaSshdServiceMethod = MinaSshdManagementGrpc.getAcquireMinaSshdServiceMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getAcquireMinaSshdServiceMethod = MinaSshdManagementGrpc.getAcquireMinaSshdServiceMethod) == null) {
          MinaSshdManagementGrpc.getAcquireMinaSshdServiceMethod = getAcquireMinaSshdServiceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AcquireMinaSshdService"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse> getListMinaSshdServicesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListMinaSshdServices",
      requestType = com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest.class,
      responseType = com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse> getListMinaSshdServicesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse> getListMinaSshdServicesMethod;
    if ((getListMinaSshdServicesMethod = MinaSshdManagementGrpc.getListMinaSshdServicesMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getListMinaSshdServicesMethod = MinaSshdManagementGrpc.getListMinaSshdServicesMethod) == null) {
          MinaSshdManagementGrpc.getListMinaSshdServicesMethod = getListMinaSshdServicesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListMinaSshdServices"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse> getGenerateAndRegisterSshTunnelingKeyPairMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GenerateAndRegisterSshTunnelingKeyPair",
      requestType = com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest.class,
      responseType = com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse> getGenerateAndRegisterSshTunnelingKeyPairMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse> getGenerateAndRegisterSshTunnelingKeyPairMethod;
    if ((getGenerateAndRegisterSshTunnelingKeyPairMethod = MinaSshdManagementGrpc.getGenerateAndRegisterSshTunnelingKeyPairMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getGenerateAndRegisterSshTunnelingKeyPairMethod = MinaSshdManagementGrpc.getGenerateAndRegisterSshTunnelingKeyPairMethod) == null) {
          MinaSshdManagementGrpc.getGenerateAndRegisterSshTunnelingKeyPairMethod = getGenerateAndRegisterSshTunnelingKeyPairMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GenerateAndRegisterSshTunnelingKeyPair"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse> getRegisterSshTunnelingKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterSshTunnelingKey",
      requestType = com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse> getRegisterSshTunnelingKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse> getRegisterSshTunnelingKeyMethod;
    if ((getRegisterSshTunnelingKeyMethod = MinaSshdManagementGrpc.getRegisterSshTunnelingKeyMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getRegisterSshTunnelingKeyMethod = MinaSshdManagementGrpc.getRegisterSshTunnelingKeyMethod) == null) {
          MinaSshdManagementGrpc.getRegisterSshTunnelingKeyMethod = getRegisterSshTunnelingKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterSshTunnelingKey"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> getUnregisterSshTunnelingKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnregisterSshTunnelingKey",
      requestType = com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> getUnregisterSshTunnelingKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> getUnregisterSshTunnelingKeyMethod;
    if ((getUnregisterSshTunnelingKeyMethod = MinaSshdManagementGrpc.getUnregisterSshTunnelingKeyMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getUnregisterSshTunnelingKeyMethod = MinaSshdManagementGrpc.getUnregisterSshTunnelingKeyMethod) == null) {
          MinaSshdManagementGrpc.getUnregisterSshTunnelingKeyMethod = getUnregisterSshTunnelingKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnregisterSshTunnelingKey"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> getListSshTunnelingKeysMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListSshTunnelingKeys",
      requestType = com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest.class,
      responseType = com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest,
      com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> getListSshTunnelingKeysMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> getListSshTunnelingKeysMethod;
    if ((getListSshTunnelingKeysMethod = MinaSshdManagementGrpc.getListSshTunnelingKeysMethod) == null) {
      synchronized (MinaSshdManagementGrpc.class) {
        if ((getListSshTunnelingKeysMethod = MinaSshdManagementGrpc.getListSshTunnelingKeysMethod) == null) {
          MinaSshdManagementGrpc.getListSshTunnelingKeysMethod = getListSshTunnelingKeysMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest, com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListSshTunnelingKeys"))
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
    io.grpc.stub.AbstractStub.StubFactory<MinaSshdManagementStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MinaSshdManagementStub>() {
        @java.lang.Override
        public MinaSshdManagementStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MinaSshdManagementStub(channel, callOptions);
        }
      };
    return MinaSshdManagementStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static MinaSshdManagementBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MinaSshdManagementBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MinaSshdManagementBlockingV2Stub>() {
        @java.lang.Override
        public MinaSshdManagementBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MinaSshdManagementBlockingV2Stub(channel, callOptions);
        }
      };
    return MinaSshdManagementBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MinaSshdManagementBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MinaSshdManagementBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MinaSshdManagementBlockingStub>() {
        @java.lang.Override
        public MinaSshdManagementBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MinaSshdManagementBlockingStub(channel, callOptions);
        }
      };
    return MinaSshdManagementBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MinaSshdManagementFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MinaSshdManagementFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MinaSshdManagementFutureStub>() {
        @java.lang.Override
        public MinaSshdManagementFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MinaSshdManagementFutureStub(channel, callOptions);
        }
      };
    return MinaSshdManagementFutureStub.newStub(factory, channel);
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
     * Get the service version.
     * </pre>
     */
    default void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetVersionMethod(), responseObserver);
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
    default void acquireMinaSshdService(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAcquireMinaSshdServiceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Currently there can only be one MinaSshdService open per account.
     * Use this to check if the MinaSshdService is ready as well as to get
     * the public key for the server.
     * </pre>
     */
    default void listMinaSshdServices(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListMinaSshdServicesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Generates a ssh key pair, registers the public key, and returns the
     * enciphered private key and public key
     * MinaSshdService must be ready or this will fail.
     * </pre>
     */
    default void generateAndRegisterSshTunnelingKeyPair(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGenerateAndRegisterSshTunnelingKeyPairMethod(), responseObserver);
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
    default void registerSshTunnelingKey(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterSshTunnelingKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unregister Ssh Tunneling Key
     * </pre>
     */
    default void unregisterSshTunnelingKey(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnregisterSshTunnelingKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * For minaSshdService
     * </pre>
     */
    default void listSshTunnelingKeys(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListSshTunnelingKeysMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service MinaSshdManagement.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class MinaSshdManagementImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return MinaSshdManagementGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service MinaSshdManagement.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MinaSshdManagementStub
      extends io.grpc.stub.AbstractAsyncStub<MinaSshdManagementStub> {
    private MinaSshdManagementStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MinaSshdManagementStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MinaSshdManagementStub(channel, callOptions);
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
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAcquireMinaSshdServiceMethod(), getCallOptions()), request, responseObserver);
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
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListMinaSshdServicesMethod(), getCallOptions()), request, responseObserver);
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
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGenerateAndRegisterSshTunnelingKeyPairMethod(), getCallOptions()), request, responseObserver);
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
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterSshTunnelingKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unregister Ssh Tunneling Key
     * </pre>
     */
    public void unregisterSshTunnelingKey(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnregisterSshTunnelingKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * For minaSshdService
     * </pre>
     */
    public void listSshTunnelingKeys(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListSshTunnelingKeysMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service MinaSshdManagement.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MinaSshdManagementBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<MinaSshdManagementBlockingV2Stub> {
    private MinaSshdManagementBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MinaSshdManagementBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MinaSshdManagementBlockingV2Stub(channel, callOptions);
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
     * Acquire an open MinaSshdService. This always returns a MinaSshdService
     * open to rececive new connections of which there will always be at most
     * one per account.
     * The MinaSshdService may not be ready. If one already exists, it will
     * be returned. If one does not, a new one will have been created, but the
     * initializing workflows may not have completed yet.
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse acquireMinaSshdService(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getAcquireMinaSshdServiceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Currently there can only be one MinaSshdService open per account.
     * Use this to check if the MinaSshdService is ready as well as to get
     * the public key for the server.
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse listMinaSshdServices(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListMinaSshdServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Generates a ssh key pair, registers the public key, and returns the
     * enciphered private key and public key
     * MinaSshdService must be ready or this will fail.
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse generateAndRegisterSshTunnelingKeyPair(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGenerateAndRegisterSshTunnelingKeyPairMethod(), getCallOptions(), request);
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
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse registerSshTunnelingKey(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRegisterSshTunnelingKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unregister Ssh Tunneling Key
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse unregisterSshTunnelingKey(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUnregisterSshTunnelingKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * For minaSshdService
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse listSshTunnelingKeys(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListSshTunnelingKeysMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service MinaSshdManagement.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MinaSshdManagementBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<MinaSshdManagementBlockingStub> {
    private MinaSshdManagementBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MinaSshdManagementBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MinaSshdManagementBlockingStub(channel, callOptions);
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
     * Acquire an open MinaSshdService. This always returns a MinaSshdService
     * open to rececive new connections of which there will always be at most
     * one per account.
     * The MinaSshdService may not be ready. If one already exists, it will
     * be returned. If one does not, a new one will have been created, but the
     * initializing workflows may not have completed yet.
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse acquireMinaSshdService(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAcquireMinaSshdServiceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Currently there can only be one MinaSshdService open per account.
     * Use this to check if the MinaSshdService is ready as well as to get
     * the public key for the server.
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse listMinaSshdServices(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListMinaSshdServicesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Generates a ssh key pair, registers the public key, and returns the
     * enciphered private key and public key
     * MinaSshdService must be ready or this will fail.
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse generateAndRegisterSshTunnelingKeyPair(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGenerateAndRegisterSshTunnelingKeyPairMethod(), getCallOptions(), request);
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
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterSshTunnelingKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unregister Ssh Tunneling Key
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse unregisterSshTunnelingKey(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnregisterSshTunnelingKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * For minaSshdService
     * </pre>
     */
    public com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse listSshTunnelingKeys(com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListSshTunnelingKeysMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service MinaSshdManagement.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class MinaSshdManagementFutureStub
      extends io.grpc.stub.AbstractFutureStub<MinaSshdManagementFutureStub> {
    private MinaSshdManagementFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MinaSshdManagementFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MinaSshdManagementFutureStub(channel, callOptions);
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
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAcquireMinaSshdServiceMethod(), getCallOptions()), request);
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
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListMinaSshdServicesMethod(), getCallOptions()), request);
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
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGenerateAndRegisterSshTunnelingKeyPairMethod(), getCallOptions()), request);
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
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterSshTunnelingKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unregister Ssh Tunneling Key
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse> unregisterSshTunnelingKey(
        com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnregisterSshTunnelingKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * For minaSshdService
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse> listSshTunnelingKeys(
        com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListSshTunnelingKeysMethod(), getCallOptions()), request);
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
          getAcquireMinaSshdServiceMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest,
              com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse>(
                service, METHODID_ACQUIRE_MINA_SSHD_SERVICE)))
        .addMethod(
          getListMinaSshdServicesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest,
              com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse>(
                service, METHODID_LIST_MINA_SSHD_SERVICES)))
        .addMethod(
          getGenerateAndRegisterSshTunnelingKeyPairMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest,
              com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse>(
                service, METHODID_GENERATE_AND_REGISTER_SSH_TUNNELING_KEY_PAIR)))
        .addMethod(
          getRegisterSshTunnelingKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyRequest,
              com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.RegisterSshTunnelingKeyResponse>(
                service, METHODID_REGISTER_SSH_TUNNELING_KEY)))
        .addMethod(
          getUnregisterSshTunnelingKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest,
              com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse>(
                service, METHODID_UNREGISTER_SSH_TUNNELING_KEY)))
        .addMethod(
          getListSshTunnelingKeysMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysRequest,
              com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListSshTunnelingKeysResponse>(
                service, METHODID_LIST_SSH_TUNNELING_KEYS)))
        .build();
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
    private final java.lang.String methodName;

    MinaSshdManagementMethodDescriptorSupplier(java.lang.String methodName) {
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
              .addMethod(getGetVersionMethod())
              .addMethod(getAcquireMinaSshdServiceMethod())
              .addMethod(getListMinaSshdServicesMethod())
              .addMethod(getGenerateAndRegisterSshTunnelingKeyPairMethod())
              .addMethod(getRegisterSshTunnelingKeyMethod())
              .addMethod(getUnregisterSshTunnelingKeyMethod())
              .addMethod(getListSshTunnelingKeysMethod())
              .build();
        }
      }
    }
    return result;
  }
}
