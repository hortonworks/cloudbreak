package com.cloudera.thunderhead.service.publicendpointmanagement;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class PublicEndpointManagementGrpc {

  private PublicEndpointManagementGrpc() {}

  public static final java.lang.String SERVICE_NAME = "publicendpointmanagement.PublicEndpointManagement";

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
    if ((getGetVersionMethod = PublicEndpointManagementGrpc.getGetVersionMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getGetVersionMethod = PublicEndpointManagementGrpc.getGetVersionMethod) == null) {
          PublicEndpointManagementGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> getCreateDnsEntryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateDnsEntry",
      requestType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest.class,
      responseType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> getCreateDnsEntryMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> getCreateDnsEntryMethod;
    if ((getCreateDnsEntryMethod = PublicEndpointManagementGrpc.getCreateDnsEntryMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getCreateDnsEntryMethod = PublicEndpointManagementGrpc.getCreateDnsEntryMethod) == null) {
          PublicEndpointManagementGrpc.getCreateDnsEntryMethod = getCreateDnsEntryMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateDnsEntry"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> getDeleteDnsEntryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteDnsEntry",
      requestType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest.class,
      responseType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> getDeleteDnsEntryMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> getDeleteDnsEntryMethod;
    if ((getDeleteDnsEntryMethod = PublicEndpointManagementGrpc.getDeleteDnsEntryMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getDeleteDnsEntryMethod = PublicEndpointManagementGrpc.getDeleteDnsEntryMethod) == null) {
          PublicEndpointManagementGrpc.getDeleteDnsEntryMethod = getDeleteDnsEntryMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteDnsEntry"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> getCreateCertificateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateCertificate",
      requestType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest.class,
      responseType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> getCreateCertificateMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> getCreateCertificateMethod;
    if ((getCreateCertificateMethod = PublicEndpointManagementGrpc.getCreateCertificateMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getCreateCertificateMethod = PublicEndpointManagementGrpc.getCreateCertificateMethod) == null) {
          PublicEndpointManagementGrpc.getCreateCertificateMethod = getCreateCertificateMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateCertificate"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> getPollCertificateCreationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PollCertificateCreation",
      requestType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest.class,
      responseType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> getPollCertificateCreationMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> getPollCertificateCreationMethod;
    if ((getPollCertificateCreationMethod = PublicEndpointManagementGrpc.getPollCertificateCreationMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getPollCertificateCreationMethod = PublicEndpointManagementGrpc.getPollCertificateCreationMethod) == null) {
          PublicEndpointManagementGrpc.getPollCertificateCreationMethod = getPollCertificateCreationMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PollCertificateCreation"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse> getGenerateManagedDomainNamesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GenerateManagedDomainNames",
      requestType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest.class,
      responseType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse> getGenerateManagedDomainNamesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse> getGenerateManagedDomainNamesMethod;
    if ((getGenerateManagedDomainNamesMethod = PublicEndpointManagementGrpc.getGenerateManagedDomainNamesMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getGenerateManagedDomainNamesMethod = PublicEndpointManagementGrpc.getGenerateManagedDomainNamesMethod) == null) {
          PublicEndpointManagementGrpc.getGenerateManagedDomainNamesMethod = getGenerateManagedDomainNamesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GenerateManagedDomainNames"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse> getSignCertificateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SignCertificate",
      requestType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest.class,
      responseType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse> getSignCertificateMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse> getSignCertificateMethod;
    if ((getSignCertificateMethod = PublicEndpointManagementGrpc.getSignCertificateMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getSignCertificateMethod = PublicEndpointManagementGrpc.getSignCertificateMethod) == null) {
          PublicEndpointManagementGrpc.getSignCertificateMethod = getSignCertificateMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SignCertificate"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse> getPollCertificateSigningMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PollCertificateSigning",
      requestType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest.class,
      responseType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse> getPollCertificateSigningMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse> getPollCertificateSigningMethod;
    if ((getPollCertificateSigningMethod = PublicEndpointManagementGrpc.getPollCertificateSigningMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getPollCertificateSigningMethod = PublicEndpointManagementGrpc.getPollCertificateSigningMethod) == null) {
          PublicEndpointManagementGrpc.getPollCertificateSigningMethod = getPollCertificateSigningMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PollCertificateSigning"))
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

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse> getRevokeCertificateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RevokeCertificate",
      requestType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest.class,
      responseType = com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest,
      com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse> getRevokeCertificateMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse> getRevokeCertificateMethod;
    if ((getRevokeCertificateMethod = PublicEndpointManagementGrpc.getRevokeCertificateMethod) == null) {
      synchronized (PublicEndpointManagementGrpc.class) {
        if ((getRevokeCertificateMethod = PublicEndpointManagementGrpc.getRevokeCertificateMethod) == null) {
          PublicEndpointManagementGrpc.getRevokeCertificateMethod = getRevokeCertificateMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest, com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RevokeCertificate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PublicEndpointManagementMethodDescriptorSupplier("RevokeCertificate"))
              .build();
        }
      }
    }
    return getRevokeCertificateMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PublicEndpointManagementStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PublicEndpointManagementStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PublicEndpointManagementStub>() {
        @java.lang.Override
        public PublicEndpointManagementStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PublicEndpointManagementStub(channel, callOptions);
        }
      };
    return PublicEndpointManagementStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static PublicEndpointManagementBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PublicEndpointManagementBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PublicEndpointManagementBlockingV2Stub>() {
        @java.lang.Override
        public PublicEndpointManagementBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PublicEndpointManagementBlockingV2Stub(channel, callOptions);
        }
      };
    return PublicEndpointManagementBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PublicEndpointManagementBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PublicEndpointManagementBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PublicEndpointManagementBlockingStub>() {
        @java.lang.Override
        public PublicEndpointManagementBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PublicEndpointManagementBlockingStub(channel, callOptions);
        }
      };
    return PublicEndpointManagementBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PublicEndpointManagementFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PublicEndpointManagementFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PublicEndpointManagementFutureStub>() {
        @java.lang.Override
        public PublicEndpointManagementFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PublicEndpointManagementFutureStub(channel, callOptions);
        }
      };
    return PublicEndpointManagementFutureStub.newStub(factory, channel);
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
     * Create or update a DNS entry. :: Create or update a DNS entry.
     * </pre>
     */
    default void createDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateDnsEntryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete a DNS entry. :: Delete a DNS entry.
     * </pre>
     */
    default void deleteDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteDnsEntryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get a TLS certificate. :: Get a TLS certificate
     * </pre>
     */
    default void createCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateCertificateMethod(), responseObserver);
    }

    /**
     */
    default void pollCertificateCreation(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPollCertificateCreationMethod(), responseObserver);
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
    default void generateManagedDomainNames(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGenerateManagedDomainNamesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Takes in byte array representing a DER/binary formatted CSR with accountId and name of the environment,
     * validates the domains in CSR and triggers a workflow to submit it to LetsEncrypt and returns a workflowId
     * using which a client can poll the workflow's progress and finally get the signed certificate
     * </pre>
     */
    default void signCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSignCertificateMethod(), responseObserver);
    }

    /**
     * <pre>
     * Takes in a workflowId representing a workflow in system which tracks the signing of a CSR
     * If successfully complete, it returns a list of certificates in the trust chain with status as SigningStatus.SUCCESS
     * In case of error, it returns the status as SigningStatus.FAILED
     * If the workflow is not yet complete, it returns the status as SigningStatus.IN_PROGRESS
     * </pre>
     */
    default void pollCertificateSigning(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPollCertificateSigningMethod(), responseObserver);
    }

    /**
     * <pre>
     *Revoke a TLS certificate
     * </pre>
     */
    default void revokeCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRevokeCertificateMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service PublicEndpointManagement.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class PublicEndpointManagementImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return PublicEndpointManagementGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service PublicEndpointManagement.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PublicEndpointManagementStub
      extends io.grpc.stub.AbstractAsyncStub<PublicEndpointManagementStub> {
    private PublicEndpointManagementStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PublicEndpointManagementStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PublicEndpointManagementStub(channel, callOptions);
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
     * Create or update a DNS entry. :: Create or update a DNS entry.
     * </pre>
     */
    public void createDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateDnsEntryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete a DNS entry. :: Delete a DNS entry.
     * </pre>
     */
    public void deleteDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteDnsEntryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get a TLS certificate. :: Get a TLS certificate
     * </pre>
     */
    public void createCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateCertificateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void pollCertificateCreation(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPollCertificateCreationMethod(), getCallOptions()), request, responseObserver);
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
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGenerateManagedDomainNamesMethod(), getCallOptions()), request, responseObserver);
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
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSignCertificateMethod(), getCallOptions()), request, responseObserver);
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
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPollCertificateSigningMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Revoke a TLS certificate
     * </pre>
     */
    public void revokeCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRevokeCertificateMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service PublicEndpointManagement.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PublicEndpointManagementBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<PublicEndpointManagementBlockingV2Stub> {
    private PublicEndpointManagementBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PublicEndpointManagementBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PublicEndpointManagementBlockingV2Stub(channel, callOptions);
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
     * Create or update a DNS entry. :: Create or update a DNS entry.
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse createDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateDnsEntryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a DNS entry. :: Delete a DNS entry.
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse deleteDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteDnsEntryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a TLS certificate. :: Get a TLS certificate
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse createCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateCertificateMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse pollCertificateCreation(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getPollCertificateCreationMethod(), getCallOptions(), request);
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
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse generateManagedDomainNames(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGenerateManagedDomainNamesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Takes in byte array representing a DER/binary formatted CSR with accountId and name of the environment,
     * validates the domains in CSR and triggers a workflow to submit it to LetsEncrypt and returns a workflowId
     * using which a client can poll the workflow's progress and finally get the signed certificate
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse signCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getSignCertificateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Takes in a workflowId representing a workflow in system which tracks the signing of a CSR
     * If successfully complete, it returns a list of certificates in the trust chain with status as SigningStatus.SUCCESS
     * In case of error, it returns the status as SigningStatus.FAILED
     * If the workflow is not yet complete, it returns the status as SigningStatus.IN_PROGRESS
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse pollCertificateSigning(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getPollCertificateSigningMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Revoke a TLS certificate
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse revokeCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRevokeCertificateMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service PublicEndpointManagement.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PublicEndpointManagementBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<PublicEndpointManagementBlockingStub> {
    private PublicEndpointManagementBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PublicEndpointManagementBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PublicEndpointManagementBlockingStub(channel, callOptions);
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
     * Create or update a DNS entry. :: Create or update a DNS entry.
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse createDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateDnsEntryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a DNS entry. :: Delete a DNS entry.
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse deleteDnsEntry(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteDnsEntryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a TLS certificate. :: Get a TLS certificate
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse createCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateCertificateMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse pollCertificateCreation(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPollCertificateCreationMethod(), getCallOptions(), request);
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
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGenerateManagedDomainNamesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Takes in byte array representing a DER/binary formatted CSR with accountId and name of the environment,
     * validates the domains in CSR and triggers a workflow to submit it to LetsEncrypt and returns a workflowId
     * using which a client can poll the workflow's progress and finally get the signed certificate
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse signCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSignCertificateMethod(), getCallOptions(), request);
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
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPollCertificateSigningMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Revoke a TLS certificate
     * </pre>
     */
    public com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse revokeCertificate(com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRevokeCertificateMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service PublicEndpointManagement.
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class PublicEndpointManagementFutureStub
      extends io.grpc.stub.AbstractFutureStub<PublicEndpointManagementFutureStub> {
    private PublicEndpointManagementFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PublicEndpointManagementFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PublicEndpointManagementFutureStub(channel, callOptions);
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
     * Create or update a DNS entry. :: Create or update a DNS entry.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse> createDnsEntry(
        com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateDnsEntryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete a DNS entry. :: Delete a DNS entry.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse> deleteDnsEntry(
        com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteDnsEntryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get a TLS certificate. :: Get a TLS certificate
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse> createCertificate(
        com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateCertificateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse> pollCertificateCreation(
        com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPollCertificateCreationMethod(), getCallOptions()), request);
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
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGenerateManagedDomainNamesMethod(), getCallOptions()), request);
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
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSignCertificateMethod(), getCallOptions()), request);
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
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPollCertificateSigningMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Revoke a TLS certificate
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse> revokeCertificate(
        com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRevokeCertificateMethod(), getCallOptions()), request);
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
  private static final int METHODID_REVOKE_CERTIFICATE = 8;

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
        case METHODID_REVOKE_CERTIFICATE:
          serviceImpl.revokeCertificate((com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse>) responseObserver);
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
          getCreateDnsEntryMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest,
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse>(
                service, METHODID_CREATE_DNS_ENTRY)))
        .addMethod(
          getDeleteDnsEntryMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest,
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse>(
                service, METHODID_DELETE_DNS_ENTRY)))
        .addMethod(
          getCreateCertificateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest,
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateResponse>(
                service, METHODID_CREATE_CERTIFICATE)))
        .addMethod(
          getPollCertificateCreationMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest,
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse>(
                service, METHODID_POLL_CERTIFICATE_CREATION)))
        .addMethod(
          getGenerateManagedDomainNamesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest,
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse>(
                service, METHODID_GENERATE_MANAGED_DOMAIN_NAMES)))
        .addMethod(
          getSignCertificateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest,
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningResponse>(
                service, METHODID_SIGN_CERTIFICATE)))
        .addMethod(
          getPollCertificateSigningMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest,
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse>(
                service, METHODID_POLL_CERTIFICATE_SIGNING)))
        .addMethod(
          getRevokeCertificateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateRequest,
              com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.RevokeCertificateResponse>(
                service, METHODID_REVOKE_CERTIFICATE)))
        .build();
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
    private final java.lang.String methodName;

    PublicEndpointManagementMethodDescriptorSupplier(java.lang.String methodName) {
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
              .addMethod(getGetVersionMethod())
              .addMethod(getCreateDnsEntryMethod())
              .addMethod(getDeleteDnsEntryMethod())
              .addMethod(getCreateCertificateMethod())
              .addMethod(getPollCertificateCreationMethod())
              .addMethod(getGenerateManagedDomainNamesMethod())
              .addMethod(getSignCertificateMethod())
              .addMethod(getPollCertificateSigningMethod())
              .addMethod(getRevokeCertificateMethod())
              .build();
        }
      }
    }
    return result;
  }
}
