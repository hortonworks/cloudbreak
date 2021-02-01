package com.cloudera.thunderhead.service.personalresourceview;

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
    comments = "Source: personalresourceview.proto")
public final class PersonalResourceViewGrpc {

  private PersonalResourceViewGrpc() {}

  public static final String SERVICE_NAME = "authorization.PersonalResourceView";

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
    if ((getGetVersionMethod = PersonalResourceViewGrpc.getGetVersionMethod) == null) {
      synchronized (PersonalResourceViewGrpc.class) {
        if ((getGetVersionMethod = PersonalResourceViewGrpc.getGetVersionMethod) == null) {
          PersonalResourceViewGrpc.getGetVersionMethod = getGetVersionMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "authorization.PersonalResourceView", "GetVersion"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getHasResourcesByRightMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest,
      com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> METHOD_HAS_RESOURCES_BY_RIGHT = getHasResourcesByRightMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest,
      com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> getHasResourcesByRightMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest,
      com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> getHasResourcesByRightMethod() {
    return getHasResourcesByRightMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest,
      com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> getHasResourcesByRightMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest, com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> getHasResourcesByRightMethod;
    if ((getHasResourcesByRightMethod = PersonalResourceViewGrpc.getHasResourcesByRightMethod) == null) {
      synchronized (PersonalResourceViewGrpc.class) {
        if ((getHasResourcesByRightMethod = PersonalResourceViewGrpc.getHasResourcesByRightMethod) == null) {
          PersonalResourceViewGrpc.getHasResourcesByRightMethod = getHasResourcesByRightMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest, com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "authorization.PersonalResourceView", "hasResourcesByRight"))
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
    return new PersonalResourceViewStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PersonalResourceViewBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new PersonalResourceViewBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PersonalResourceViewFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new PersonalResourceViewFutureStub(channel);
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class PersonalResourceViewImplBase implements io.grpc.BindableService {

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
     * Checks whether or not the requested resources are part of the the
     * personal view requested.
     * </pre>
     */
    public void hasResourcesByRight(com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getHasResourcesByRightMethodHelper(), responseObserver);
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
            getHasResourcesByRightMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest,
                com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse>(
                  this, METHODID_HAS_RESOURCES_BY_RIGHT)))
          .build();
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PersonalResourceViewStub extends io.grpc.stub.AbstractStub<PersonalResourceViewStub> {
    private PersonalResourceViewStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PersonalResourceViewStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersonalResourceViewStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PersonalResourceViewStub(channel, callOptions);
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
     * Checks whether or not the requested resources are part of the the
     * personal view requested.
     * </pre>
     */
    public void hasResourcesByRight(com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getHasResourcesByRightMethodHelper(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PersonalResourceViewBlockingStub extends io.grpc.stub.AbstractStub<PersonalResourceViewBlockingStub> {
    private PersonalResourceViewBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PersonalResourceViewBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersonalResourceViewBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PersonalResourceViewBlockingStub(channel, callOptions);
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
     * Checks whether or not the requested resources are part of the the
     * personal view requested.
     * </pre>
     */
    public com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse hasResourcesByRight(com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest request) {
      return blockingUnaryCall(
          getChannel(), getHasResourcesByRightMethodHelper(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PersonalResourceViewFutureStub extends io.grpc.stub.AbstractStub<PersonalResourceViewFutureStub> {
    private PersonalResourceViewFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PersonalResourceViewFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersonalResourceViewFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PersonalResourceViewFutureStub(channel, callOptions);
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
     * Checks whether or not the requested resources are part of the the
     * personal view requested.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightResponse> hasResourcesByRight(
        com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto.HasResourcesByRightRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getHasResourcesByRightMethodHelper(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_HAS_RESOURCES_BY_RIGHT = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PersonalResourceViewImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PersonalResourceViewImplBase serviceImpl, int methodId) {
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
    private final String methodName;

    PersonalResourceViewMethodDescriptorSupplier(String methodName) {
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
              .addMethod(getGetVersionMethodHelper())
              .addMethod(getHasResourcesByRightMethodHelper())
              .build();
        }
      }
    }
    return result;
  }
}
