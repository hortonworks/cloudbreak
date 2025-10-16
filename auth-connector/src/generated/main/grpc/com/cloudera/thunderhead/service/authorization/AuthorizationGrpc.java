package com.cloudera.thunderhead.service.authorization;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class AuthorizationGrpc {

  private AuthorizationGrpc() {}

  public static final java.lang.String SERVICE_NAME = "authorization.Authorization";

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
    if ((getGetVersionMethod = AuthorizationGrpc.getGetVersionMethod) == null) {
      synchronized (AuthorizationGrpc.class) {
        if ((getGetVersionMethod = AuthorizationGrpc.getGetVersionMethod) == null) {
          AuthorizationGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuthorizationMethodDescriptorSupplier("GetVersion"))
              .build();
        }
      }
    }
    return getGetVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest,
      com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> getCheckRightMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CheckRight",
      requestType = com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest.class,
      responseType = com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest,
      com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> getCheckRightMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest, com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> getCheckRightMethod;
    if ((getCheckRightMethod = AuthorizationGrpc.getCheckRightMethod) == null) {
      synchronized (AuthorizationGrpc.class) {
        if ((getCheckRightMethod = AuthorizationGrpc.getCheckRightMethod) == null) {
          AuthorizationGrpc.getCheckRightMethod = getCheckRightMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest, com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CheckRight"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuthorizationMethodDescriptorSupplier("CheckRight"))
              .build();
        }
      }
    }
    return getCheckRightMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest,
      com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse> getHasRightsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HasRights",
      requestType = com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest.class,
      responseType = com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest,
      com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse> getHasRightsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest, com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse> getHasRightsMethod;
    if ((getHasRightsMethod = AuthorizationGrpc.getHasRightsMethod) == null) {
      synchronized (AuthorizationGrpc.class) {
        if ((getHasRightsMethod = AuthorizationGrpc.getHasRightsMethod) == null) {
          AuthorizationGrpc.getHasRightsMethod = getHasRightsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest, com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "HasRights"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuthorizationMethodDescriptorSupplier("HasRights"))
              .build();
        }
      }
    }
    return getHasRightsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AuthorizationStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthorizationStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthorizationStub>() {
        @java.lang.Override
        public AuthorizationStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthorizationStub(channel, callOptions);
        }
      };
    return AuthorizationStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static AuthorizationBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthorizationBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthorizationBlockingV2Stub>() {
        @java.lang.Override
        public AuthorizationBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthorizationBlockingV2Stub(channel, callOptions);
        }
      };
    return AuthorizationBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AuthorizationBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthorizationBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthorizationBlockingStub>() {
        @java.lang.Override
        public AuthorizationBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthorizationBlockingStub(channel, callOptions);
        }
      };
    return AuthorizationBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AuthorizationFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthorizationFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthorizationFutureStub>() {
        @java.lang.Override
        public AuthorizationFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthorizationFutureStub(channel, callOptions);
        }
      };
    return AuthorizationFutureStub.newStub(factory, channel);
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
     * Does a rights check for the input actor. Fails with a PERMISSION_DENIED
     * error with a user-appropriate message if the check fails.
     * </pre>
     */
    default void checkRight(com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckRightMethod(), responseObserver);
    }

    /**
     * <pre>
     * Performs a number of rights checks returning results rather than throwing.
     * This call should be used by clients to perform multiple rights checks as
     * part of a single operation. It is the caller's responsibility to craft an
     * appropriate error if the results indicate authorization failure. See
     * Scrutinizer.java for examples of user-appropriate errors.
     * </pre>
     */
    default void hasRights(com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHasRightsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service Authorization.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class AuthorizationImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AuthorizationGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service Authorization.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuthorizationStub
      extends io.grpc.stub.AbstractAsyncStub<AuthorizationStub> {
    private AuthorizationStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthorizationStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthorizationStub(channel, callOptions);
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
     * Does a rights check for the input actor. Fails with a PERMISSION_DENIED
     * error with a user-appropriate message if the check fails.
     * </pre>
     */
    public void checkRight(com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCheckRightMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Performs a number of rights checks returning results rather than throwing.
     * This call should be used by clients to perform multiple rights checks as
     * part of a single operation. It is the caller's responsibility to craft an
     * appropriate error if the results indicate authorization failure. See
     * Scrutinizer.java for examples of user-appropriate errors.
     * </pre>
     */
    public void hasRights(com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHasRightsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service Authorization.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuthorizationBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<AuthorizationBlockingV2Stub> {
    private AuthorizationBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthorizationBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthorizationBlockingV2Stub(channel, callOptions);
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
     * Does a rights check for the input actor. Fails with a PERMISSION_DENIED
     * error with a user-appropriate message if the check fails.
     * </pre>
     */
    public com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse checkRight(com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCheckRightMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Performs a number of rights checks returning results rather than throwing.
     * This call should be used by clients to perform multiple rights checks as
     * part of a single operation. It is the caller's responsibility to craft an
     * appropriate error if the results indicate authorization failure. See
     * Scrutinizer.java for examples of user-appropriate errors.
     * </pre>
     */
    public com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse hasRights(com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getHasRightsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service Authorization.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuthorizationBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AuthorizationBlockingStub> {
    private AuthorizationBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthorizationBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthorizationBlockingStub(channel, callOptions);
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
     * Does a rights check for the input actor. Fails with a PERMISSION_DENIED
     * error with a user-appropriate message if the check fails.
     * </pre>
     */
    public com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse checkRight(com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCheckRightMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Performs a number of rights checks returning results rather than throwing.
     * This call should be used by clients to perform multiple rights checks as
     * part of a single operation. It is the caller's responsibility to craft an
     * appropriate error if the results indicate authorization failure. See
     * Scrutinizer.java for examples of user-appropriate errors.
     * </pre>
     */
    public com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse hasRights(com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHasRightsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service Authorization.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuthorizationFutureStub
      extends io.grpc.stub.AbstractFutureStub<AuthorizationFutureStub> {
    private AuthorizationFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthorizationFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthorizationFutureStub(channel, callOptions);
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
     * Does a rights check for the input actor. Fails with a PERMISSION_DENIED
     * error with a user-appropriate message if the check fails.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> checkRight(
        com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCheckRightMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Performs a number of rights checks returning results rather than throwing.
     * This call should be used by clients to perform multiple rights checks as
     * part of a single operation. It is the caller's responsibility to craft an
     * appropriate error if the results indicate authorization failure. See
     * Scrutinizer.java for examples of user-appropriate errors.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse> hasRights(
        com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHasRightsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_CHECK_RIGHT = 1;
  private static final int METHODID_HAS_RIGHTS = 2;

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
        case METHODID_CHECK_RIGHT:
          serviceImpl.checkRight((com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse>) responseObserver);
          break;
        case METHODID_HAS_RIGHTS:
          serviceImpl.hasRights((com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse>) responseObserver);
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
          getCheckRightMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest,
              com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse>(
                service, METHODID_CHECK_RIGHT)))
        .addMethod(
          getHasRightsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest,
              com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse>(
                service, METHODID_HAS_RIGHTS)))
        .build();
  }

  private static abstract class AuthorizationBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AuthorizationBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.authorization.AuthorizationProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Authorization");
    }
  }

  private static final class AuthorizationFileDescriptorSupplier
      extends AuthorizationBaseDescriptorSupplier {
    AuthorizationFileDescriptorSupplier() {}
  }

  private static final class AuthorizationMethodDescriptorSupplier
      extends AuthorizationBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    AuthorizationMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (AuthorizationGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AuthorizationFileDescriptorSupplier())
              .addMethod(getGetVersionMethod())
              .addMethod(getCheckRightMethod())
              .addMethod(getHasRightsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
