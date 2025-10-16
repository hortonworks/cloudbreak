package com.cloudera.thunderhead.service.personalresourceview;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class PersonalResourceViewGrpc {

  private PersonalResourceViewGrpc() {}

  public static final java.lang.String SERVICE_NAME = "authorization.PersonalResourceView";

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
    if ((getGetVersionMethod = PersonalResourceViewGrpc.getGetVersionMethod) == null) {
      synchronized (PersonalResourceViewGrpc.class) {
        if ((getGetVersionMethod = PersonalResourceViewGrpc.getGetVersionMethod) == null) {
          PersonalResourceViewGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PersonalResourceViewMethodDescriptorSupplier("GetVersion"))
              .build();
        }
      }
    }
    return getGetVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest,
      com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> getHasResourcesByRightMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "hasResourcesByRight",
      requestType = com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest.class,
      responseType = com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest,
      com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> getHasResourcesByRightMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest, com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> getHasResourcesByRightMethod;
    if ((getHasResourcesByRightMethod = PersonalResourceViewGrpc.getHasResourcesByRightMethod) == null) {
      synchronized (PersonalResourceViewGrpc.class) {
        if ((getHasResourcesByRightMethod = PersonalResourceViewGrpc.getHasResourcesByRightMethod) == null) {
          PersonalResourceViewGrpc.getHasResourcesByRightMethod = getHasResourcesByRightMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest, com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "hasResourcesByRight"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PersonalResourceViewMethodDescriptorSupplier("hasResourcesByRight"))
              .build();
        }
      }
    }
    return getHasResourcesByRightMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PersonalResourceViewStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PersonalResourceViewStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PersonalResourceViewStub>() {
        @java.lang.Override
        public PersonalResourceViewStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PersonalResourceViewStub(channel, callOptions);
        }
      };
    return PersonalResourceViewStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static PersonalResourceViewBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PersonalResourceViewBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PersonalResourceViewBlockingV2Stub>() {
        @java.lang.Override
        public PersonalResourceViewBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PersonalResourceViewBlockingV2Stub(channel, callOptions);
        }
      };
    return PersonalResourceViewBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PersonalResourceViewBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PersonalResourceViewBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PersonalResourceViewBlockingStub>() {
        @java.lang.Override
        public PersonalResourceViewBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PersonalResourceViewBlockingStub(channel, callOptions);
        }
      };
    return PersonalResourceViewBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PersonalResourceViewFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PersonalResourceViewFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PersonalResourceViewFutureStub>() {
        @java.lang.Override
        public PersonalResourceViewFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PersonalResourceViewFutureStub(channel, callOptions);
        }
      };
    return PersonalResourceViewFutureStub.newStub(factory, channel);
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
     * Checks whether or not the requested resources are part of the the
     * personal view requested.
     * </pre>
     */
    default void hasResourcesByRight(com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHasResourcesByRightMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service PersonalResourceView.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class PersonalResourceViewImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return PersonalResourceViewGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service PersonalResourceView.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PersonalResourceViewStub
      extends io.grpc.stub.AbstractAsyncStub<PersonalResourceViewStub> {
    private PersonalResourceViewStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersonalResourceViewStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PersonalResourceViewStub(channel, callOptions);
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
     * Checks whether or not the requested resources are part of the the
     * personal view requested.
     * </pre>
     */
    public void hasResourcesByRight(com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHasResourcesByRightMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service PersonalResourceView.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PersonalResourceViewBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<PersonalResourceViewBlockingV2Stub> {
    private PersonalResourceViewBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersonalResourceViewBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PersonalResourceViewBlockingV2Stub(channel, callOptions);
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
     * Checks whether or not the requested resources are part of the the
     * personal view requested.
     * </pre>
     */
    public com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse hasResourcesByRight(com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getHasResourcesByRightMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service PersonalResourceView.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PersonalResourceViewBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<PersonalResourceViewBlockingStub> {
    private PersonalResourceViewBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersonalResourceViewBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PersonalResourceViewBlockingStub(channel, callOptions);
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
     * Checks whether or not the requested resources are part of the the
     * personal view requested.
     * </pre>
     */
    public com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse hasResourcesByRight(com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHasResourcesByRightMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service PersonalResourceView.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PersonalResourceViewFutureStub
      extends io.grpc.stub.AbstractFutureStub<PersonalResourceViewFutureStub> {
    private PersonalResourceViewFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersonalResourceViewFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PersonalResourceViewFutureStub(channel, callOptions);
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
     * Checks whether or not the requested resources are part of the the
     * personal view requested.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> hasResourcesByRight(
        com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHasResourcesByRightMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_HAS_RESOURCES_BY_RIGHT = 1;

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
        case METHODID_HAS_RESOURCES_BY_RIGHT:
          serviceImpl.hasResourcesByRight((com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse>) responseObserver);
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
          getHasResourcesByRightMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest,
              com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse>(
                service, METHODID_HAS_RESOURCES_BY_RIGHT)))
        .build();
  }

  private static abstract class PersonalResourceViewBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PersonalResourceViewBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PersonalResourceView");
    }
  }

  private static final class PersonalResourceViewFileDescriptorSupplier
      extends PersonalResourceViewBaseDescriptorSupplier {
    PersonalResourceViewFileDescriptorSupplier() {}
  }

  private static final class PersonalResourceViewMethodDescriptorSupplier
      extends PersonalResourceViewBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    PersonalResourceViewMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (PersonalResourceViewGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PersonalResourceViewFileDescriptorSupplier())
              .addMethod(getGetVersionMethod())
              .addMethod(getHasResourcesByRightMethod())
              .build();
        }
      }
    }
    return result;
  }
}
