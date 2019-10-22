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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGenerateManagedDomainNamesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse> METHOD_GENERATE_MANAGED_DOMAIN_NAMES = getGenerateManagedDomainNamesMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse> getGenerateManagedDomainNamesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse> getGenerateManagedDomainNamesMethod() {
    return getGenerateManagedDomainNamesMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse> getGenerateManagedDomainNamesMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse> getGenerateManagedDomainNamesMethod;
    if ((getGenerateManagedDomainNamesMethod = PublicEndpointManagementGrpc.getGenerateManagedDomainNamesMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getGenerateManagedDomainNamesMethod = PublicEndpointManagementGrpc.getGenerateManagedDomainNamesMethod) == null) {
          PublicEndpointManagementGrpc.getGenerateManagedDomainNamesMethod = getGenerateManagedDomainNamesMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "publicendpointmanagement.PublicEndpointManagement", "GenerateManagedDomainNames"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PublicEndpointManagementMethodDescriptorSupplier("GenerateManagedDomainNames"))
                  .build();
          }
        }
     }
     return getGenerateManagedDomainNamesMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getSignCertificateMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse> METHOD_SIGN_CERTIFICATE = getSignCertificateMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse> getSignCertificateMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse> getSignCertificateMethod() {
    return getSignCertificateMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse> getSignCertificateMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse> getSignCertificateMethod;
    if ((getSignCertificateMethod = PublicEndpointManagementGrpc.getSignCertificateMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getSignCertificateMethod = PublicEndpointManagementGrpc.getSignCertificateMethod) == null) {
          PublicEndpointManagementGrpc.getSignCertificateMethod = getSignCertificateMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "publicendpointmanagement.PublicEndpointManagement", "SignCertificate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PublicEndpointManagementMethodDescriptorSupplier("SignCertificate"))
                  .build();
          }
        }
     }
     return getSignCertificateMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getPollCertificateSigningMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse> METHOD_POLL_CERTIFICATE_SIGNING = getPollCertificateSigningMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse> getPollCertificateSigningMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse> getPollCertificateSigningMethod() {
    return getPollCertificateSigningMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse> getPollCertificateSigningMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse> getPollCertificateSigningMethod;
    if ((getPollCertificateSigningMethod = PublicEndpointManagementGrpc.getPollCertificateSigningMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getPollCertificateSigningMethod = PublicEndpointManagementGrpc.getPollCertificateSigningMethod) == null) {
          PublicEndpointManagementGrpc.getPollCertificateSigningMethod = getPollCertificateSigningMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "publicendpointmanagement.PublicEndpointManagement", "PollCertificateSigning"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PublicEndpointManagementMethodDescriptorSupplier("PollCertificateSigning"))
                  .build();
          }
        }
     }
     return getPollCertificateSigningMethod;
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

    /**
     * <pre>
     * Takes in a list subdomain patterns as a list with accountId and name of the environment
     * and returns a map of subdomain pattern and generate domain name/pattern
     * For example, if workloadSubdomain of provided accountId is `workload-subdomain`,
     * name of the environment is `env-name` and the supplied array of subdomain patterns
     * is [ "host-a", "*.mlx", "subdomain.mlx" ], the method would return the result as following:
     * {
     *    "host-a": "host-a.env-name.workload-subdomain.cloudera.site",
     *    "*.mlx": "*.mlx.env-name.workload-subdomain.cloudera.site",
     *    "subdomain.mlx": "subdomain.mlx.env-name.workload-subdomain.cloudera.site"
     *  }
     * </pre>
     */
    public void generateManagedDomainNames(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGenerateManagedDomainNamesMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Takes in byte array representing a DER/binary formatted CSR with accountId and name of the environment,
     * validates the domains in CSR and triggers a workflow to submit it to LetsEncrypt and returns a workflowId
     * using which a client can poll the workflow's progress and finally get the signed certificate
     * </pre>
     */
    public void signCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSignCertificateMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Takes in a workflowId representing a workflow in system which tracks the signing of a CSR
     * If successfully complete, it returns a list of certificates in the trust chain with status as SigningStatus.SUCCESS
     * In case of error, it returns the status as SigningStatus.FAILED
     * If the workflow is not yet complete, it returns the status as SigningStatus.IN_PROGRESS
     * </pre>
     */
    public void pollCertificateSigning(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPollCertificateSigningMethodHelper(), responseObserver);
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
          .addMethod(
            getGenerateManagedDomainNamesMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest,
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse>(
                  this, METHODID_GENERATE_MANAGED_DOMAIN_NAMES)))
          .addMethod(
            getSignCertificateMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest,
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse>(
                  this, METHODID_SIGN_CERTIFICATE)))
          .addMethod(
            getPollCertificateSigningMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest,
                com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse>(
                  this, METHODID_POLL_CERTIFICATE_SIGNING)))
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

    /**
     * <pre>
     * Takes in a list subdomain patterns as a list with accountId and name of the environment
     * and returns a map of subdomain pattern and generate domain name/pattern
     * For example, if workloadSubdomain of provided accountId is `workload-subdomain`,
     * name of the environment is `env-name` and the supplied array of subdomain patterns
     * is [ "host-a", "*.mlx", "subdomain.mlx" ], the method would return the result as following:
     * {
     *    "host-a": "host-a.env-name.workload-subdomain.cloudera.site",
     *    "*.mlx": "*.mlx.env-name.workload-subdomain.cloudera.site",
     *    "subdomain.mlx": "subdomain.mlx.env-name.workload-subdomain.cloudera.site"
     *  }
     * </pre>
     */
    public void generateManagedDomainNames(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGenerateManagedDomainNamesMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Takes in byte array representing a DER/binary formatted CSR with accountId and name of the environment,
     * validates the domains in CSR and triggers a workflow to submit it to LetsEncrypt and returns a workflowId
     * using which a client can poll the workflow's progress and finally get the signed certificate
     * </pre>
     */
    public void signCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSignCertificateMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Takes in a workflowId representing a workflow in system which tracks the signing of a CSR
     * If successfully complete, it returns a list of certificates in the trust chain with status as SigningStatus.SUCCESS
     * In case of error, it returns the status as SigningStatus.FAILED
     * If the workflow is not yet complete, it returns the status as SigningStatus.IN_PROGRESS
     * </pre>
     */
    public void pollCertificateSigning(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPollCertificateSigningMethodHelper(), getCallOptions()), request, responseObserver);
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

    /**
     * <pre>
     * Takes in a list subdomain patterns as a list with accountId and name of the environment
     * and returns a map of subdomain pattern and generate domain name/pattern
     * For example, if workloadSubdomain of provided accountId is `workload-subdomain`,
     * name of the environment is `env-name` and the supplied array of subdomain patterns
     * is [ "host-a", "*.mlx", "subdomain.mlx" ], the method would return the result as following:
     * {
     *    "host-a": "host-a.env-name.workload-subdomain.cloudera.site",
     *    "*.mlx": "*.mlx.env-name.workload-subdomain.cloudera.site",
     *    "subdomain.mlx": "subdomain.mlx.env-name.workload-subdomain.cloudera.site"
     *  }
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse generateManagedDomainNames(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest request) {
      return blockingUnaryCall(
          getChannel(), getGenerateManagedDomainNamesMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Takes in byte array representing a DER/binary formatted CSR with accountId and name of the environment,
     * validates the domains in CSR and triggers a workflow to submit it to LetsEncrypt and returns a workflowId
     * using which a client can poll the workflow's progress and finally get the signed certificate
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse signCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest request) {
      return blockingUnaryCall(
          getChannel(), getSignCertificateMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Takes in a workflowId representing a workflow in system which tracks the signing of a CSR
     * If successfully complete, it returns a list of certificates in the trust chain with status as SigningStatus.SUCCESS
     * In case of error, it returns the status as SigningStatus.FAILED
     * If the workflow is not yet complete, it returns the status as SigningStatus.IN_PROGRESS
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse pollCertificateSigning(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest request) {
      return blockingUnaryCall(
          getChannel(), getPollCertificateSigningMethodHelper(), getCallOptions(), request);
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

    /**
     * <pre>
     * Takes in a list subdomain patterns as a list with accountId and name of the environment
     * and returns a map of subdomain pattern and generate domain name/pattern
     * For example, if workloadSubdomain of provided accountId is `workload-subdomain`,
     * name of the environment is `env-name` and the supplied array of subdomain patterns
     * is [ "host-a", "*.mlx", "subdomain.mlx" ], the method would return the result as following:
     * {
     *    "host-a": "host-a.env-name.workload-subdomain.cloudera.site",
     *    "*.mlx": "*.mlx.env-name.workload-subdomain.cloudera.site",
     *    "subdomain.mlx": "subdomain.mlx.env-name.workload-subdomain.cloudera.site"
     *  }
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse> generateManagedDomainNames(
        com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGenerateManagedDomainNamesMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Takes in byte array representing a DER/binary formatted CSR with accountId and name of the environment,
     * validates the domains in CSR and triggers a workflow to submit it to LetsEncrypt and returns a workflowId
     * using which a client can poll the workflow's progress and finally get the signed certificate
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse> signCertificate(
        com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSignCertificateMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Takes in a workflowId representing a workflow in system which tracks the signing of a CSR
     * If successfully complete, it returns a list of certificates in the trust chain with status as SigningStatus.SUCCESS
     * In case of error, it returns the status as SigningStatus.FAILED
     * If the workflow is not yet complete, it returns the status as SigningStatus.IN_PROGRESS
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse> pollCertificateSigning(
        com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPollCertificateSigningMethodHelper(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_VERSION = 0;
  private static final int METHODID_CREATE_DNS_ENTRY = 1;
  private static final int METHODID_DELETE_DNS_ENTRY = 2;
  private static final int METHODID_CREATE_CERTIFICATE = 3;
  private static final int METHODID_POLL_CERTIFICATE_CREATION = 4;
  private static final int METHODID_GENERATE_MANAGED_DOMAIN_NAMES = 5;
  private static final int METHODID_SIGN_CERTIFICATE = 6;
  private static final int METHODID_POLL_CERTIFICATE_SIGNING = 7;

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
        case METHODID_GENERATE_MANAGED_DOMAIN_NAMES:
          serviceImpl.generateManagedDomainNames((com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse>) responseObserver);
          break;
        case METHODID_SIGN_CERTIFICATE:
          serviceImpl.signCertificate((com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse>) responseObserver);
          break;
        case METHODID_POLL_CERTIFICATE_SIGNING:
          serviceImpl.pollCertificateSigning((com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse>) responseObserver);
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
              .addMethod(getGenerateManagedDomainNamesMethodHelper())
              .addMethod(getSignCertificateMethodHelper())
              .addMethod(getPollCertificateSigningMethodHelper())
              .build();
        }
      }
    }
    return result;
  }
}
