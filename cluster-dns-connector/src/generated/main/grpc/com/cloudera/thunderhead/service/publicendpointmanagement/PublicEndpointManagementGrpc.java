package com.cloudera.thunderhead.service.publicendpointmanagement;

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
    comments = "Source: publicendpointmanagement.proto")
public final class PublicEndpointManagementGrpc {

  private PublicEndpointManagementGrpc() {}

  public static final String SERVICE_NAME = "publicendpointmanagement.PublicEndpointManagement";

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
    if ((getGetVersionMethod = PublicEndpointManagementGrpc.getGetVersionMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getGetVersionMethod = PublicEndpointManagementGrpc.getGetVersionMethod) == null) {
          PublicEndpointManagementGrpc.getGetVersionMethod = getGetVersionMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "publicendpointmanagement.PublicEndpointManagement", "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PublicEndpointManagementMethodDescriptorSupplier("GetVersion"))
                  .build();
          }
        }
     }
     return getGetVersionMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateDnsEntryMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> METHOD_CREATE_DNS_ENTRY = getCreateDnsEntryMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> getCreateDnsEntryMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> getCreateDnsEntryMethod() {
    return getCreateDnsEntryMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> getCreateDnsEntryMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> getCreateDnsEntryMethod;
    if ((getCreateDnsEntryMethod = PublicEndpointManagementGrpc.getCreateDnsEntryMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getCreateDnsEntryMethod = PublicEndpointManagementGrpc.getCreateDnsEntryMethod) == null) {
          PublicEndpointManagementGrpc.getCreateDnsEntryMethod = getCreateDnsEntryMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "publicendpointmanagement.PublicEndpointManagement", "CreateDnsEntry"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PublicEndpointManagementMethodDescriptorSupplier("CreateDnsEntry"))
                  .build();
          }
        }
     }
     return getCreateDnsEntryMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteDnsEntryMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> METHOD_DELETE_DNS_ENTRY = getDeleteDnsEntryMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> getDeleteDnsEntryMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> getDeleteDnsEntryMethod() {
    return getDeleteDnsEntryMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> getDeleteDnsEntryMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> getDeleteDnsEntryMethod;
    if ((getDeleteDnsEntryMethod = PublicEndpointManagementGrpc.getDeleteDnsEntryMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getDeleteDnsEntryMethod = PublicEndpointManagementGrpc.getDeleteDnsEntryMethod) == null) {
          PublicEndpointManagementGrpc.getDeleteDnsEntryMethod = getDeleteDnsEntryMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "publicendpointmanagement.PublicEndpointManagement", "DeleteDnsEntry"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PublicEndpointManagementMethodDescriptorSupplier("DeleteDnsEntry"))
                  .build();
          }
        }
     }
     return getDeleteDnsEntryMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateCertificateMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> METHOD_CREATE_CERTIFICATE = getCreateCertificateMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> getCreateCertificateMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> getCreateCertificateMethod() {
    return getCreateCertificateMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> getCreateCertificateMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> getCreateCertificateMethod;
    if ((getCreateCertificateMethod = PublicEndpointManagementGrpc.getCreateCertificateMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getCreateCertificateMethod = PublicEndpointManagementGrpc.getCreateCertificateMethod) == null) {
          PublicEndpointManagementGrpc.getCreateCertificateMethod = getCreateCertificateMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "publicendpointmanagement.PublicEndpointManagement", "CreateCertificate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PublicEndpointManagementMethodDescriptorSupplier("CreateCertificate"))
                  .build();
          }
        }
     }
     return getCreateCertificateMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getPollCertificateCreationMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> METHOD_POLL_CERTIFICATE_CREATION = getPollCertificateCreationMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> getPollCertificateCreationMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> getPollCertificateCreationMethod() {
    return getPollCertificateCreationMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> getPollCertificateCreationMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> getPollCertificateCreationMethod;
    if ((getPollCertificateCreationMethod = PublicEndpointManagementGrpc.getPollCertificateCreationMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getPollCertificateCreationMethod = PublicEndpointManagementGrpc.getPollCertificateCreationMethod) == null) {
          PublicEndpointManagementGrpc.getPollCertificateCreationMethod = getPollCertificateCreationMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "publicendpointmanagement.PublicEndpointManagement", "PollCertificateCreation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PublicEndpointManagementMethodDescriptorSupplier("PollCertificateCreation"))
                  .build();
          }
        }
     }
     return getPollCertificateCreationMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PublicEndpointManagementStub newStub(io.grpc.Channel channel) {
    return new PublicEndpointManagementStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PublicEndpointManagementBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new PublicEndpointManagementBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PublicEndpointManagementFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new PublicEndpointManagementFutureStub(channel);
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class PublicEndpointManagementImplBase implements io.grpc.BindableService {

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
     * Create or update a DNS entry. :: Create or update a DNS entry.
     * </pre>
     */
    public void createDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateDnsEntryMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Delete a DNS entry. :: Delete a DNS entry.
     * </pre>
     */
    public void deleteDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteDnsEntryMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Get a TLS certificate. :: Get a TLS certificate
     * </pre>
     */
    public void createCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateCertificateMethodHelper(), responseObserver);
    }

    /**
     */
    public void pollCertificateCreation(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPollCertificateCreationMethodHelper(), responseObserver);
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
            getCreateDnsEntryMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest,
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse>(
                  this, METHODID_CREATE_DNS_ENTRY)))
          .addMethod(
            getDeleteDnsEntryMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest,
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse>(
                  this, METHODID_DELETE_DNS_ENTRY)))
          .addMethod(
            getCreateCertificateMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest,
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse>(
                  this, METHODID_CREATE_CERTIFICATE)))
          .addMethod(
            getPollCertificateCreationMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest,
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse>(
                  this, METHODID_POLL_CERTIFICATE_CREATION)))
          .build();
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PublicEndpointManagementStub extends io.grpc.stub.AbstractStub<PublicEndpointManagementStub> {
    private PublicEndpointManagementStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PublicEndpointManagementStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PublicEndpointManagementStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PublicEndpointManagementStub(channel, callOptions);
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
     * Create or update a DNS entry. :: Create or update a DNS entry.
     * </pre>
     */
    public void createDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateDnsEntryMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete a DNS entry. :: Delete a DNS entry.
     * </pre>
     */
    public void deleteDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteDnsEntryMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get a TLS certificate. :: Get a TLS certificate
     * </pre>
     */
    public void createCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateCertificateMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void pollCertificateCreation(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPollCertificateCreationMethodHelper(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PublicEndpointManagementBlockingStub extends io.grpc.stub.AbstractStub<PublicEndpointManagementBlockingStub> {
    private PublicEndpointManagementBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PublicEndpointManagementBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PublicEndpointManagementBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PublicEndpointManagementBlockingStub(channel, callOptions);
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
     * Create or update a DNS entry. :: Create or update a DNS entry.
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse createDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateDnsEntryMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a DNS entry. :: Delete a DNS entry.
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse deleteDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteDnsEntryMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a TLS certificate. :: Get a TLS certificate
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse createCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateCertificateMethodHelper(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse pollCertificateCreation(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest request) {
      return blockingUnaryCall(
          getChannel(), getPollCertificateCreationMethodHelper(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PublicEndpointManagementFutureStub extends io.grpc.stub.AbstractStub<PublicEndpointManagementFutureStub> {
    private PublicEndpointManagementFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PublicEndpointManagementFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PublicEndpointManagementFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PublicEndpointManagementFutureStub(channel, callOptions);
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
     * Create or update a DNS entry. :: Create or update a DNS entry.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> createDnsEntry(
        com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateDnsEntryMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete a DNS entry. :: Delete a DNS entry.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> deleteDnsEntry(
        com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteDnsEntryMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get a TLS certificate. :: Get a TLS certificate
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> createCertificate(
        com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateCertificateMethodHelper(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> pollCertificateCreation(
        com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPollCertificateCreationMethodHelper(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_CREATE_DNS_ENTRY = 1;
  private static final int METHODID_DELETE_DNS_ENTRY = 2;
  private static final int METHODID_CREATE_CERTIFICATE = 3;
  private static final int METHODID_POLL_CERTIFICATE_CREATION = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PublicEndpointManagementImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PublicEndpointManagementImplBase serviceImpl, int methodId) {
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
        case METHODID_CREATE_DNS_ENTRY:
          serviceImpl.createDnsEntry((com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse>) responseObserver);
          break;
        case METHODID_DELETE_DNS_ENTRY:
          serviceImpl.deleteDnsEntry((com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse>) responseObserver);
          break;
        case METHODID_CREATE_CERTIFICATE:
          serviceImpl.createCertificate((com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse>) responseObserver);
          break;
        case METHODID_POLL_CERTIFICATE_CREATION:
          serviceImpl.pollCertificateCreation((com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse>) responseObserver);
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

  private static abstract class PublicEndpointManagementBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PublicEndpointManagementBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PublicEndpointManagement");
    }
  }

  private static final class PublicEndpointManagementFileDescriptorSupplier
      extends PublicEndpointManagementBaseDescriptorSupplier {
    PublicEndpointManagementFileDescriptorSupplier() {}
  }

  private static final class PublicEndpointManagementMethodDescriptorSupplier
      extends PublicEndpointManagementBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PublicEndpointManagementMethodDescriptorSupplier(String methodName) {
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
      synchronized (PublicEndpointManagementGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PublicEndpointManagementFileDescriptorSupplier())
              .addMethod(getGetVersionMethodHelper())
              .addMethod(getCreateDnsEntryMethodHelper())
              .addMethod(getDeleteDnsEntryMethodHelper())
              .addMethod(getCreateCertificateMethodHelper())
              .addMethod(getPollCertificateCreationMethodHelper())
              .build();
        }
      }
    }
    return result;
  }
}
