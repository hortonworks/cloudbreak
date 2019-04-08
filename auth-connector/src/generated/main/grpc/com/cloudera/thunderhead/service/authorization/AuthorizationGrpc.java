package com.cloudera.thunderhead.service.authorization;

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
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.12.0)",
    comments = "Source: authorization.proto")
public final class AuthorizationGrpc {

  private AuthorizationGrpc() {}

  public static final String SERVICE_NAME = "authorization.Authorization";

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
    if ((getGetVersionMethod = AuthorizationGrpc.getGetVersionMethod) == null) {
      synchronized (AuthorizationGrpc.class) {
        if ((getGetVersionMethod = AuthorizationGrpc.getGetVersionMethod) == null) {
          AuthorizationGrpc.getGetVersionMethod = getGetVersionMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "authorization.Authorization", "GetVersion"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCheckRightMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest,
      com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> METHOD_CHECK_RIGHT = getCheckRightMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest,
      com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> getCheckRightMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest,
      com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> getCheckRightMethod() {
    return getCheckRightMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest,
      com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> getCheckRightMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest, com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> getCheckRightMethod;
    if ((getCheckRightMethod = AuthorizationGrpc.getCheckRightMethod) == null) {
      synchronized (AuthorizationGrpc.class) {
        if ((getCheckRightMethod = AuthorizationGrpc.getCheckRightMethod) == null) {
          AuthorizationGrpc.getCheckRightMethod = getCheckRightMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest, com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "authorization.Authorization", "CheckRight"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getHasRightsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest,
      com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse> METHOD_HAS_RIGHTS = getHasRightsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest,
      com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse> getHasRightsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest,
      com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse> getHasRightsMethod() {
    return getHasRightsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest,
      com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse> getHasRightsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest, com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse> getHasRightsMethod;
    if ((getHasRightsMethod = AuthorizationGrpc.getHasRightsMethod) == null) {
      synchronized (AuthorizationGrpc.class) {
        if ((getHasRightsMethod = AuthorizationGrpc.getHasRightsMethod) == null) {
          AuthorizationGrpc.getHasRightsMethod = getHasRightsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest, com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "authorization.Authorization", "HasRights"))
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
    return new AuthorizationStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AuthorizationBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new AuthorizationBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AuthorizationFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new AuthorizationFutureStub(channel);
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class AuthorizationImplBase implements io.grpc.BindableService {

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
     * Does a rights check for the input actor. Fails with a PERMISSION_DENIED
     * error with a user-appropriate message if the check fails.
     * </pre>
     */
    public void checkRight(com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCheckRightMethodHelper(), responseObserver);
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
      asyncUnimplementedUnaryCall(getHasRightsMethodHelper(), responseObserver);
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
            getCheckRightMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest,
                com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse>(
                  this, METHODID_CHECK_RIGHT)))
          .addMethod(
            getHasRightsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest,
                com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse>(
                  this, METHODID_HAS_RIGHTS)))
          .build();
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuthorizationStub extends io.grpc.stub.AbstractStub<AuthorizationStub> {
    private AuthorizationStub(io.grpc.Channel channel) {
      super(channel);
    }

    private AuthorizationStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthorizationStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new AuthorizationStub(channel, callOptions);
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
     * Does a rights check for the input actor. Fails with a PERMISSION_DENIED
     * error with a user-appropriate message if the check fails.
     * </pre>
     */
    public void checkRight(com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCheckRightMethodHelper(), getCallOptions()), request, responseObserver);
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
      asyncUnaryCall(
          getChannel().newCall(getHasRightsMethodHelper(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuthorizationBlockingStub extends io.grpc.stub.AbstractStub<AuthorizationBlockingStub> {
    private AuthorizationBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private AuthorizationBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthorizationBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new AuthorizationBlockingStub(channel, callOptions);
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
     * Does a rights check for the input actor. Fails with a PERMISSION_DENIED
     * error with a user-appropriate message if the check fails.
     * </pre>
     */
    public com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse checkRight(com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest request) {
      return blockingUnaryCall(
          getChannel(), getCheckRightMethodHelper(), getCallOptions(), request);
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
      return blockingUnaryCall(
          getChannel(), getHasRightsMethodHelper(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class AuthorizationFutureStub extends io.grpc.stub.AbstractStub<AuthorizationFutureStub> {
    private AuthorizationFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private AuthorizationFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthorizationFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new AuthorizationFutureStub(channel, callOptions);
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
     * Does a rights check for the input actor. Fails with a PERMISSION_DENIED
     * error with a user-appropriate message if the check fails.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse> checkRight(
        com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCheckRightMethodHelper(), getCallOptions()), request);
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
      return futureUnaryCall(
          getChannel().newCall(getHasRightsMethodHelper(), getCallOptions()), request);
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
    private final AuthorizationImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(AuthorizationImplBase serviceImpl, int methodId) {
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
    private final String methodName;

    AuthorizationMethodDescriptorSupplier(String methodName) {
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
              .addMethod(getGetVersionMethodHelper())
              .addMethod(getCheckRightMethodHelper())
              .addMethod(getHasRightsMethodHelper())
              .build();
        }
      }
    }
    return result;
  }
}
