package com.cloudera.thunderhead.service.usermanagement;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * For future compatibility, all rpcs must take a request and return a response
 * even if there is initially no content for these messages.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.42.1)",
    comments = "Source: usermanagement.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class UserManagementGrpc {

  private UserManagementGrpc() {}

  public static final String SERVICE_NAME = "usermanagement.UserManagement";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> getInteractiveLoginMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "InteractiveLogin",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> getInteractiveLoginMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> getInteractiveLoginMethod;
    if ((getInteractiveLoginMethod = UserManagementGrpc.getInteractiveLoginMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getInteractiveLoginMethod = UserManagementGrpc.getInteractiveLoginMethod) == null) {
          UserManagementGrpc.getInteractiveLoginMethod = getInteractiveLoginMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "InteractiveLogin"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("InteractiveLogin"))
              .build();
        }
      }
    }
    return getInteractiveLoginMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse> getInteractiveLogin3rdPartyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "InteractiveLogin3rdParty",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse> getInteractiveLogin3rdPartyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse> getInteractiveLogin3rdPartyMethod;
    if ((getInteractiveLogin3rdPartyMethod = UserManagementGrpc.getInteractiveLogin3rdPartyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getInteractiveLogin3rdPartyMethod = UserManagementGrpc.getInteractiveLogin3rdPartyMethod) == null) {
          UserManagementGrpc.getInteractiveLogin3rdPartyMethod = getInteractiveLogin3rdPartyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "InteractiveLogin3rdParty"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("InteractiveLogin3rdParty"))
              .build();
        }
      }
    }
    return getInteractiveLogin3rdPartyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalResponse> getInteractiveLoginLocalMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "InteractiveLoginLocal",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalResponse> getInteractiveLoginLocalMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalResponse> getInteractiveLoginLocalMethod;
    if ((getInteractiveLoginLocalMethod = UserManagementGrpc.getInteractiveLoginLocalMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getInteractiveLoginLocalMethod = UserManagementGrpc.getInteractiveLoginLocalMethod) == null) {
          UserManagementGrpc.getInteractiveLoginLocalMethod = getInteractiveLoginLocalMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "InteractiveLoginLocal"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("InteractiveLoginLocal"))
              .build();
        }
      }
    }
    return getInteractiveLoginLocalMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> getDeleteAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteAccount",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> getDeleteAccountMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> getDeleteAccountMethod;
    if ((getDeleteAccountMethod = UserManagementGrpc.getDeleteAccountMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteAccountMethod = UserManagementGrpc.getDeleteAccountMethod) == null) {
          UserManagementGrpc.getDeleteAccountMethod = getDeleteAccountMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DeleteAccount"))
              .build();
        }
      }
    }
    return getDeleteAccountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> getDeleteActorMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteActor",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> getDeleteActorMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> getDeleteActorMethod;
    if ((getDeleteActorMethod = UserManagementGrpc.getDeleteActorMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteActorMethod = UserManagementGrpc.getDeleteActorMethod) == null) {
          UserManagementGrpc.getDeleteActorMethod = getDeleteActorMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteActor"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DeleteActor"))
              .build();
        }
      }
    }
    return getDeleteActorMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> getDeleteTrialUserMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteTrialUser",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> getDeleteTrialUserMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> getDeleteTrialUserMethod;
    if ((getDeleteTrialUserMethod = UserManagementGrpc.getDeleteTrialUserMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteTrialUserMethod = UserManagementGrpc.getDeleteTrialUserMethod) == null) {
          UserManagementGrpc.getDeleteTrialUserMethod = getDeleteTrialUserMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteTrialUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DeleteTrialUser"))
              .build();
        }
      }
    }
    return getDeleteTrialUserMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> getGetAccessKeyVerificationDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAccessKeyVerificationData",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> getGetAccessKeyVerificationDataMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> getGetAccessKeyVerificationDataMethod;
    if ((getGetAccessKeyVerificationDataMethod = UserManagementGrpc.getGetAccessKeyVerificationDataMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetAccessKeyVerificationDataMethod = UserManagementGrpc.getGetAccessKeyVerificationDataMethod) == null) {
          UserManagementGrpc.getGetAccessKeyVerificationDataMethod = getGetAccessKeyVerificationDataMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAccessKeyVerificationData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetAccessKeyVerificationData"))
              .build();
        }
      }
    }
    return getGetAccessKeyVerificationDataMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse> getVerifyInteractiveUserSessionTokenMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "VerifyInteractiveUserSessionToken",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse> getVerifyInteractiveUserSessionTokenMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse> getVerifyInteractiveUserSessionTokenMethod;
    if ((getVerifyInteractiveUserSessionTokenMethod = UserManagementGrpc.getVerifyInteractiveUserSessionTokenMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getVerifyInteractiveUserSessionTokenMethod = UserManagementGrpc.getVerifyInteractiveUserSessionTokenMethod) == null) {
          UserManagementGrpc.getVerifyInteractiveUserSessionTokenMethod = getVerifyInteractiveUserSessionTokenMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "VerifyInteractiveUserSessionToken"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("VerifyInteractiveUserSessionToken"))
              .build();
        }
      }
    }
    return getVerifyInteractiveUserSessionTokenMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenResponse> getVerifyAccessTokenMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "VerifyAccessToken",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenResponse> getVerifyAccessTokenMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenResponse> getVerifyAccessTokenMethod;
    if ((getVerifyAccessTokenMethod = UserManagementGrpc.getVerifyAccessTokenMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getVerifyAccessTokenMethod = UserManagementGrpc.getVerifyAccessTokenMethod) == null) {
          UserManagementGrpc.getVerifyAccessTokenMethod = getVerifyAccessTokenMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "VerifyAccessToken"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("VerifyAccessToken"))
              .build();
        }
      }
    }
    return getVerifyAccessTokenMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> getAuthenticateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Authenticate",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> getAuthenticateMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> getAuthenticateMethod;
    if ((getAuthenticateMethod = UserManagementGrpc.getAuthenticateMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAuthenticateMethod = UserManagementGrpc.getAuthenticateMethod) == null) {
          UserManagementGrpc.getAuthenticateMethod = getAuthenticateMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Authenticate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("Authenticate"))
              .build();
        }
      }
    }
    return getAuthenticateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> getAccessKeyUsageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AccessKeyUsage",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> getAccessKeyUsageMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> getAccessKeyUsageMethod;
    if ((getAccessKeyUsageMethod = UserManagementGrpc.getAccessKeyUsageMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAccessKeyUsageMethod = UserManagementGrpc.getAccessKeyUsageMethod) == null) {
          UserManagementGrpc.getAccessKeyUsageMethod = getAccessKeyUsageMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AccessKeyUsage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("AccessKeyUsage"))
              .build();
        }
      }
    }
    return getAccessKeyUsageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserResponse> getCreateUserMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateUser",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserResponse> getCreateUserMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserResponse> getCreateUserMethod;
    if ((getCreateUserMethod = UserManagementGrpc.getCreateUserMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateUserMethod = UserManagementGrpc.getCreateUserMethod) == null) {
          UserManagementGrpc.getCreateUserMethod = getCreateUserMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CreateUser"))
              .build();
        }
      }
    }
    return getCreateUserMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> getGetUserMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetUser",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> getGetUserMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> getGetUserMethod;
    if ((getGetUserMethod = UserManagementGrpc.getGetUserMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetUserMethod = UserManagementGrpc.getGetUserMethod) == null) {
          UserManagementGrpc.getGetUserMethod = getGetUserMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetUser"))
              .build();
        }
      }
    }
    return getGetUserMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> getListUsersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListUsers",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> getListUsersMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> getListUsersMethod;
    if ((getListUsersMethod = UserManagementGrpc.getListUsersMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListUsersMethod = UserManagementGrpc.getListUsersMethod) == null) {
          UserManagementGrpc.getListUsersMethod = getListUsersMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListUsers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListUsers"))
              .build();
        }
      }
    }
    return getListUsersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> getFindUsersByEmailMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FindUsersByEmail",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> getFindUsersByEmailMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> getFindUsersByEmailMethod;
    if ((getFindUsersByEmailMethod = UserManagementGrpc.getFindUsersByEmailMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getFindUsersByEmailMethod = UserManagementGrpc.getFindUsersByEmailMethod) == null) {
          UserManagementGrpc.getFindUsersByEmailMethod = getFindUsersByEmailMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FindUsersByEmail"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("FindUsersByEmail"))
              .build();
        }
      }
    }
    return getFindUsersByEmailMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersResponse> getFindUsersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FindUsers",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersResponse> getFindUsersMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersResponse> getFindUsersMethod;
    if ((getFindUsersMethod = UserManagementGrpc.getFindUsersMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getFindUsersMethod = UserManagementGrpc.getFindUsersMethod) == null) {
          UserManagementGrpc.getFindUsersMethod = getFindUsersMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FindUsers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("FindUsers"))
              .build();
        }
      }
    }
    return getFindUsersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> getCreateAccessKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateAccessKey",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> getCreateAccessKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> getCreateAccessKeyMethod;
    if ((getCreateAccessKeyMethod = UserManagementGrpc.getCreateAccessKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateAccessKeyMethod = UserManagementGrpc.getCreateAccessKeyMethod) == null) {
          UserManagementGrpc.getCreateAccessKeyMethod = getCreateAccessKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateAccessKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CreateAccessKey"))
              .build();
        }
      }
    }
    return getCreateAccessKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> getUpdateAccessKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateAccessKey",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> getUpdateAccessKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> getUpdateAccessKeyMethod;
    if ((getUpdateAccessKeyMethod = UserManagementGrpc.getUpdateAccessKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUpdateAccessKeyMethod = UserManagementGrpc.getUpdateAccessKeyMethod) == null) {
          UserManagementGrpc.getUpdateAccessKeyMethod = getUpdateAccessKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateAccessKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("UpdateAccessKey"))
              .build();
        }
      }
    }
    return getUpdateAccessKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> getDeleteAccessKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteAccessKey",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> getDeleteAccessKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> getDeleteAccessKeyMethod;
    if ((getDeleteAccessKeyMethod = UserManagementGrpc.getDeleteAccessKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteAccessKeyMethod = UserManagementGrpc.getDeleteAccessKeyMethod) == null) {
          UserManagementGrpc.getDeleteAccessKeyMethod = getDeleteAccessKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteAccessKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DeleteAccessKey"))
              .build();
        }
      }
    }
    return getDeleteAccessKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> getGetAccessKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAccessKey",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> getGetAccessKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> getGetAccessKeyMethod;
    if ((getGetAccessKeyMethod = UserManagementGrpc.getGetAccessKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetAccessKeyMethod = UserManagementGrpc.getGetAccessKeyMethod) == null) {
          UserManagementGrpc.getGetAccessKeyMethod = getGetAccessKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAccessKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetAccessKey"))
              .build();
        }
      }
    }
    return getGetAccessKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> getListAccessKeysMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListAccessKeys",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> getListAccessKeysMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> getListAccessKeysMethod;
    if ((getListAccessKeysMethod = UserManagementGrpc.getListAccessKeysMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListAccessKeysMethod = UserManagementGrpc.getListAccessKeysMethod) == null) {
          UserManagementGrpc.getListAccessKeysMethod = getListAccessKeysMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListAccessKeys"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListAccessKeys"))
              .build();
        }
      }
    }
    return getListAccessKeysMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenResponse> getCreateAccessTokenMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateAccessToken",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenResponse> getCreateAccessTokenMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenResponse> getCreateAccessTokenMethod;
    if ((getCreateAccessTokenMethod = UserManagementGrpc.getCreateAccessTokenMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateAccessTokenMethod = UserManagementGrpc.getCreateAccessTokenMethod) == null) {
          UserManagementGrpc.getCreateAccessTokenMethod = getCreateAccessTokenMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateAccessToken"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CreateAccessToken"))
              .build();
        }
      }
    }
    return getCreateAccessTokenMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenResponse> getDeleteAccessTokenMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteAccessToken",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenResponse> getDeleteAccessTokenMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenResponse> getDeleteAccessTokenMethod;
    if ((getDeleteAccessTokenMethod = UserManagementGrpc.getDeleteAccessTokenMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteAccessTokenMethod = UserManagementGrpc.getDeleteAccessTokenMethod) == null) {
          UserManagementGrpc.getDeleteAccessTokenMethod = getDeleteAccessTokenMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteAccessToken"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DeleteAccessToken"))
              .build();
        }
      }
    }
    return getDeleteAccessTokenMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenResponse> getGetAccessTokenMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAccessToken",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenResponse> getGetAccessTokenMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenResponse> getGetAccessTokenMethod;
    if ((getGetAccessTokenMethod = UserManagementGrpc.getGetAccessTokenMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetAccessTokenMethod = UserManagementGrpc.getGetAccessTokenMethod) == null) {
          UserManagementGrpc.getGetAccessTokenMethod = getGetAccessTokenMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAccessToken"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetAccessToken"))
              .build();
        }
      }
    }
    return getGetAccessTokenMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensResponse> getListAccessTokensMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListAccessTokens",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensResponse> getListAccessTokensMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensResponse> getListAccessTokensMethod;
    if ((getListAccessTokensMethod = UserManagementGrpc.getListAccessTokensMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListAccessTokensMethod = UserManagementGrpc.getListAccessTokensMethod) == null) {
          UserManagementGrpc.getListAccessTokensMethod = getListAccessTokensMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListAccessTokens"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListAccessTokens"))
              .build();
        }
      }
    }
    return getListAccessTokensMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenResponse> getCreateScimAccessTokenMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateScimAccessToken",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenResponse> getCreateScimAccessTokenMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenResponse> getCreateScimAccessTokenMethod;
    if ((getCreateScimAccessTokenMethod = UserManagementGrpc.getCreateScimAccessTokenMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateScimAccessTokenMethod = UserManagementGrpc.getCreateScimAccessTokenMethod) == null) {
          UserManagementGrpc.getCreateScimAccessTokenMethod = getCreateScimAccessTokenMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateScimAccessToken"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CreateScimAccessToken"))
              .build();
        }
      }
    }
    return getCreateScimAccessTokenMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenResponse> getDeleteScimAccessTokenMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteScimAccessToken",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenResponse> getDeleteScimAccessTokenMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenResponse> getDeleteScimAccessTokenMethod;
    if ((getDeleteScimAccessTokenMethod = UserManagementGrpc.getDeleteScimAccessTokenMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteScimAccessTokenMethod = UserManagementGrpc.getDeleteScimAccessTokenMethod) == null) {
          UserManagementGrpc.getDeleteScimAccessTokenMethod = getDeleteScimAccessTokenMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteScimAccessToken"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DeleteScimAccessToken"))
              .build();
        }
      }
    }
    return getDeleteScimAccessTokenMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensResponse> getListScimAccessTokensMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListScimAccessTokens",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensResponse> getListScimAccessTokensMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensResponse> getListScimAccessTokensMethod;
    if ((getListScimAccessTokensMethod = UserManagementGrpc.getListScimAccessTokensMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListScimAccessTokensMethod = UserManagementGrpc.getListScimAccessTokensMethod) == null) {
          UserManagementGrpc.getListScimAccessTokensMethod = getListScimAccessTokensMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListScimAccessTokens"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListScimAccessTokens"))
              .build();
        }
      }
    }
    return getListScimAccessTokensMethod;
  }

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
    if ((getGetVersionMethod = UserManagementGrpc.getGetVersionMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetVersionMethod = UserManagementGrpc.getGetVersionMethod) == null) {
          UserManagementGrpc.getGetVersionMethod = getGetVersionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.common.version.Version.VersionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetVersion"))
              .build();
        }
      }
    }
    return getGetVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> getGetAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAccount",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> getGetAccountMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> getGetAccountMethod;
    if ((getGetAccountMethod = UserManagementGrpc.getGetAccountMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetAccountMethod = UserManagementGrpc.getGetAccountMethod) == null) {
          UserManagementGrpc.getGetAccountMethod = getGetAccountMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetAccount"))
              .build();
        }
      }
    }
    return getGetAccountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> getListAccountsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListAccounts",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> getListAccountsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> getListAccountsMethod;
    if ((getListAccountsMethod = UserManagementGrpc.getListAccountsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListAccountsMethod = UserManagementGrpc.getListAccountsMethod) == null) {
          UserManagementGrpc.getListAccountsMethod = getListAccountsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListAccounts"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListAccounts"))
              .build();
        }
      }
    }
    return getListAccountsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> getGetRightsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetRights",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> getGetRightsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> getGetRightsMethod;
    if ((getGetRightsMethod = UserManagementGrpc.getGetRightsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetRightsMethod = UserManagementGrpc.getGetRightsMethod) == null) {
          UserManagementGrpc.getGetRightsMethod = getGetRightsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetRights"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetRights"))
              .build();
        }
      }
    }
    return getGetRightsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> getCheckRightsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CheckRights",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> getCheckRightsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> getCheckRightsMethod;
    if ((getCheckRightsMethod = UserManagementGrpc.getCheckRightsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCheckRightsMethod = UserManagementGrpc.getCheckRightsMethod) == null) {
          UserManagementGrpc.getCheckRightsMethod = getCheckRightsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CheckRights"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CheckRights"))
              .build();
        }
      }
    }
    return getCheckRightsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> getCreateAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateAccount",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> getCreateAccountMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> getCreateAccountMethod;
    if ((getCreateAccountMethod = UserManagementGrpc.getCreateAccountMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateAccountMethod = UserManagementGrpc.getCreateAccountMethod) == null) {
          UserManagementGrpc.getCreateAccountMethod = getCreateAccountMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CreateAccount"))
              .build();
        }
      }
    }
    return getCreateAccountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountResponse> getCreateTrialAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateTrialAccount",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountResponse> getCreateTrialAccountMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountResponse> getCreateTrialAccountMethod;
    if ((getCreateTrialAccountMethod = UserManagementGrpc.getCreateTrialAccountMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateTrialAccountMethod = UserManagementGrpc.getCreateTrialAccountMethod) == null) {
          UserManagementGrpc.getCreateTrialAccountMethod = getCreateTrialAccountMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateTrialAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CreateTrialAccount"))
              .build();
        }
      }
    }
    return getCreateTrialAccountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountResponse> getCreateC1CAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateC1CAccount",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountResponse> getCreateC1CAccountMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountResponse> getCreateC1CAccountMethod;
    if ((getCreateC1CAccountMethod = UserManagementGrpc.getCreateC1CAccountMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateC1CAccountMethod = UserManagementGrpc.getCreateC1CAccountMethod) == null) {
          UserManagementGrpc.getCreateC1CAccountMethod = getCreateC1CAccountMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateC1CAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CreateC1CAccount"))
              .build();
        }
      }
    }
    return getCreateC1CAccountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailResponse> getVerifyC1CEmailMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "VerifyC1CEmail",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailResponse> getVerifyC1CEmailMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailResponse> getVerifyC1CEmailMethod;
    if ((getVerifyC1CEmailMethod = UserManagementGrpc.getVerifyC1CEmailMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getVerifyC1CEmailMethod = UserManagementGrpc.getVerifyC1CEmailMethod) == null) {
          UserManagementGrpc.getVerifyC1CEmailMethod = getVerifyC1CEmailMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "VerifyC1CEmail"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("VerifyC1CEmail"))
              .build();
        }
      }
    }
    return getVerifyC1CEmailMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> getGrantEntitlementMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GrantEntitlement",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> getGrantEntitlementMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> getGrantEntitlementMethod;
    if ((getGrantEntitlementMethod = UserManagementGrpc.getGrantEntitlementMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGrantEntitlementMethod = UserManagementGrpc.getGrantEntitlementMethod) == null) {
          UserManagementGrpc.getGrantEntitlementMethod = getGrantEntitlementMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GrantEntitlement"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GrantEntitlement"))
              .build();
        }
      }
    }
    return getGrantEntitlementMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> getRevokeEntitlementMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RevokeEntitlement",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> getRevokeEntitlementMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> getRevokeEntitlementMethod;
    if ((getRevokeEntitlementMethod = UserManagementGrpc.getRevokeEntitlementMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getRevokeEntitlementMethod = UserManagementGrpc.getRevokeEntitlementMethod) == null) {
          UserManagementGrpc.getRevokeEntitlementMethod = getRevokeEntitlementMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RevokeEntitlement"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("RevokeEntitlement"))
              .build();
        }
      }
    }
    return getRevokeEntitlementMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedResponse> getEnsureDefaultEntitlementsGrantedMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "EnsureDefaultEntitlementsGranted",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedResponse> getEnsureDefaultEntitlementsGrantedMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedResponse> getEnsureDefaultEntitlementsGrantedMethod;
    if ((getEnsureDefaultEntitlementsGrantedMethod = UserManagementGrpc.getEnsureDefaultEntitlementsGrantedMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getEnsureDefaultEntitlementsGrantedMethod = UserManagementGrpc.getEnsureDefaultEntitlementsGrantedMethod) == null) {
          UserManagementGrpc.getEnsureDefaultEntitlementsGrantedMethod = getEnsureDefaultEntitlementsGrantedMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "EnsureDefaultEntitlementsGranted"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("EnsureDefaultEntitlementsGranted"))
              .build();
        }
      }
    }
    return getEnsureDefaultEntitlementsGrantedMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> getAssignRoleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AssignRole",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> getAssignRoleMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> getAssignRoleMethod;
    if ((getAssignRoleMethod = UserManagementGrpc.getAssignRoleMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAssignRoleMethod = UserManagementGrpc.getAssignRoleMethod) == null) {
          UserManagementGrpc.getAssignRoleMethod = getAssignRoleMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AssignRole"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("AssignRole"))
              .build();
        }
      }
    }
    return getAssignRoleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> getUnassignRoleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnassignRole",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> getUnassignRoleMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> getUnassignRoleMethod;
    if ((getUnassignRoleMethod = UserManagementGrpc.getUnassignRoleMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUnassignRoleMethod = UserManagementGrpc.getUnassignRoleMethod) == null) {
          UserManagementGrpc.getUnassignRoleMethod = getUnassignRoleMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnassignRole"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("UnassignRole"))
              .build();
        }
      }
    }
    return getUnassignRoleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> getListAssignedRolesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListAssignedRoles",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> getListAssignedRolesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> getListAssignedRolesMethod;
    if ((getListAssignedRolesMethod = UserManagementGrpc.getListAssignedRolesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListAssignedRolesMethod = UserManagementGrpc.getListAssignedRolesMethod) == null) {
          UserManagementGrpc.getListAssignedRolesMethod = getListAssignedRolesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListAssignedRoles"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListAssignedRoles"))
              .build();
        }
      }
    }
    return getListAssignedRolesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> getAssignResourceRoleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AssignResourceRole",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> getAssignResourceRoleMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> getAssignResourceRoleMethod;
    if ((getAssignResourceRoleMethod = UserManagementGrpc.getAssignResourceRoleMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAssignResourceRoleMethod = UserManagementGrpc.getAssignResourceRoleMethod) == null) {
          UserManagementGrpc.getAssignResourceRoleMethod = getAssignResourceRoleMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AssignResourceRole"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("AssignResourceRole"))
              .build();
        }
      }
    }
    return getAssignResourceRoleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> getUnassignResourceRoleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnassignResourceRole",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> getUnassignResourceRoleMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> getUnassignResourceRoleMethod;
    if ((getUnassignResourceRoleMethod = UserManagementGrpc.getUnassignResourceRoleMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUnassignResourceRoleMethod = UserManagementGrpc.getUnassignResourceRoleMethod) == null) {
          UserManagementGrpc.getUnassignResourceRoleMethod = getUnassignResourceRoleMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnassignResourceRole"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("UnassignResourceRole"))
              .build();
        }
      }
    }
    return getUnassignResourceRoleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> getListAssignedResourceRolesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListAssignedResourceRoles",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> getListAssignedResourceRolesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> getListAssignedResourceRolesMethod;
    if ((getListAssignedResourceRolesMethod = UserManagementGrpc.getListAssignedResourceRolesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListAssignedResourceRolesMethod = UserManagementGrpc.getListAssignedResourceRolesMethod) == null) {
          UserManagementGrpc.getListAssignedResourceRolesMethod = getListAssignedResourceRolesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListAssignedResourceRoles"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListAssignedResourceRoles"))
              .build();
        }
      }
    }
    return getListAssignedResourceRolesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> getListRolesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListRoles",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> getListRolesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> getListRolesMethod;
    if ((getListRolesMethod = UserManagementGrpc.getListRolesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListRolesMethod = UserManagementGrpc.getListRolesMethod) == null) {
          UserManagementGrpc.getListRolesMethod = getListRolesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListRoles"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListRoles"))
              .build();
        }
      }
    }
    return getListRolesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> getListResourceRolesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListResourceRoles",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> getListResourceRolesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> getListResourceRolesMethod;
    if ((getListResourceRolesMethod = UserManagementGrpc.getListResourceRolesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListResourceRolesMethod = UserManagementGrpc.getListResourceRolesMethod) == null) {
          UserManagementGrpc.getListResourceRolesMethod = getListResourceRolesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListResourceRoles"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListResourceRoles"))
              .build();
        }
      }
    }
    return getListResourceRolesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> getListResourceAssigneesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListResourceAssignees",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> getListResourceAssigneesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> getListResourceAssigneesMethod;
    if ((getListResourceAssigneesMethod = UserManagementGrpc.getListResourceAssigneesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListResourceAssigneesMethod = UserManagementGrpc.getListResourceAssigneesMethod) == null) {
          UserManagementGrpc.getListResourceAssigneesMethod = getListResourceAssigneesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListResourceAssignees"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListResourceAssignees"))
              .build();
        }
      }
    }
    return getListResourceAssigneesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> getUpdateClouderaManagerLicenseKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateClouderaManagerLicenseKey",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> getUpdateClouderaManagerLicenseKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> getUpdateClouderaManagerLicenseKeyMethod;
    if ((getUpdateClouderaManagerLicenseKeyMethod = UserManagementGrpc.getUpdateClouderaManagerLicenseKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUpdateClouderaManagerLicenseKeyMethod = UserManagementGrpc.getUpdateClouderaManagerLicenseKeyMethod) == null) {
          UserManagementGrpc.getUpdateClouderaManagerLicenseKeyMethod = getUpdateClouderaManagerLicenseKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateClouderaManagerLicenseKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("UpdateClouderaManagerLicenseKey"))
              .build();
        }
      }
    }
    return getUpdateClouderaManagerLicenseKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> getInitiateSupportCaseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "InitiateSupportCase",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> getInitiateSupportCaseMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> getInitiateSupportCaseMethod;
    if ((getInitiateSupportCaseMethod = UserManagementGrpc.getInitiateSupportCaseMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getInitiateSupportCaseMethod = UserManagementGrpc.getInitiateSupportCaseMethod) == null) {
          UserManagementGrpc.getInitiateSupportCaseMethod = getInitiateSupportCaseMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "InitiateSupportCase"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("InitiateSupportCase"))
              .build();
        }
      }
    }
    return getInitiateSupportCaseMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> getNotifyResourceDeletedMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "NotifyResourceDeleted",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> getNotifyResourceDeletedMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> getNotifyResourceDeletedMethod;
    if ((getNotifyResourceDeletedMethod = UserManagementGrpc.getNotifyResourceDeletedMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getNotifyResourceDeletedMethod = UserManagementGrpc.getNotifyResourceDeletedMethod) == null) {
          UserManagementGrpc.getNotifyResourceDeletedMethod = getNotifyResourceDeletedMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "NotifyResourceDeleted"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("NotifyResourceDeleted"))
              .build();
        }
      }
    }
    return getNotifyResourceDeletedMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> getCreateMachineUserMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateMachineUser",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> getCreateMachineUserMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> getCreateMachineUserMethod;
    if ((getCreateMachineUserMethod = UserManagementGrpc.getCreateMachineUserMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateMachineUserMethod = UserManagementGrpc.getCreateMachineUserMethod) == null) {
          UserManagementGrpc.getCreateMachineUserMethod = getCreateMachineUserMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateMachineUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CreateMachineUser"))
              .build();
        }
      }
    }
    return getCreateMachineUserMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> getListMachineUsersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListMachineUsers",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> getListMachineUsersMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> getListMachineUsersMethod;
    if ((getListMachineUsersMethod = UserManagementGrpc.getListMachineUsersMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListMachineUsersMethod = UserManagementGrpc.getListMachineUsersMethod) == null) {
          UserManagementGrpc.getListMachineUsersMethod = getListMachineUsersMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListMachineUsers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListMachineUsers"))
              .build();
        }
      }
    }
    return getListMachineUsersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> getDeleteMachineUserMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteMachineUser",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> getDeleteMachineUserMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> getDeleteMachineUserMethod;
    if ((getDeleteMachineUserMethod = UserManagementGrpc.getDeleteMachineUserMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteMachineUserMethod = UserManagementGrpc.getDeleteMachineUserMethod) == null) {
          UserManagementGrpc.getDeleteMachineUserMethod = getDeleteMachineUserMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteMachineUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DeleteMachineUser"))
              .build();
        }
      }
    }
    return getDeleteMachineUserMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> getListResourceRoleAssignmentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListResourceRoleAssignments",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> getListResourceRoleAssignmentsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> getListResourceRoleAssignmentsMethod;
    if ((getListResourceRoleAssignmentsMethod = UserManagementGrpc.getListResourceRoleAssignmentsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListResourceRoleAssignmentsMethod = UserManagementGrpc.getListResourceRoleAssignmentsMethod) == null) {
          UserManagementGrpc.getListResourceRoleAssignmentsMethod = getListResourceRoleAssignmentsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListResourceRoleAssignments"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListResourceRoleAssignments"))
              .build();
        }
      }
    }
    return getListResourceRoleAssignmentsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> getSetAccountMessagesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetAccountMessages",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> getSetAccountMessagesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> getSetAccountMessagesMethod;
    if ((getSetAccountMessagesMethod = UserManagementGrpc.getSetAccountMessagesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getSetAccountMessagesMethod = UserManagementGrpc.getSetAccountMessagesMethod) == null) {
          UserManagementGrpc.getSetAccountMessagesMethod = getSetAccountMessagesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetAccountMessages"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("SetAccountMessages"))
              .build();
        }
      }
    }
    return getSetAccountMessagesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> getAcceptTermsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AcceptTerms",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> getAcceptTermsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> getAcceptTermsMethod;
    if ((getAcceptTermsMethod = UserManagementGrpc.getAcceptTermsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAcceptTermsMethod = UserManagementGrpc.getAcceptTermsMethod) == null) {
          UserManagementGrpc.getAcceptTermsMethod = getAcceptTermsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AcceptTerms"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("AcceptTerms"))
              .build();
        }
      }
    }
    return getAcceptTermsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> getClearAcceptedTermsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ClearAcceptedTerms",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> getClearAcceptedTermsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> getClearAcceptedTermsMethod;
    if ((getClearAcceptedTermsMethod = UserManagementGrpc.getClearAcceptedTermsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getClearAcceptedTermsMethod = UserManagementGrpc.getClearAcceptedTermsMethod) == null) {
          UserManagementGrpc.getClearAcceptedTermsMethod = getClearAcceptedTermsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ClearAcceptedTerms"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ClearAcceptedTerms"))
              .build();
        }
      }
    }
    return getClearAcceptedTermsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> getDescribeTermsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeTerms",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> getDescribeTermsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> getDescribeTermsMethod;
    if ((getDescribeTermsMethod = UserManagementGrpc.getDescribeTermsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDescribeTermsMethod = UserManagementGrpc.getDescribeTermsMethod) == null) {
          UserManagementGrpc.getDescribeTermsMethod = getDescribeTermsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeTerms"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DescribeTerms"))
              .build();
        }
      }
    }
    return getDescribeTermsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> getListTermsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListTerms",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> getListTermsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> getListTermsMethod;
    if ((getListTermsMethod = UserManagementGrpc.getListTermsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListTermsMethod = UserManagementGrpc.getListTermsMethod) == null) {
          UserManagementGrpc.getListTermsMethod = getListTermsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListTerms"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListTerms"))
              .build();
        }
      }
    }
    return getListTermsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> getListEntitlementsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListEntitlements",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> getListEntitlementsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> getListEntitlementsMethod;
    if ((getListEntitlementsMethod = UserManagementGrpc.getListEntitlementsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListEntitlementsMethod = UserManagementGrpc.getListEntitlementsMethod) == null) {
          UserManagementGrpc.getListEntitlementsMethod = getListEntitlementsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListEntitlements"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListEntitlements"))
              .build();
        }
      }
    }
    return getListEntitlementsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> getSetTermsAcceptanceExpiryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetTermsAcceptanceExpiry",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> getSetTermsAcceptanceExpiryMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> getSetTermsAcceptanceExpiryMethod;
    if ((getSetTermsAcceptanceExpiryMethod = UserManagementGrpc.getSetTermsAcceptanceExpiryMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getSetTermsAcceptanceExpiryMethod = UserManagementGrpc.getSetTermsAcceptanceExpiryMethod) == null) {
          UserManagementGrpc.getSetTermsAcceptanceExpiryMethod = getSetTermsAcceptanceExpiryMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetTermsAcceptanceExpiry"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("SetTermsAcceptanceExpiry"))
              .build();
        }
      }
    }
    return getSetTermsAcceptanceExpiryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> getConfirmAzureSubscriptionVerifiedMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ConfirmAzureSubscriptionVerified",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> getConfirmAzureSubscriptionVerifiedMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> getConfirmAzureSubscriptionVerifiedMethod;
    if ((getConfirmAzureSubscriptionVerifiedMethod = UserManagementGrpc.getConfirmAzureSubscriptionVerifiedMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getConfirmAzureSubscriptionVerifiedMethod = UserManagementGrpc.getConfirmAzureSubscriptionVerifiedMethod) == null) {
          UserManagementGrpc.getConfirmAzureSubscriptionVerifiedMethod = getConfirmAzureSubscriptionVerifiedMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ConfirmAzureSubscriptionVerified"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ConfirmAzureSubscriptionVerified"))
              .build();
        }
      }
    }
    return getConfirmAzureSubscriptionVerifiedMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> getInsertAzureSubscriptionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "InsertAzureSubscription",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> getInsertAzureSubscriptionMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> getInsertAzureSubscriptionMethod;
    if ((getInsertAzureSubscriptionMethod = UserManagementGrpc.getInsertAzureSubscriptionMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getInsertAzureSubscriptionMethod = UserManagementGrpc.getInsertAzureSubscriptionMethod) == null) {
          UserManagementGrpc.getInsertAzureSubscriptionMethod = getInsertAzureSubscriptionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "InsertAzureSubscription"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("InsertAzureSubscription"))
              .build();
        }
      }
    }
    return getInsertAzureSubscriptionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> getCreateGroupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateGroup",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> getCreateGroupMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> getCreateGroupMethod;
    if ((getCreateGroupMethod = UserManagementGrpc.getCreateGroupMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateGroupMethod = UserManagementGrpc.getCreateGroupMethod) == null) {
          UserManagementGrpc.getCreateGroupMethod = getCreateGroupMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateGroup"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CreateGroup"))
              .build();
        }
      }
    }
    return getCreateGroupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> getDeleteGroupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteGroup",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> getDeleteGroupMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> getDeleteGroupMethod;
    if ((getDeleteGroupMethod = UserManagementGrpc.getDeleteGroupMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteGroupMethod = UserManagementGrpc.getDeleteGroupMethod) == null) {
          UserManagementGrpc.getDeleteGroupMethod = getDeleteGroupMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteGroup"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DeleteGroup"))
              .build();
        }
      }
    }
    return getDeleteGroupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupResponse> getGetGroupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetGroup",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupResponse> getGetGroupMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupResponse> getGetGroupMethod;
    if ((getGetGroupMethod = UserManagementGrpc.getGetGroupMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetGroupMethod = UserManagementGrpc.getGetGroupMethod) == null) {
          UserManagementGrpc.getGetGroupMethod = getGetGroupMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetGroup"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetGroup"))
              .build();
        }
      }
    }
    return getGetGroupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> getListGroupsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListGroups",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> getListGroupsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> getListGroupsMethod;
    if ((getListGroupsMethod = UserManagementGrpc.getListGroupsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListGroupsMethod = UserManagementGrpc.getListGroupsMethod) == null) {
          UserManagementGrpc.getListGroupsMethod = getListGroupsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListGroups"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListGroups"))
              .build();
        }
      }
    }
    return getListGroupsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupResponse> getUpdateGroupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateGroup",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupResponse> getUpdateGroupMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupResponse> getUpdateGroupMethod;
    if ((getUpdateGroupMethod = UserManagementGrpc.getUpdateGroupMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUpdateGroupMethod = UserManagementGrpc.getUpdateGroupMethod) == null) {
          UserManagementGrpc.getUpdateGroupMethod = getUpdateGroupMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateGroup"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("UpdateGroup"))
              .build();
        }
      }
    }
    return getUpdateGroupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> getAddMemberToGroupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AddMemberToGroup",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> getAddMemberToGroupMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> getAddMemberToGroupMethod;
    if ((getAddMemberToGroupMethod = UserManagementGrpc.getAddMemberToGroupMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAddMemberToGroupMethod = UserManagementGrpc.getAddMemberToGroupMethod) == null) {
          UserManagementGrpc.getAddMemberToGroupMethod = getAddMemberToGroupMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AddMemberToGroup"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("AddMemberToGroup"))
              .build();
        }
      }
    }
    return getAddMemberToGroupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> getRemoveMemberFromGroupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RemoveMemberFromGroup",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> getRemoveMemberFromGroupMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> getRemoveMemberFromGroupMethod;
    if ((getRemoveMemberFromGroupMethod = UserManagementGrpc.getRemoveMemberFromGroupMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getRemoveMemberFromGroupMethod = UserManagementGrpc.getRemoveMemberFromGroupMethod) == null) {
          UserManagementGrpc.getRemoveMemberFromGroupMethod = getRemoveMemberFromGroupMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RemoveMemberFromGroup"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("RemoveMemberFromGroup"))
              .build();
        }
      }
    }
    return getRemoveMemberFromGroupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> getListGroupMembersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListGroupMembers",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> getListGroupMembersMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> getListGroupMembersMethod;
    if ((getListGroupMembersMethod = UserManagementGrpc.getListGroupMembersMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListGroupMembersMethod = UserManagementGrpc.getListGroupMembersMethod) == null) {
          UserManagementGrpc.getListGroupMembersMethod = getListGroupMembersMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListGroupMembers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListGroupMembers"))
              .build();
        }
      }
    }
    return getListGroupMembersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> getListGroupsForMemberMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListGroupsForMember",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> getListGroupsForMemberMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> getListGroupsForMemberMethod;
    if ((getListGroupsForMemberMethod = UserManagementGrpc.getListGroupsForMemberMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListGroupsForMemberMethod = UserManagementGrpc.getListGroupsForMemberMethod) == null) {
          UserManagementGrpc.getListGroupsForMemberMethod = getListGroupsForMemberMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListGroupsForMember"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListGroupsForMember"))
              .build();
        }
      }
    }
    return getListGroupsForMemberMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse> getListWorkloadAdministrationGroupsForMemberMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListWorkloadAdministrationGroupsForMember",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse> getListWorkloadAdministrationGroupsForMemberMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse> getListWorkloadAdministrationGroupsForMemberMethod;
    if ((getListWorkloadAdministrationGroupsForMemberMethod = UserManagementGrpc.getListWorkloadAdministrationGroupsForMemberMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListWorkloadAdministrationGroupsForMemberMethod = UserManagementGrpc.getListWorkloadAdministrationGroupsForMemberMethod) == null) {
          UserManagementGrpc.getListWorkloadAdministrationGroupsForMemberMethod = getListWorkloadAdministrationGroupsForMemberMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListWorkloadAdministrationGroupsForMember"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListWorkloadAdministrationGroupsForMember"))
              .build();
        }
      }
    }
    return getListWorkloadAdministrationGroupsForMemberMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> getCreateClusterSshPrivateKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateClusterSshPrivateKey",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> getCreateClusterSshPrivateKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> getCreateClusterSshPrivateKeyMethod;
    if ((getCreateClusterSshPrivateKeyMethod = UserManagementGrpc.getCreateClusterSshPrivateKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateClusterSshPrivateKeyMethod = UserManagementGrpc.getCreateClusterSshPrivateKeyMethod) == null) {
          UserManagementGrpc.getCreateClusterSshPrivateKeyMethod = getCreateClusterSshPrivateKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateClusterSshPrivateKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CreateClusterSshPrivateKey"))
              .build();
        }
      }
    }
    return getCreateClusterSshPrivateKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> getGetClusterSshPrivateKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetClusterSshPrivateKey",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> getGetClusterSshPrivateKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> getGetClusterSshPrivateKeyMethod;
    if ((getGetClusterSshPrivateKeyMethod = UserManagementGrpc.getGetClusterSshPrivateKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetClusterSshPrivateKeyMethod = UserManagementGrpc.getGetClusterSshPrivateKeyMethod) == null) {
          UserManagementGrpc.getGetClusterSshPrivateKeyMethod = getGetClusterSshPrivateKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetClusterSshPrivateKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetClusterSshPrivateKey"))
              .build();
        }
      }
    }
    return getGetClusterSshPrivateKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> getGetAssigneeAuthorizationInformationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAssigneeAuthorizationInformation",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> getGetAssigneeAuthorizationInformationMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> getGetAssigneeAuthorizationInformationMethod;
    if ((getGetAssigneeAuthorizationInformationMethod = UserManagementGrpc.getGetAssigneeAuthorizationInformationMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetAssigneeAuthorizationInformationMethod = UserManagementGrpc.getGetAssigneeAuthorizationInformationMethod) == null) {
          UserManagementGrpc.getGetAssigneeAuthorizationInformationMethod = getGetAssigneeAuthorizationInformationMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAssigneeAuthorizationInformation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetAssigneeAuthorizationInformation"))
              .build();
        }
      }
    }
    return getGetAssigneeAuthorizationInformationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> getCreateIdentityProviderConnectorMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateIdentityProviderConnector",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> getCreateIdentityProviderConnectorMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> getCreateIdentityProviderConnectorMethod;
    if ((getCreateIdentityProviderConnectorMethod = UserManagementGrpc.getCreateIdentityProviderConnectorMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateIdentityProviderConnectorMethod = UserManagementGrpc.getCreateIdentityProviderConnectorMethod) == null) {
          UserManagementGrpc.getCreateIdentityProviderConnectorMethod = getCreateIdentityProviderConnectorMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateIdentityProviderConnector"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CreateIdentityProviderConnector"))
              .build();
        }
      }
    }
    return getCreateIdentityProviderConnectorMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> getListIdentityProviderConnectorsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListIdentityProviderConnectors",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> getListIdentityProviderConnectorsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> getListIdentityProviderConnectorsMethod;
    if ((getListIdentityProviderConnectorsMethod = UserManagementGrpc.getListIdentityProviderConnectorsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListIdentityProviderConnectorsMethod = UserManagementGrpc.getListIdentityProviderConnectorsMethod) == null) {
          UserManagementGrpc.getListIdentityProviderConnectorsMethod = getListIdentityProviderConnectorsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListIdentityProviderConnectors"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListIdentityProviderConnectors"))
              .build();
        }
      }
    }
    return getListIdentityProviderConnectorsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> getDeleteIdentityProviderConnectorMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteIdentityProviderConnector",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> getDeleteIdentityProviderConnectorMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> getDeleteIdentityProviderConnectorMethod;
    if ((getDeleteIdentityProviderConnectorMethod = UserManagementGrpc.getDeleteIdentityProviderConnectorMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteIdentityProviderConnectorMethod = UserManagementGrpc.getDeleteIdentityProviderConnectorMethod) == null) {
          UserManagementGrpc.getDeleteIdentityProviderConnectorMethod = getDeleteIdentityProviderConnectorMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteIdentityProviderConnector"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DeleteIdentityProviderConnector"))
              .build();
        }
      }
    }
    return getDeleteIdentityProviderConnectorMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> getDescribeIdentityProviderConnectorMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeIdentityProviderConnector",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> getDescribeIdentityProviderConnectorMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> getDescribeIdentityProviderConnectorMethod;
    if ((getDescribeIdentityProviderConnectorMethod = UserManagementGrpc.getDescribeIdentityProviderConnectorMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDescribeIdentityProviderConnectorMethod = UserManagementGrpc.getDescribeIdentityProviderConnectorMethod) == null) {
          UserManagementGrpc.getDescribeIdentityProviderConnectorMethod = getDescribeIdentityProviderConnectorMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeIdentityProviderConnector"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DescribeIdentityProviderConnector"))
              .build();
        }
      }
    }
    return getDescribeIdentityProviderConnectorMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> getUpdateIdentityProviderConnectorMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateIdentityProviderConnector",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> getUpdateIdentityProviderConnectorMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> getUpdateIdentityProviderConnectorMethod;
    if ((getUpdateIdentityProviderConnectorMethod = UserManagementGrpc.getUpdateIdentityProviderConnectorMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUpdateIdentityProviderConnectorMethod = UserManagementGrpc.getUpdateIdentityProviderConnectorMethod) == null) {
          UserManagementGrpc.getUpdateIdentityProviderConnectorMethod = getUpdateIdentityProviderConnectorMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateIdentityProviderConnector"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("UpdateIdentityProviderConnector"))
              .build();
        }
      }
    }
    return getUpdateIdentityProviderConnectorMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> getSetClouderaSSOLoginEnabledMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetClouderaSSOLoginEnabled",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> getSetClouderaSSOLoginEnabledMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> getSetClouderaSSOLoginEnabledMethod;
    if ((getSetClouderaSSOLoginEnabledMethod = UserManagementGrpc.getSetClouderaSSOLoginEnabledMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getSetClouderaSSOLoginEnabledMethod = UserManagementGrpc.getSetClouderaSSOLoginEnabledMethod) == null) {
          UserManagementGrpc.getSetClouderaSSOLoginEnabledMethod = getSetClouderaSSOLoginEnabledMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetClouderaSSOLoginEnabled"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("SetClouderaSSOLoginEnabled"))
              .build();
        }
      }
    }
    return getSetClouderaSSOLoginEnabledMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse> getGetIdPMetadataForWorkloadSSOMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetIdPMetadataForWorkloadSSO",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse> getGetIdPMetadataForWorkloadSSOMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse> getGetIdPMetadataForWorkloadSSOMethod;
    if ((getGetIdPMetadataForWorkloadSSOMethod = UserManagementGrpc.getGetIdPMetadataForWorkloadSSOMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetIdPMetadataForWorkloadSSOMethod = UserManagementGrpc.getGetIdPMetadataForWorkloadSSOMethod) == null) {
          UserManagementGrpc.getGetIdPMetadataForWorkloadSSOMethod = getGetIdPMetadataForWorkloadSSOMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetIdPMetadataForWorkloadSSO"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetIdPMetadataForWorkloadSSO"))
              .build();
        }
      }
    }
    return getGetIdPMetadataForWorkloadSSOMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqResponse> getProcessWorkloadSSOAuthnReqMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ProcessWorkloadSSOAuthnReq",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqResponse> getProcessWorkloadSSOAuthnReqMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqResponse> getProcessWorkloadSSOAuthnReqMethod;
    if ((getProcessWorkloadSSOAuthnReqMethod = UserManagementGrpc.getProcessWorkloadSSOAuthnReqMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getProcessWorkloadSSOAuthnReqMethod = UserManagementGrpc.getProcessWorkloadSSOAuthnReqMethod) == null) {
          UserManagementGrpc.getProcessWorkloadSSOAuthnReqMethod = getProcessWorkloadSSOAuthnReqMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ProcessWorkloadSSOAuthnReq"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ProcessWorkloadSSOAuthnReq"))
              .build();
        }
      }
    }
    return getProcessWorkloadSSOAuthnReqMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqResponse> getGenerateControlPlaneSSOAuthnReqMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GenerateControlPlaneSSOAuthnReq",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqResponse> getGenerateControlPlaneSSOAuthnReqMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqResponse> getGenerateControlPlaneSSOAuthnReqMethod;
    if ((getGenerateControlPlaneSSOAuthnReqMethod = UserManagementGrpc.getGenerateControlPlaneSSOAuthnReqMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGenerateControlPlaneSSOAuthnReqMethod = UserManagementGrpc.getGenerateControlPlaneSSOAuthnReqMethod) == null) {
          UserManagementGrpc.getGenerateControlPlaneSSOAuthnReqMethod = getGenerateControlPlaneSSOAuthnReqMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GenerateControlPlaneSSOAuthnReq"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GenerateControlPlaneSSOAuthnReq"))
              .build();
        }
      }
    }
    return getGenerateControlPlaneSSOAuthnReqMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainResponse> getSetWorkloadSubdomainMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetWorkloadSubdomain",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainResponse> getSetWorkloadSubdomainMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainResponse> getSetWorkloadSubdomainMethod;
    if ((getSetWorkloadSubdomainMethod = UserManagementGrpc.getSetWorkloadSubdomainMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getSetWorkloadSubdomainMethod = UserManagementGrpc.getSetWorkloadSubdomainMethod) == null) {
          UserManagementGrpc.getSetWorkloadSubdomainMethod = getSetWorkloadSubdomainMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetWorkloadSubdomain"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("SetWorkloadSubdomain"))
              .build();
        }
      }
    }
    return getSetWorkloadSubdomainMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse> getCreateWorkloadMachineUserMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateWorkloadMachineUser",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse> getCreateWorkloadMachineUserMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse> getCreateWorkloadMachineUserMethod;
    if ((getCreateWorkloadMachineUserMethod = UserManagementGrpc.getCreateWorkloadMachineUserMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateWorkloadMachineUserMethod = UserManagementGrpc.getCreateWorkloadMachineUserMethod) == null) {
          UserManagementGrpc.getCreateWorkloadMachineUserMethod = getCreateWorkloadMachineUserMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateWorkloadMachineUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("CreateWorkloadMachineUser"))
              .build();
        }
      }
    }
    return getCreateWorkloadMachineUserMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserResponse> getDeleteWorkloadMachineUserMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteWorkloadMachineUser",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserResponse> getDeleteWorkloadMachineUserMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserResponse> getDeleteWorkloadMachineUserMethod;
    if ((getDeleteWorkloadMachineUserMethod = UserManagementGrpc.getDeleteWorkloadMachineUserMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteWorkloadMachineUserMethod = UserManagementGrpc.getDeleteWorkloadMachineUserMethod) == null) {
          UserManagementGrpc.getDeleteWorkloadMachineUserMethod = getDeleteWorkloadMachineUserMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteWorkloadMachineUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DeleteWorkloadMachineUser"))
              .build();
        }
      }
    }
    return getDeleteWorkloadMachineUserMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse> getGetWorkloadAdministrationGroupNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetWorkloadAdministrationGroupName",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse> getGetWorkloadAdministrationGroupNameMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse> getGetWorkloadAdministrationGroupNameMethod;
    if ((getGetWorkloadAdministrationGroupNameMethod = UserManagementGrpc.getGetWorkloadAdministrationGroupNameMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetWorkloadAdministrationGroupNameMethod = UserManagementGrpc.getGetWorkloadAdministrationGroupNameMethod) == null) {
          UserManagementGrpc.getGetWorkloadAdministrationGroupNameMethod = getGetWorkloadAdministrationGroupNameMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetWorkloadAdministrationGroupName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetWorkloadAdministrationGroupName"))
              .build();
        }
      }
    }
    return getGetWorkloadAdministrationGroupNameMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse> getSetWorkloadAdministrationGroupNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetWorkloadAdministrationGroupName",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse> getSetWorkloadAdministrationGroupNameMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse> getSetWorkloadAdministrationGroupNameMethod;
    if ((getSetWorkloadAdministrationGroupNameMethod = UserManagementGrpc.getSetWorkloadAdministrationGroupNameMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getSetWorkloadAdministrationGroupNameMethod = UserManagementGrpc.getSetWorkloadAdministrationGroupNameMethod) == null) {
          UserManagementGrpc.getSetWorkloadAdministrationGroupNameMethod = getSetWorkloadAdministrationGroupNameMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetWorkloadAdministrationGroupName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("SetWorkloadAdministrationGroupName"))
              .build();
        }
      }
    }
    return getSetWorkloadAdministrationGroupNameMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse> getDeleteWorkloadAdministrationGroupNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteWorkloadAdministrationGroupName",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse> getDeleteWorkloadAdministrationGroupNameMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse> getDeleteWorkloadAdministrationGroupNameMethod;
    if ((getDeleteWorkloadAdministrationGroupNameMethod = UserManagementGrpc.getDeleteWorkloadAdministrationGroupNameMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteWorkloadAdministrationGroupNameMethod = UserManagementGrpc.getDeleteWorkloadAdministrationGroupNameMethod) == null) {
          UserManagementGrpc.getDeleteWorkloadAdministrationGroupNameMethod = getDeleteWorkloadAdministrationGroupNameMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteWorkloadAdministrationGroupName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DeleteWorkloadAdministrationGroupName"))
              .build();
        }
      }
    }
    return getDeleteWorkloadAdministrationGroupNameMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse> getListWorkloadAdministrationGroupsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListWorkloadAdministrationGroups",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse> getListWorkloadAdministrationGroupsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse> getListWorkloadAdministrationGroupsMethod;
    if ((getListWorkloadAdministrationGroupsMethod = UserManagementGrpc.getListWorkloadAdministrationGroupsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListWorkloadAdministrationGroupsMethod = UserManagementGrpc.getListWorkloadAdministrationGroupsMethod) == null) {
          UserManagementGrpc.getListWorkloadAdministrationGroupsMethod = getListWorkloadAdministrationGroupsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListWorkloadAdministrationGroups"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListWorkloadAdministrationGroups"))
              .build();
        }
      }
    }
    return getListWorkloadAdministrationGroupsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsResponse> getSetActorWorkloadCredentialsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetActorWorkloadCredentials",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsResponse> getSetActorWorkloadCredentialsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsResponse> getSetActorWorkloadCredentialsMethod;
    if ((getSetActorWorkloadCredentialsMethod = UserManagementGrpc.getSetActorWorkloadCredentialsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getSetActorWorkloadCredentialsMethod = UserManagementGrpc.getSetActorWorkloadCredentialsMethod) == null) {
          UserManagementGrpc.getSetActorWorkloadCredentialsMethod = getSetActorWorkloadCredentialsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetActorWorkloadCredentials"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("SetActorWorkloadCredentials"))
              .build();
        }
      }
    }
    return getSetActorWorkloadCredentialsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsResponse> getValidateActorWorkloadCredentialsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ValidateActorWorkloadCredentials",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsResponse> getValidateActorWorkloadCredentialsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsResponse> getValidateActorWorkloadCredentialsMethod;
    if ((getValidateActorWorkloadCredentialsMethod = UserManagementGrpc.getValidateActorWorkloadCredentialsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getValidateActorWorkloadCredentialsMethod = UserManagementGrpc.getValidateActorWorkloadCredentialsMethod) == null) {
          UserManagementGrpc.getValidateActorWorkloadCredentialsMethod = getValidateActorWorkloadCredentialsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ValidateActorWorkloadCredentials"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ValidateActorWorkloadCredentials"))
              .build();
        }
      }
    }
    return getValidateActorWorkloadCredentialsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse> getGetActorWorkloadCredentialsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetActorWorkloadCredentials",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse> getGetActorWorkloadCredentialsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse> getGetActorWorkloadCredentialsMethod;
    if ((getGetActorWorkloadCredentialsMethod = UserManagementGrpc.getGetActorWorkloadCredentialsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetActorWorkloadCredentialsMethod = UserManagementGrpc.getGetActorWorkloadCredentialsMethod) == null) {
          UserManagementGrpc.getGetActorWorkloadCredentialsMethod = getGetActorWorkloadCredentialsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetActorWorkloadCredentials"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetActorWorkloadCredentials"))
              .build();
        }
      }
    }
    return getGetActorWorkloadCredentialsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse> getGetEventGenerationIdsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetEventGenerationIds",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse> getGetEventGenerationIdsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse> getGetEventGenerationIdsMethod;
    if ((getGetEventGenerationIdsMethod = UserManagementGrpc.getGetEventGenerationIdsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetEventGenerationIdsMethod = UserManagementGrpc.getGetEventGenerationIdsMethod) == null) {
          UserManagementGrpc.getGetEventGenerationIdsMethod = getGetEventGenerationIdsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetEventGenerationIds"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetEventGenerationIds"))
              .build();
        }
      }
    }
    return getGetEventGenerationIdsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyResponse> getAddActorSshPublicKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AddActorSshPublicKey",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyResponse> getAddActorSshPublicKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyResponse> getAddActorSshPublicKeyMethod;
    if ((getAddActorSshPublicKeyMethod = UserManagementGrpc.getAddActorSshPublicKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAddActorSshPublicKeyMethod = UserManagementGrpc.getAddActorSshPublicKeyMethod) == null) {
          UserManagementGrpc.getAddActorSshPublicKeyMethod = getAddActorSshPublicKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AddActorSshPublicKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("AddActorSshPublicKey"))
              .build();
        }
      }
    }
    return getAddActorSshPublicKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysResponse> getListActorSshPublicKeysMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListActorSshPublicKeys",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysResponse> getListActorSshPublicKeysMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysResponse> getListActorSshPublicKeysMethod;
    if ((getListActorSshPublicKeysMethod = UserManagementGrpc.getListActorSshPublicKeysMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListActorSshPublicKeysMethod = UserManagementGrpc.getListActorSshPublicKeysMethod) == null) {
          UserManagementGrpc.getListActorSshPublicKeysMethod = getListActorSshPublicKeysMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListActorSshPublicKeys"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListActorSshPublicKeys"))
              .build();
        }
      }
    }
    return getListActorSshPublicKeysMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyResponse> getDescribeActorSshPublicKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeActorSshPublicKey",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyResponse> getDescribeActorSshPublicKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyResponse> getDescribeActorSshPublicKeyMethod;
    if ((getDescribeActorSshPublicKeyMethod = UserManagementGrpc.getDescribeActorSshPublicKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDescribeActorSshPublicKeyMethod = UserManagementGrpc.getDescribeActorSshPublicKeyMethod) == null) {
          UserManagementGrpc.getDescribeActorSshPublicKeyMethod = getDescribeActorSshPublicKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeActorSshPublicKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DescribeActorSshPublicKey"))
              .build();
        }
      }
    }
    return getDescribeActorSshPublicKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyResponse> getDeleteActorSshPublicKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteActorSshPublicKey",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyResponse> getDeleteActorSshPublicKeyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyResponse> getDeleteActorSshPublicKeyMethod;
    if ((getDeleteActorSshPublicKeyMethod = UserManagementGrpc.getDeleteActorSshPublicKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteActorSshPublicKeyMethod = UserManagementGrpc.getDeleteActorSshPublicKeyMethod) == null) {
          UserManagementGrpc.getDeleteActorSshPublicKeyMethod = getDeleteActorSshPublicKeyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteActorSshPublicKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("DeleteActorSshPublicKey"))
              .build();
        }
      }
    }
    return getDeleteActorSshPublicKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyResponse> getSetWorkloadPasswordPolicyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetWorkloadPasswordPolicy",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyResponse> getSetWorkloadPasswordPolicyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyResponse> getSetWorkloadPasswordPolicyMethod;
    if ((getSetWorkloadPasswordPolicyMethod = UserManagementGrpc.getSetWorkloadPasswordPolicyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getSetWorkloadPasswordPolicyMethod = UserManagementGrpc.getSetWorkloadPasswordPolicyMethod) == null) {
          UserManagementGrpc.getSetWorkloadPasswordPolicyMethod = getSetWorkloadPasswordPolicyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetWorkloadPasswordPolicy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("SetWorkloadPasswordPolicy"))
              .build();
        }
      }
    }
    return getSetWorkloadPasswordPolicyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyResponse> getUnsetWorkloadPasswordPolicyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnsetWorkloadPasswordPolicy",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyResponse> getUnsetWorkloadPasswordPolicyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyResponse> getUnsetWorkloadPasswordPolicyMethod;
    if ((getUnsetWorkloadPasswordPolicyMethod = UserManagementGrpc.getUnsetWorkloadPasswordPolicyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUnsetWorkloadPasswordPolicyMethod = UserManagementGrpc.getUnsetWorkloadPasswordPolicyMethod) == null) {
          UserManagementGrpc.getUnsetWorkloadPasswordPolicyMethod = getUnsetWorkloadPasswordPolicyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnsetWorkloadPasswordPolicy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("UnsetWorkloadPasswordPolicy"))
              .build();
        }
      }
    }
    return getUnsetWorkloadPasswordPolicyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyResponse> getSetAuthenticationPolicyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetAuthenticationPolicy",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyResponse> getSetAuthenticationPolicyMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyResponse> getSetAuthenticationPolicyMethod;
    if ((getSetAuthenticationPolicyMethod = UserManagementGrpc.getSetAuthenticationPolicyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getSetAuthenticationPolicyMethod = UserManagementGrpc.getSetAuthenticationPolicyMethod) == null) {
          UserManagementGrpc.getSetAuthenticationPolicyMethod = getSetAuthenticationPolicyMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetAuthenticationPolicy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("SetAuthenticationPolicy"))
              .build();
        }
      }
    }
    return getSetAuthenticationPolicyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityResponse> getAssignCloudIdentityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AssignCloudIdentity",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityResponse> getAssignCloudIdentityMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityResponse> getAssignCloudIdentityMethod;
    if ((getAssignCloudIdentityMethod = UserManagementGrpc.getAssignCloudIdentityMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAssignCloudIdentityMethod = UserManagementGrpc.getAssignCloudIdentityMethod) == null) {
          UserManagementGrpc.getAssignCloudIdentityMethod = getAssignCloudIdentityMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AssignCloudIdentity"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("AssignCloudIdentity"))
              .build();
        }
      }
    }
    return getAssignCloudIdentityMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityResponse> getUnassignCloudIdentityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnassignCloudIdentity",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityResponse> getUnassignCloudIdentityMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityResponse> getUnassignCloudIdentityMethod;
    if ((getUnassignCloudIdentityMethod = UserManagementGrpc.getUnassignCloudIdentityMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUnassignCloudIdentityMethod = UserManagementGrpc.getUnassignCloudIdentityMethod) == null) {
          UserManagementGrpc.getUnassignCloudIdentityMethod = getUnassignCloudIdentityMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnassignCloudIdentity"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("UnassignCloudIdentity"))
              .build();
        }
      }
    }
    return getUnassignCloudIdentityMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityResponse> getAssignServicePrincipalCloudIdentityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AssignServicePrincipalCloudIdentity",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityResponse> getAssignServicePrincipalCloudIdentityMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityResponse> getAssignServicePrincipalCloudIdentityMethod;
    if ((getAssignServicePrincipalCloudIdentityMethod = UserManagementGrpc.getAssignServicePrincipalCloudIdentityMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAssignServicePrincipalCloudIdentityMethod = UserManagementGrpc.getAssignServicePrincipalCloudIdentityMethod) == null) {
          UserManagementGrpc.getAssignServicePrincipalCloudIdentityMethod = getAssignServicePrincipalCloudIdentityMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AssignServicePrincipalCloudIdentity"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("AssignServicePrincipalCloudIdentity"))
              .build();
        }
      }
    }
    return getAssignServicePrincipalCloudIdentityMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityResponse> getUnassignServicePrincipalCloudIdentityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnassignServicePrincipalCloudIdentity",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityResponse> getUnassignServicePrincipalCloudIdentityMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityResponse> getUnassignServicePrincipalCloudIdentityMethod;
    if ((getUnassignServicePrincipalCloudIdentityMethod = UserManagementGrpc.getUnassignServicePrincipalCloudIdentityMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUnassignServicePrincipalCloudIdentityMethod = UserManagementGrpc.getUnassignServicePrincipalCloudIdentityMethod) == null) {
          UserManagementGrpc.getUnassignServicePrincipalCloudIdentityMethod = getUnassignServicePrincipalCloudIdentityMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnassignServicePrincipalCloudIdentity"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("UnassignServicePrincipalCloudIdentity"))
              .build();
        }
      }
    }
    return getUnassignServicePrincipalCloudIdentityMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse> getListServicePrincipalCloudIdentitiesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListServicePrincipalCloudIdentities",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse> getListServicePrincipalCloudIdentitiesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse> getListServicePrincipalCloudIdentitiesMethod;
    if ((getListServicePrincipalCloudIdentitiesMethod = UserManagementGrpc.getListServicePrincipalCloudIdentitiesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListServicePrincipalCloudIdentitiesMethod = UserManagementGrpc.getListServicePrincipalCloudIdentitiesMethod) == null) {
          UserManagementGrpc.getListServicePrincipalCloudIdentitiesMethod = getListServicePrincipalCloudIdentitiesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListServicePrincipalCloudIdentities"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListServicePrincipalCloudIdentities"))
              .build();
        }
      }
    }
    return getListServicePrincipalCloudIdentitiesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorResponse> getGetDefaultIdentityProviderConnectorMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetDefaultIdentityProviderConnector",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorResponse> getGetDefaultIdentityProviderConnectorMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorResponse> getGetDefaultIdentityProviderConnectorMethod;
    if ((getGetDefaultIdentityProviderConnectorMethod = UserManagementGrpc.getGetDefaultIdentityProviderConnectorMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetDefaultIdentityProviderConnectorMethod = UserManagementGrpc.getGetDefaultIdentityProviderConnectorMethod) == null) {
          UserManagementGrpc.getGetDefaultIdentityProviderConnectorMethod = getGetDefaultIdentityProviderConnectorMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetDefaultIdentityProviderConnector"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetDefaultIdentityProviderConnector"))
              .build();
        }
      }
    }
    return getGetDefaultIdentityProviderConnectorMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorResponse> getSetDefaultIdentityProviderConnectorMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetDefaultIdentityProviderConnector",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorResponse> getSetDefaultIdentityProviderConnectorMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorResponse> getSetDefaultIdentityProviderConnectorMethod;
    if ((getSetDefaultIdentityProviderConnectorMethod = UserManagementGrpc.getSetDefaultIdentityProviderConnectorMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getSetDefaultIdentityProviderConnectorMethod = UserManagementGrpc.getSetDefaultIdentityProviderConnectorMethod) == null) {
          UserManagementGrpc.getSetDefaultIdentityProviderConnectorMethod = getSetDefaultIdentityProviderConnectorMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetDefaultIdentityProviderConnector"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("SetDefaultIdentityProviderConnector"))
              .build();
        }
      }
    }
    return getSetDefaultIdentityProviderConnectorMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse> getGetUserSyncStateModelMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetUserSyncStateModel",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse> getGetUserSyncStateModelMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse> getGetUserSyncStateModelMethod;
    if ((getGetUserSyncStateModelMethod = UserManagementGrpc.getGetUserSyncStateModelMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetUserSyncStateModelMethod = UserManagementGrpc.getGetUserSyncStateModelMethod) == null) {
          UserManagementGrpc.getGetUserSyncStateModelMethod = getGetUserSyncStateModelMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetUserSyncStateModel"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetUserSyncStateModel"))
              .build();
        }
      }
    }
    return getGetUserSyncStateModelMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsResponse> getListRoleAssignmentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListRoleAssignments",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsResponse> getListRoleAssignmentsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsResponse> getListRoleAssignmentsMethod;
    if ((getListRoleAssignmentsMethod = UserManagementGrpc.getListRoleAssignmentsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListRoleAssignmentsMethod = UserManagementGrpc.getListRoleAssignmentsMethod) == null) {
          UserManagementGrpc.getListRoleAssignmentsMethod = getListRoleAssignmentsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListRoleAssignments"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("ListRoleAssignments"))
              .build();
        }
      }
    }
    return getListRoleAssignmentsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenResponse> getGenerateWorkloadAuthTokenMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GenerateWorkloadAuthToken",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenResponse> getGenerateWorkloadAuthTokenMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenResponse> getGenerateWorkloadAuthTokenMethod;
    if ((getGenerateWorkloadAuthTokenMethod = UserManagementGrpc.getGenerateWorkloadAuthTokenMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGenerateWorkloadAuthTokenMethod = UserManagementGrpc.getGenerateWorkloadAuthTokenMethod) == null) {
          UserManagementGrpc.getGenerateWorkloadAuthTokenMethod = getGenerateWorkloadAuthTokenMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GenerateWorkloadAuthToken"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GenerateWorkloadAuthToken"))
              .build();
        }
      }
    }
    return getGenerateWorkloadAuthTokenMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationResponse> getGetWorkloadAuthConfigurationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetWorkloadAuthConfiguration",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationResponse> getGetWorkloadAuthConfigurationMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationResponse> getGetWorkloadAuthConfigurationMethod;
    if ((getGetWorkloadAuthConfigurationMethod = UserManagementGrpc.getGetWorkloadAuthConfigurationMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetWorkloadAuthConfigurationMethod = UserManagementGrpc.getGetWorkloadAuthConfigurationMethod) == null) {
          UserManagementGrpc.getGetWorkloadAuthConfigurationMethod = getGetWorkloadAuthConfigurationMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetWorkloadAuthConfiguration"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("GetWorkloadAuthConfiguration"))
              .build();
        }
      }
    }
    return getGetWorkloadAuthConfigurationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserResponse> getUpdateUserMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateUser",
      requestType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserRequest.class,
      responseType = com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserResponse> getUpdateUserMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserResponse> getUpdateUserMethod;
    if ((getUpdateUserMethod = UserManagementGrpc.getUpdateUserMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUpdateUserMethod = UserManagementGrpc.getUpdateUserMethod) == null) {
          UserManagementGrpc.getUpdateUserMethod = getUpdateUserMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("UpdateUser"))
              .build();
        }
      }
    }
    return getUpdateUserMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static UserManagementStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UserManagementStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UserManagementStub>() {
        @java.lang.Override
        public UserManagementStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UserManagementStub(channel, callOptions);
        }
      };
    return UserManagementStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static UserManagementBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UserManagementBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UserManagementBlockingStub>() {
        @java.lang.Override
        public UserManagementBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UserManagementBlockingStub(channel, callOptions);
        }
      };
    return UserManagementBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static UserManagementFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UserManagementFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UserManagementFutureStub>() {
        @java.lang.Override
        public UserManagementFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UserManagementFutureStub(channel, callOptions);
        }
      };
    return UserManagementFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static abstract class UserManagementImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Handles an interactive login for a user from a Cloudera identity provider.
     * The user record will be created if necessary.
     * The account record must already exist.
     * </pre>
     */
    public void interactiveLogin(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getInteractiveLoginMethod(), responseObserver);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in who is logging in using a
     * their own IdP. We assume that the account is created. The user will be
     * be created and their group membership synchronized with their Altus state.
     * </pre>
     */
    public void interactiveLogin3rdParty(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getInteractiveLogin3rdPartyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in who is logging in using the CDP control plane local
     * identity provider. We assume that the account had been created.
     * </pre>
     */
    public void interactiveLoginLocal(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getInteractiveLoginLocalMethod(), responseObserver);
    }

    /**
     * <pre>
     * Deletes the account from Altus tests only.
     * </pre>
     */
    public void deleteAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteAccountMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete an actor from Altus.
     * </pre>
     */
    public void deleteActor(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteActorMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete trial user from Altus for tests only.
     * </pre>
     */
    public void deleteTrialUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteTrialUserMethod(), responseObserver);
    }

    /**
     * <pre>
     * Gets all the information associated with an access key needed to verify a
     * request signature produced that key.
     * </pre>
     */
    public void getAccessKeyVerificationData(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAccessKeyVerificationDataMethod(), responseObserver);
    }

    /**
     * <pre>
     * Verifies an interactive user session key. If the session key is expired an
     * exception is thrown. If the session key is found and is valid,
     * information about the user and their account is returned.
     * </pre>
     */
    public void verifyInteractiveUserSessionToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getVerifyInteractiveUserSessionTokenMethod(), responseObserver);
    }

    /**
     * <pre>
     * Verifies an access token. If the access token is invalid (not found, expired, doesn't match),
     * an exception is thrown. If the access token is valid, information about the actor and their
     * account is returned.
     * </pre>
     */
    public void verifyAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getVerifyAccessTokenMethod(), responseObserver);
    }

    /**
     * <pre>
     * Authenticate an actor. This method currently supports session tokens and
     * access key authentication.
     * </pre>
     */
    public void authenticate(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAuthenticateMethod(), responseObserver);
    }

    /**
     * <pre>
     * Handles access key usage, marking the last time it was used and the last
     * service on which it was used.
     * </pre>
     */
    public void accessKeyUsage(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAccessKeyUsageMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create user. Can only be used to create a user associated with a customer
     * identity provider connector.
     * </pre>
     */
    public void createUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateUserMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get user.
     * </pre>
     */
    public void getUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetUserMethod(), responseObserver);
    }

    /**
     * <pre>
     * List users.
     * </pre>
     */
    public void listUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListUsersMethod(), responseObserver);
    }

    /**
     * <pre>
     * Find users by Email.
     * </pre>
     */
    public void findUsersByEmail(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFindUsersByEmailMethod(), responseObserver);
    }

    /**
     * <pre>
     * Find users.
     * </pre>
     */
    public void findUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFindUsersMethod(), responseObserver);
    }

    /**
     * <pre>
     * Creates a new access key.
     * </pre>
     */
    public void createAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateAccessKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Updates an access key.
     * </pre>
     */
    public void updateAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateAccessKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Deletes an access key.
     * </pre>
     */
    public void deleteAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteAccessKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get access key.
     * </pre>
     */
    public void getAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAccessKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * List access keys.
     * </pre>
     */
    public void listAccessKeys(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListAccessKeysMethod(), responseObserver);
    }

    /**
     * <pre>
     * Creates a new access token.
     * </pre>
     */
    public void createAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateAccessTokenMethod(), responseObserver);
    }

    /**
     * <pre>
     * Deletes an access token.
     * </pre>
     */
    public void deleteAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteAccessTokenMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get an access token.
     * </pre>
     */
    public void getAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAccessTokenMethod(), responseObserver);
    }

    /**
     * <pre>
     * List access tokens.
     * </pre>
     */
    public void listAccessTokens(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListAccessTokensMethod(), responseObserver);
    }

    /**
     * <pre>
     * Creates a new SCIM access token.
     * </pre>
     */
    public void createScimAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateScimAccessTokenMethod(), responseObserver);
    }

    /**
     * <pre>
     * Deletes a SCIM access token.
     * </pre>
     */
    public void deleteScimAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteScimAccessTokenMethod(), responseObserver);
    }

    /**
     * <pre>
     * List SCIM access tokens.
     * </pre>
     */
    public void listScimAccessTokens(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListScimAccessTokensMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the service version.
     * </pre>
     */
    public void getVersion(com.cloudera.thunderhead.service.common.version.Version.VersionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetVersionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get account.
     * </pre>
     */
    public void getAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAccountMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get account.
     * </pre>
     */
    public void listAccounts(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListAccountsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the rights for an actor.
     * </pre>
     */
    public void getRights(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetRightsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Checks if an actor has the input rights on the input resources.
     * </pre>
     */
    public void checkRights(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckRightsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create a regular account.
     * </pre>
     */
    public void createAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateAccountMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create a trial account.
     * </pre>
     */
    public void createTrialAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateTrialAccountMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create an email based account.
     * </pre>
     */
    public void createC1CAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateC1CAccountMethod(), responseObserver);
    }

    /**
     * <pre>
     * An endpoint called when a user verifies their email by clicking the link we sent
     * them.
     * </pre>
     */
    public void verifyC1CEmail(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getVerifyC1CEmailMethod(), responseObserver);
    }

    /**
     * <pre>
     * Grant Entitlement to an Account
     * </pre>
     */
    public void grantEntitlement(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGrantEntitlementMethod(), responseObserver);
    }

    /**
     * <pre>
     * Revoke Entitlement from an Account
     * </pre>
     */
    public void revokeEntitlement(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRevokeEntitlementMethod(), responseObserver);
    }

    /**
     * <pre>
     * Ensure default entitlements are granted to an Account
     * </pre>
     */
    public void ensureDefaultEntitlementsGranted(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getEnsureDefaultEntitlementsGrantedMethod(), responseObserver);
    }

    /**
     * <pre>
     * Assign a role to an assignee
     * </pre>
     */
    public void assignRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAssignRoleMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unassign a role from an assignee
     * </pre>
     */
    public void unassignRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnassignRoleMethod(), responseObserver);
    }

    /**
     * <pre>
     * List the assigned roles for an assignee:
     * </pre>
     */
    public void listAssignedRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListAssignedRolesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Assign a resource role to an assignee
     * </pre>
     */
    public void assignResourceRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAssignResourceRoleMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unassign a resource role from an assignee
     * </pre>
     */
    public void unassignResourceRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnassignResourceRoleMethod(), responseObserver);
    }

    /**
     * <pre>
     * List the assigned resource roles for an assignee:
     * </pre>
     */
    public void listAssignedResourceRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListAssignedResourceRolesMethod(), responseObserver);
    }

    /**
     * <pre>
     * List roles.
     * </pre>
     */
    public void listRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListRolesMethod(), responseObserver);
    }

    /**
     * <pre>
     * List resource roles.
     * </pre>
     */
    public void listResourceRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListResourceRolesMethod(), responseObserver);
    }

    /**
     * <pre>
     * List resource assignees.
     * </pre>
     */
    public void listResourceAssignees(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListResourceAssigneesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Update Cloudera Manager License Key
     * </pre>
     */
    public void updateClouderaManagerLicenseKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateClouderaManagerLicenseKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Initiates a support case creation pipeline.
     * </pre>
     */
    public void initiateSupportCase(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getInitiateSupportCaseMethod(), responseObserver);
    }

    /**
     * <pre>
     * Notify that a resource was deleted. All resource role assignments
     * associated with this resource will be deleted.
     * </pre>
     */
    public void notifyResourceDeleted(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getNotifyResourceDeletedMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create a machine user
     * </pre>
     */
    public void createMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateMachineUserMethod(), responseObserver);
    }

    /**
     * <pre>
     * list machine users
     * </pre>
     */
    public void listMachineUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListMachineUsersMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete a machine user
     * </pre>
     */
    public void deleteMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteMachineUserMethod(), responseObserver);
    }

    /**
     */
    public void listResourceRoleAssignments(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListResourceRoleAssignmentsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Sets the account messages.
     * </pre>
     */
    public void setAccountMessages(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetAccountMessagesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Terms acceptance
     * </pre>
     */
    public void acceptTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAcceptTermsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Clearing accepted terms. This will clear the accepted terms with the
     * same version as the current Terms found in the TermsProvider.
     * </pre>
     */
    public void clearAcceptedTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getClearAcceptedTermsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Terms description
     * </pre>
     */
    public void describeTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeTermsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Terms listing
     * </pre>
     */
    public void listTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListTermsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Entitlements listing
     * </pre>
     */
    public void listEntitlements(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListEntitlementsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Set terms acceptance expiry
     * </pre>
     */
    public void setTermsAcceptanceExpiry(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetTermsAcceptanceExpiryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Confirm whether Altus account and Azure Subscription Id
     * </pre>
     */
    public void confirmAzureSubscriptionVerified(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getConfirmAzureSubscriptionVerifiedMethod(), responseObserver);
    }

    /**
     * <pre>
     * Insert Azure Subscriptions
     * </pre>
     */
    public void insertAzureSubscription(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getInsertAzureSubscriptionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create group
     * </pre>
     */
    public void createGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateGroupMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete group
     * </pre>
     */
    public void deleteGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteGroupMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get groups
     * </pre>
     */
    public void getGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetGroupMethod(), responseObserver);
    }

    /**
     * <pre>
     * List groups
     * </pre>
     */
    public void listGroups(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListGroupsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Update group
     * </pre>
     */
    public void updateGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateGroupMethod(), responseObserver);
    }

    /**
     * <pre>
     * Add member to group
     * </pre>
     */
    public void addMemberToGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAddMemberToGroupMethod(), responseObserver);
    }

    /**
     * <pre>
     * Remove member from group
     * </pre>
     */
    public void removeMemberFromGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRemoveMemberFromGroupMethod(), responseObserver);
    }

    /**
     * <pre>
     * List group members
     * </pre>
     */
    public void listGroupMembers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListGroupMembersMethod(), responseObserver);
    }

    /**
     * <pre>
     * List of groupCRNs corresponding to the member
     * </pre>
     */
    public void listGroupsForMember(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListGroupsForMemberMethod(), responseObserver);
    }

    /**
     * <pre>
     * List workload administration group names corresponding to the member
     * </pre>
     */
    public void listWorkloadAdministrationGroupsForMember(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListWorkloadAdministrationGroupsForMemberMethod(), responseObserver);
    }

    /**
     * <pre>
     * Creates a new cluster ssh private key.
     * </pre>
     */
    public void createClusterSshPrivateKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateClusterSshPrivateKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get cluster ssh private key.
     * </pre>
     */
    public void getClusterSshPrivateKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetClusterSshPrivateKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the authorization information about an assignee.
     * </pre>
     */
    public void getAssigneeAuthorizationInformation(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAssigneeAuthorizationInformationMethod(), responseObserver);
    }

    /**
     * <pre>
     * Creates the identity provider connector
     * </pre>
     */
    public void createIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateIdentityProviderConnectorMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists identity provider connectors
     * </pre>
     */
    public void listIdentityProviderConnectors(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListIdentityProviderConnectorsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Deletes identity provider connector
     * </pre>
     */
    public void deleteIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteIdentityProviderConnectorMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describes an identity provider connector
     * </pre>
     */
    public void describeIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeIdentityProviderConnectorMethod(), responseObserver);
    }

    /**
     * <pre>
     * Update an identity provider connector
     * </pre>
     */
    public void updateIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateIdentityProviderConnectorMethod(), responseObserver);
    }

    /**
     * <pre>
     * Set whether login using Cloudera SSO is enabled.
     * </pre>
     */
    public void setClouderaSSOLoginEnabled(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetClouderaSSOLoginEnabledMethod(), responseObserver);
    }

    /**
     * <pre>
     * Retrieves the control plane IdP metadata file for a workload
     * SSO service.
     * </pre>
     */
    public void getIdPMetadataForWorkloadSSO(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetIdPMetadataForWorkloadSSOMethod(), responseObserver);
    }

    /**
     * <pre>
     * Process a workload SSO AuthNRequest. If the user is already authenticated
     * an appropriate authn response will be generated to the workload SSO. If not,
     * an appropriate authn request will be generated to either cloudera-sso or
     * to one of the customer defined identity providers.
     * </pre>
     */
    public void processWorkloadSSOAuthnReq(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getProcessWorkloadSSOAuthnReqMethod(), responseObserver);
    }

    /**
     * <pre>
     * Generate a SSO AuthNRequest for control plane SP-initiated login.
     * </pre>
     */
    public void generateControlPlaneSSOAuthnReq(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGenerateControlPlaneSSOAuthnReqMethod(), responseObserver);
    }

    /**
     * <pre>
     * Set the workload subdomain for an account if no such workload domain has
     * been set for a different account before.
     * </pre>
     */
    public void setWorkloadSubdomain(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetWorkloadSubdomainMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create a machine-user, assign the resource roles and roles to it, create
     * an access key for it, and return it. This is exposed as a convenience method
     * for applications. Callers must be internal actors. The call is idempotent
     * and safe to be called multiple times. Machine users created through this
     * interface should be deleted by called DeleteWorkloadMachineUser.
     * </pre>
     */
    public void createWorkloadMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateWorkloadMachineUserMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete the workload machine user, all the role and resource role assignments,
     * as well as any access keys. This is a convenience method for application who
     * created a machine user using CreateWorkloadMachineUser.
     * </pre>
     */
    public void deleteWorkloadMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteWorkloadMachineUserMethod(), responseObserver);
    }

    /**
     * <pre>
     * Returns the the workload administration group name for the
     * (account, right, resource) tuple. Will throw NOT_FOUND exception if no name
     * for the workload administration group has been set yet.
     * </pre>
     */
    public void getWorkloadAdministrationGroupName(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetWorkloadAdministrationGroupNameMethod(), responseObserver);
    }

    /**
     * <pre>
     * Sets the workload administration group name for the (account, right, resource)
     * tuple. If the name was already set for the workload administration group
     * this is a no-op and the name generated for the workload administration group
     * will be returned.
     * </pre>
     */
    public void setWorkloadAdministrationGroupName(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetWorkloadAdministrationGroupNameMethod(), responseObserver);
    }

    /**
     * <pre>
     * Deletes the workload administration group name for the (account, right, resource)
     * tuple. Throws a NOT_FOUND exception if no such workload administration group
     * can be found.
     * </pre>
     */
    public void deleteWorkloadAdministrationGroupName(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteWorkloadAdministrationGroupNameMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists the workload administration groups in an account.
     * </pre>
     */
    public void listWorkloadAdministrationGroups(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListWorkloadAdministrationGroupsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Sets the actor workloads credentials. This will replace and overwrite any
     * existing actor credentials.
     * </pre>
     */
    public void setActorWorkloadCredentials(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetActorWorkloadCredentialsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Validates the actor workloads credentials based on the password policy for the account.
     * </pre>
     */
    public void validateActorWorkloadCredentials(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getValidateActorWorkloadCredentialsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Retrieves the actor workload credentials.
     * </pre>
     */
    public void getActorWorkloadCredentials(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetActorWorkloadCredentialsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Returns a unique ID for the following events:
     * * Role assignment events.
     * * Resource role assignment events.
     * * Group membership changes events.
     * * Actor deletion events.
     * * Actor workload credentials change events.
     * The IDs are guaranteed to be unique and can be used to track the above
     * changes in a specific account. If no such event has happened in the account
     * since tracking started an empty string will be returned instead of an ID.
     * </pre>
     */
    public void getEventGenerationIds(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetEventGenerationIdsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Adds an SSH public key for an actor.
     * </pre>
     */
    public void addActorSshPublicKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAddActorSshPublicKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists the SSH public keys for an actor.
     * </pre>
     */
    public void listActorSshPublicKeys(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListActorSshPublicKeysMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describes an SSH public key.
     * </pre>
     */
    public void describeActorSshPublicKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDescribeActorSshPublicKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Deletes an SSH public key for an actor.
     * </pre>
     */
    public void deleteActorSshPublicKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteActorSshPublicKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Sets the workload password policy for an account.
     * </pre>
     */
    public void setWorkloadPasswordPolicy(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetWorkloadPasswordPolicyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unsets the workload password policy for an account.
     * </pre>
     */
    public void unsetWorkloadPasswordPolicy(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnsetWorkloadPasswordPolicyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Sets the authentication policy for an account.
     * </pre>
     */
    public void setAuthenticationPolicy(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetAuthenticationPolicyMethod(), responseObserver);
    }

    /**
     * <pre>
     * Assign a cloud identity to an actor or group.
     * </pre>
     */
    public void assignCloudIdentity(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAssignCloudIdentityMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unassign a cloud identity from an actor or group.
     * </pre>
     */
    public void unassignCloudIdentity(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnassignCloudIdentityMethod(), responseObserver);
    }

    /**
     * <pre>
     * Assign a cloud identity to a service principal.
     * </pre>
     */
    public void assignServicePrincipalCloudIdentity(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAssignServicePrincipalCloudIdentityMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unassign a cloud identity from a service principal.
     * </pre>
     */
    public void unassignServicePrincipalCloudIdentity(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnassignServicePrincipalCloudIdentityMethod(), responseObserver);
    }

    /**
     * <pre>
     * List cloud identity mappings for service principals.
     * </pre>
     */
    public void listServicePrincipalCloudIdentities(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListServicePrincipalCloudIdentitiesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Retrieves the CRN of the default identity provider connector used for CDP initiated logins.
     * </pre>
     */
    public void getDefaultIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetDefaultIdentityProviderConnectorMethod(), responseObserver);
    }

    /**
     * <pre>
     * Sets the CRN of the default identity provider connector used for CDP initiated logins.
     * </pre>
     */
    public void setDefaultIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetDefaultIdentityProviderConnectorMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get user sync state model, including actors, groups, workload administration groups, etc
     * </pre>
     */
    public void getUserSyncStateModel(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetUserSyncStateModelMethod(), responseObserver);
    }

    /**
     * <pre>
     * List all role assignments in an account.
     * </pre>
     */
    public void listRoleAssignments(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListRoleAssignmentsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Generate authentication token for workload API.
     * </pre>
     */
    public void generateWorkloadAuthToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGenerateWorkloadAuthTokenMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get authentication configuration for workload API.
     * </pre>
     */
    public void getWorkloadAuthConfiguration(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetWorkloadAuthConfigurationMethod(), responseObserver);
    }

    /**
     * <pre>
     * Update user.
     * </pre>
     */
    public void updateUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateUserMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getInteractiveLoginMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse>(
                  this, METHODID_INTERACTIVE_LOGIN)))
          .addMethod(
            getInteractiveLogin3rdPartyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse>(
                  this, METHODID_INTERACTIVE_LOGIN3RD_PARTY)))
          .addMethod(
            getInteractiveLoginLocalMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalResponse>(
                  this, METHODID_INTERACTIVE_LOGIN_LOCAL)))
          .addMethod(
            getDeleteAccountMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse>(
                  this, METHODID_DELETE_ACCOUNT)))
          .addMethod(
            getDeleteActorMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse>(
                  this, METHODID_DELETE_ACTOR)))
          .addMethod(
            getDeleteTrialUserMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse>(
                  this, METHODID_DELETE_TRIAL_USER)))
          .addMethod(
            getGetAccessKeyVerificationDataMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse>(
                  this, METHODID_GET_ACCESS_KEY_VERIFICATION_DATA)))
          .addMethod(
            getVerifyInteractiveUserSessionTokenMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse>(
                  this, METHODID_VERIFY_INTERACTIVE_USER_SESSION_TOKEN)))
          .addMethod(
            getVerifyAccessTokenMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenResponse>(
                  this, METHODID_VERIFY_ACCESS_TOKEN)))
          .addMethod(
            getAuthenticateMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse>(
                  this, METHODID_AUTHENTICATE)))
          .addMethod(
            getAccessKeyUsageMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse>(
                  this, METHODID_ACCESS_KEY_USAGE)))
          .addMethod(
            getCreateUserMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserResponse>(
                  this, METHODID_CREATE_USER)))
          .addMethod(
            getGetUserMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse>(
                  this, METHODID_GET_USER)))
          .addMethod(
            getListUsersMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse>(
                  this, METHODID_LIST_USERS)))
          .addMethod(
            getFindUsersByEmailMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse>(
                  this, METHODID_FIND_USERS_BY_EMAIL)))
          .addMethod(
            getFindUsersMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersResponse>(
                  this, METHODID_FIND_USERS)))
          .addMethod(
            getCreateAccessKeyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse>(
                  this, METHODID_CREATE_ACCESS_KEY)))
          .addMethod(
            getUpdateAccessKeyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse>(
                  this, METHODID_UPDATE_ACCESS_KEY)))
          .addMethod(
            getDeleteAccessKeyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse>(
                  this, METHODID_DELETE_ACCESS_KEY)))
          .addMethod(
            getGetAccessKeyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse>(
                  this, METHODID_GET_ACCESS_KEY)))
          .addMethod(
            getListAccessKeysMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse>(
                  this, METHODID_LIST_ACCESS_KEYS)))
          .addMethod(
            getCreateAccessTokenMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenResponse>(
                  this, METHODID_CREATE_ACCESS_TOKEN)))
          .addMethod(
            getDeleteAccessTokenMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenResponse>(
                  this, METHODID_DELETE_ACCESS_TOKEN)))
          .addMethod(
            getGetAccessTokenMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenResponse>(
                  this, METHODID_GET_ACCESS_TOKEN)))
          .addMethod(
            getListAccessTokensMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensResponse>(
                  this, METHODID_LIST_ACCESS_TOKENS)))
          .addMethod(
            getCreateScimAccessTokenMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenResponse>(
                  this, METHODID_CREATE_SCIM_ACCESS_TOKEN)))
          .addMethod(
            getDeleteScimAccessTokenMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenResponse>(
                  this, METHODID_DELETE_SCIM_ACCESS_TOKEN)))
          .addMethod(
            getListScimAccessTokensMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensResponse>(
                  this, METHODID_LIST_SCIM_ACCESS_TOKENS)))
          .addMethod(
            getGetVersionMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
                com.cloudera.thunderhead.service.common.version.Version.VersionResponse>(
                  this, METHODID_GET_VERSION)))
          .addMethod(
            getGetAccountMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse>(
                  this, METHODID_GET_ACCOUNT)))
          .addMethod(
            getListAccountsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse>(
                  this, METHODID_LIST_ACCOUNTS)))
          .addMethod(
            getGetRightsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse>(
                  this, METHODID_GET_RIGHTS)))
          .addMethod(
            getCheckRightsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse>(
                  this, METHODID_CHECK_RIGHTS)))
          .addMethod(
            getCreateAccountMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse>(
                  this, METHODID_CREATE_ACCOUNT)))
          .addMethod(
            getCreateTrialAccountMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountResponse>(
                  this, METHODID_CREATE_TRIAL_ACCOUNT)))
          .addMethod(
            getCreateC1CAccountMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountResponse>(
                  this, METHODID_CREATE_C1CACCOUNT)))
          .addMethod(
            getVerifyC1CEmailMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailResponse>(
                  this, METHODID_VERIFY_C1CEMAIL)))
          .addMethod(
            getGrantEntitlementMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse>(
                  this, METHODID_GRANT_ENTITLEMENT)))
          .addMethod(
            getRevokeEntitlementMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse>(
                  this, METHODID_REVOKE_ENTITLEMENT)))
          .addMethod(
            getEnsureDefaultEntitlementsGrantedMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedResponse>(
                  this, METHODID_ENSURE_DEFAULT_ENTITLEMENTS_GRANTED)))
          .addMethod(
            getAssignRoleMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse>(
                  this, METHODID_ASSIGN_ROLE)))
          .addMethod(
            getUnassignRoleMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse>(
                  this, METHODID_UNASSIGN_ROLE)))
          .addMethod(
            getListAssignedRolesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse>(
                  this, METHODID_LIST_ASSIGNED_ROLES)))
          .addMethod(
            getAssignResourceRoleMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse>(
                  this, METHODID_ASSIGN_RESOURCE_ROLE)))
          .addMethod(
            getUnassignResourceRoleMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse>(
                  this, METHODID_UNASSIGN_RESOURCE_ROLE)))
          .addMethod(
            getListAssignedResourceRolesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse>(
                  this, METHODID_LIST_ASSIGNED_RESOURCE_ROLES)))
          .addMethod(
            getListRolesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse>(
                  this, METHODID_LIST_ROLES)))
          .addMethod(
            getListResourceRolesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse>(
                  this, METHODID_LIST_RESOURCE_ROLES)))
          .addMethod(
            getListResourceAssigneesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse>(
                  this, METHODID_LIST_RESOURCE_ASSIGNEES)))
          .addMethod(
            getUpdateClouderaManagerLicenseKeyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse>(
                  this, METHODID_UPDATE_CLOUDERA_MANAGER_LICENSE_KEY)))
          .addMethod(
            getInitiateSupportCaseMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse>(
                  this, METHODID_INITIATE_SUPPORT_CASE)))
          .addMethod(
            getNotifyResourceDeletedMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse>(
                  this, METHODID_NOTIFY_RESOURCE_DELETED)))
          .addMethod(
            getCreateMachineUserMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse>(
                  this, METHODID_CREATE_MACHINE_USER)))
          .addMethod(
            getListMachineUsersMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse>(
                  this, METHODID_LIST_MACHINE_USERS)))
          .addMethod(
            getDeleteMachineUserMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse>(
                  this, METHODID_DELETE_MACHINE_USER)))
          .addMethod(
            getListResourceRoleAssignmentsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse>(
                  this, METHODID_LIST_RESOURCE_ROLE_ASSIGNMENTS)))
          .addMethod(
            getSetAccountMessagesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse>(
                  this, METHODID_SET_ACCOUNT_MESSAGES)))
          .addMethod(
            getAcceptTermsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse>(
                  this, METHODID_ACCEPT_TERMS)))
          .addMethod(
            getClearAcceptedTermsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse>(
                  this, METHODID_CLEAR_ACCEPTED_TERMS)))
          .addMethod(
            getDescribeTermsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse>(
                  this, METHODID_DESCRIBE_TERMS)))
          .addMethod(
            getListTermsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse>(
                  this, METHODID_LIST_TERMS)))
          .addMethod(
            getListEntitlementsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse>(
                  this, METHODID_LIST_ENTITLEMENTS)))
          .addMethod(
            getSetTermsAcceptanceExpiryMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse>(
                  this, METHODID_SET_TERMS_ACCEPTANCE_EXPIRY)))
          .addMethod(
            getConfirmAzureSubscriptionVerifiedMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse>(
                  this, METHODID_CONFIRM_AZURE_SUBSCRIPTION_VERIFIED)))
          .addMethod(
            getInsertAzureSubscriptionMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse>(
                  this, METHODID_INSERT_AZURE_SUBSCRIPTION)))
          .addMethod(
            getCreateGroupMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse>(
                  this, METHODID_CREATE_GROUP)))
          .addMethod(
            getDeleteGroupMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse>(
                  this, METHODID_DELETE_GROUP)))
          .addMethod(
            getGetGroupMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupResponse>(
                  this, METHODID_GET_GROUP)))
          .addMethod(
            getListGroupsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse>(
                  this, METHODID_LIST_GROUPS)))
          .addMethod(
            getUpdateGroupMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupResponse>(
                  this, METHODID_UPDATE_GROUP)))
          .addMethod(
            getAddMemberToGroupMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse>(
                  this, METHODID_ADD_MEMBER_TO_GROUP)))
          .addMethod(
            getRemoveMemberFromGroupMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse>(
                  this, METHODID_REMOVE_MEMBER_FROM_GROUP)))
          .addMethod(
            getListGroupMembersMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse>(
                  this, METHODID_LIST_GROUP_MEMBERS)))
          .addMethod(
            getListGroupsForMemberMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse>(
                  this, METHODID_LIST_GROUPS_FOR_MEMBER)))
          .addMethod(
            getListWorkloadAdministrationGroupsForMemberMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse>(
                  this, METHODID_LIST_WORKLOAD_ADMINISTRATION_GROUPS_FOR_MEMBER)))
          .addMethod(
            getCreateClusterSshPrivateKeyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse>(
                  this, METHODID_CREATE_CLUSTER_SSH_PRIVATE_KEY)))
          .addMethod(
            getGetClusterSshPrivateKeyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse>(
                  this, METHODID_GET_CLUSTER_SSH_PRIVATE_KEY)))
          .addMethod(
            getGetAssigneeAuthorizationInformationMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse>(
                  this, METHODID_GET_ASSIGNEE_AUTHORIZATION_INFORMATION)))
          .addMethod(
            getCreateIdentityProviderConnectorMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse>(
                  this, METHODID_CREATE_IDENTITY_PROVIDER_CONNECTOR)))
          .addMethod(
            getListIdentityProviderConnectorsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse>(
                  this, METHODID_LIST_IDENTITY_PROVIDER_CONNECTORS)))
          .addMethod(
            getDeleteIdentityProviderConnectorMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse>(
                  this, METHODID_DELETE_IDENTITY_PROVIDER_CONNECTOR)))
          .addMethod(
            getDescribeIdentityProviderConnectorMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse>(
                  this, METHODID_DESCRIBE_IDENTITY_PROVIDER_CONNECTOR)))
          .addMethod(
            getUpdateIdentityProviderConnectorMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse>(
                  this, METHODID_UPDATE_IDENTITY_PROVIDER_CONNECTOR)))
          .addMethod(
            getSetClouderaSSOLoginEnabledMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse>(
                  this, METHODID_SET_CLOUDERA_SSOLOGIN_ENABLED)))
          .addMethod(
            getGetIdPMetadataForWorkloadSSOMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse>(
                  this, METHODID_GET_ID_PMETADATA_FOR_WORKLOAD_SSO)))
          .addMethod(
            getProcessWorkloadSSOAuthnReqMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqResponse>(
                  this, METHODID_PROCESS_WORKLOAD_SSOAUTHN_REQ)))
          .addMethod(
            getGenerateControlPlaneSSOAuthnReqMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqResponse>(
                  this, METHODID_GENERATE_CONTROL_PLANE_SSOAUTHN_REQ)))
          .addMethod(
            getSetWorkloadSubdomainMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainResponse>(
                  this, METHODID_SET_WORKLOAD_SUBDOMAIN)))
          .addMethod(
            getCreateWorkloadMachineUserMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse>(
                  this, METHODID_CREATE_WORKLOAD_MACHINE_USER)))
          .addMethod(
            getDeleteWorkloadMachineUserMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserResponse>(
                  this, METHODID_DELETE_WORKLOAD_MACHINE_USER)))
          .addMethod(
            getGetWorkloadAdministrationGroupNameMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse>(
                  this, METHODID_GET_WORKLOAD_ADMINISTRATION_GROUP_NAME)))
          .addMethod(
            getSetWorkloadAdministrationGroupNameMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse>(
                  this, METHODID_SET_WORKLOAD_ADMINISTRATION_GROUP_NAME)))
          .addMethod(
            getDeleteWorkloadAdministrationGroupNameMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse>(
                  this, METHODID_DELETE_WORKLOAD_ADMINISTRATION_GROUP_NAME)))
          .addMethod(
            getListWorkloadAdministrationGroupsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse>(
                  this, METHODID_LIST_WORKLOAD_ADMINISTRATION_GROUPS)))
          .addMethod(
            getSetActorWorkloadCredentialsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsResponse>(
                  this, METHODID_SET_ACTOR_WORKLOAD_CREDENTIALS)))
          .addMethod(
            getValidateActorWorkloadCredentialsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsResponse>(
                  this, METHODID_VALIDATE_ACTOR_WORKLOAD_CREDENTIALS)))
          .addMethod(
            getGetActorWorkloadCredentialsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse>(
                  this, METHODID_GET_ACTOR_WORKLOAD_CREDENTIALS)))
          .addMethod(
            getGetEventGenerationIdsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse>(
                  this, METHODID_GET_EVENT_GENERATION_IDS)))
          .addMethod(
            getAddActorSshPublicKeyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyResponse>(
                  this, METHODID_ADD_ACTOR_SSH_PUBLIC_KEY)))
          .addMethod(
            getListActorSshPublicKeysMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysResponse>(
                  this, METHODID_LIST_ACTOR_SSH_PUBLIC_KEYS)))
          .addMethod(
            getDescribeActorSshPublicKeyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyResponse>(
                  this, METHODID_DESCRIBE_ACTOR_SSH_PUBLIC_KEY)))
          .addMethod(
            getDeleteActorSshPublicKeyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyResponse>(
                  this, METHODID_DELETE_ACTOR_SSH_PUBLIC_KEY)))
          .addMethod(
            getSetWorkloadPasswordPolicyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyResponse>(
                  this, METHODID_SET_WORKLOAD_PASSWORD_POLICY)))
          .addMethod(
            getUnsetWorkloadPasswordPolicyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyResponse>(
                  this, METHODID_UNSET_WORKLOAD_PASSWORD_POLICY)))
          .addMethod(
            getSetAuthenticationPolicyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyResponse>(
                  this, METHODID_SET_AUTHENTICATION_POLICY)))
          .addMethod(
            getAssignCloudIdentityMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityResponse>(
                  this, METHODID_ASSIGN_CLOUD_IDENTITY)))
          .addMethod(
            getUnassignCloudIdentityMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityResponse>(
                  this, METHODID_UNASSIGN_CLOUD_IDENTITY)))
          .addMethod(
            getAssignServicePrincipalCloudIdentityMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityResponse>(
                  this, METHODID_ASSIGN_SERVICE_PRINCIPAL_CLOUD_IDENTITY)))
          .addMethod(
            getUnassignServicePrincipalCloudIdentityMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityResponse>(
                  this, METHODID_UNASSIGN_SERVICE_PRINCIPAL_CLOUD_IDENTITY)))
          .addMethod(
            getListServicePrincipalCloudIdentitiesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse>(
                  this, METHODID_LIST_SERVICE_PRINCIPAL_CLOUD_IDENTITIES)))
          .addMethod(
            getGetDefaultIdentityProviderConnectorMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorResponse>(
                  this, METHODID_GET_DEFAULT_IDENTITY_PROVIDER_CONNECTOR)))
          .addMethod(
            getSetDefaultIdentityProviderConnectorMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorResponse>(
                  this, METHODID_SET_DEFAULT_IDENTITY_PROVIDER_CONNECTOR)))
          .addMethod(
            getGetUserSyncStateModelMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse>(
                  this, METHODID_GET_USER_SYNC_STATE_MODEL)))
          .addMethod(
            getListRoleAssignmentsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsResponse>(
                  this, METHODID_LIST_ROLE_ASSIGNMENTS)))
          .addMethod(
            getGenerateWorkloadAuthTokenMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenResponse>(
                  this, METHODID_GENERATE_WORKLOAD_AUTH_TOKEN)))
          .addMethod(
            getGetWorkloadAuthConfigurationMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationResponse>(
                  this, METHODID_GET_WORKLOAD_AUTH_CONFIGURATION)))
          .addMethod(
            getUpdateUserMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserResponse>(
                  this, METHODID_UPDATE_USER)))
          .build();
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class UserManagementStub extends io.grpc.stub.AbstractAsyncStub<UserManagementStub> {
    private UserManagementStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UserManagementStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UserManagementStub(channel, callOptions);
    }

    /**
     * <pre>
     * Handles an interactive login for a user from a Cloudera identity provider.
     * The user record will be created if necessary.
     * The account record must already exist.
     * </pre>
     */
    public void interactiveLogin(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getInteractiveLoginMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in who is logging in using a
     * their own IdP. We assume that the account is created. The user will be
     * be created and their group membership synchronized with their Altus state.
     * </pre>
     */
    public void interactiveLogin3rdParty(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getInteractiveLogin3rdPartyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in who is logging in using the CDP control plane local
     * identity provider. We assume that the account had been created.
     * </pre>
     */
    public void interactiveLoginLocal(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getInteractiveLoginLocalMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes the account from Altus tests only.
     * </pre>
     */
    public void deleteAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete an actor from Altus.
     * </pre>
     */
    public void deleteActor(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteActorMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete trial user from Altus for tests only.
     * </pre>
     */
    public void deleteTrialUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteTrialUserMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Gets all the information associated with an access key needed to verify a
     * request signature produced that key.
     * </pre>
     */
    public void getAccessKeyVerificationData(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAccessKeyVerificationDataMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Verifies an interactive user session key. If the session key is expired an
     * exception is thrown. If the session key is found and is valid,
     * information about the user and their account is returned.
     * </pre>
     */
    public void verifyInteractiveUserSessionToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getVerifyInteractiveUserSessionTokenMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Verifies an access token. If the access token is invalid (not found, expired, doesn't match),
     * an exception is thrown. If the access token is valid, information about the actor and their
     * account is returned.
     * </pre>
     */
    public void verifyAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getVerifyAccessTokenMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Authenticate an actor. This method currently supports session tokens and
     * access key authentication.
     * </pre>
     */
    public void authenticate(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAuthenticateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Handles access key usage, marking the last time it was used and the last
     * service on which it was used.
     * </pre>
     */
    public void accessKeyUsage(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAccessKeyUsageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create user. Can only be used to create a user associated with a customer
     * identity provider connector.
     * </pre>
     */
    public void createUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateUserMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get user.
     * </pre>
     */
    public void getUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetUserMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List users.
     * </pre>
     */
    public void listUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListUsersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Find users by Email.
     * </pre>
     */
    public void findUsersByEmail(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFindUsersByEmailMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Find users.
     * </pre>
     */
    public void findUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFindUsersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates a new access key.
     * </pre>
     */
    public void createAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateAccessKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Updates an access key.
     * </pre>
     */
    public void updateAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateAccessKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes an access key.
     * </pre>
     */
    public void deleteAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteAccessKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get access key.
     * </pre>
     */
    public void getAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAccessKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List access keys.
     * </pre>
     */
    public void listAccessKeys(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListAccessKeysMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates a new access token.
     * </pre>
     */
    public void createAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateAccessTokenMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes an access token.
     * </pre>
     */
    public void deleteAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteAccessTokenMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get an access token.
     * </pre>
     */
    public void getAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAccessTokenMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List access tokens.
     * </pre>
     */
    public void listAccessTokens(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListAccessTokensMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates a new SCIM access token.
     * </pre>
     */
    public void createScimAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateScimAccessTokenMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes a SCIM access token.
     * </pre>
     */
    public void deleteScimAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteScimAccessTokenMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List SCIM access tokens.
     * </pre>
     */
    public void listScimAccessTokens(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListScimAccessTokensMethod(), getCallOptions()), request, responseObserver);
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
     * Get account.
     * </pre>
     */
    public void getAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get account.
     * </pre>
     */
    public void listAccounts(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListAccountsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the rights for an actor.
     * </pre>
     */
    public void getRights(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetRightsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Checks if an actor has the input rights on the input resources.
     * </pre>
     */
    public void checkRights(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCheckRightsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create a regular account.
     * </pre>
     */
    public void createAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create a trial account.
     * </pre>
     */
    public void createTrialAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateTrialAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create an email based account.
     * </pre>
     */
    public void createC1CAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateC1CAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * An endpoint called when a user verifies their email by clicking the link we sent
     * them.
     * </pre>
     */
    public void verifyC1CEmail(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getVerifyC1CEmailMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Grant Entitlement to an Account
     * </pre>
     */
    public void grantEntitlement(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGrantEntitlementMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Revoke Entitlement from an Account
     * </pre>
     */
    public void revokeEntitlement(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRevokeEntitlementMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Ensure default entitlements are granted to an Account
     * </pre>
     */
    public void ensureDefaultEntitlementsGranted(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getEnsureDefaultEntitlementsGrantedMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Assign a role to an assignee
     * </pre>
     */
    public void assignRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAssignRoleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unassign a role from an assignee
     * </pre>
     */
    public void unassignRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnassignRoleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List the assigned roles for an assignee:
     * </pre>
     */
    public void listAssignedRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListAssignedRolesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Assign a resource role to an assignee
     * </pre>
     */
    public void assignResourceRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAssignResourceRoleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unassign a resource role from an assignee
     * </pre>
     */
    public void unassignResourceRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnassignResourceRoleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List the assigned resource roles for an assignee:
     * </pre>
     */
    public void listAssignedResourceRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListAssignedResourceRolesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List roles.
     * </pre>
     */
    public void listRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListRolesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List resource roles.
     * </pre>
     */
    public void listResourceRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListResourceRolesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List resource assignees.
     * </pre>
     */
    public void listResourceAssignees(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListResourceAssigneesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Update Cloudera Manager License Key
     * </pre>
     */
    public void updateClouderaManagerLicenseKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateClouderaManagerLicenseKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Initiates a support case creation pipeline.
     * </pre>
     */
    public void initiateSupportCase(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getInitiateSupportCaseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Notify that a resource was deleted. All resource role assignments
     * associated with this resource will be deleted.
     * </pre>
     */
    public void notifyResourceDeleted(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getNotifyResourceDeletedMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create a machine user
     * </pre>
     */
    public void createMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateMachineUserMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * list machine users
     * </pre>
     */
    public void listMachineUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListMachineUsersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete a machine user
     * </pre>
     */
    public void deleteMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteMachineUserMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listResourceRoleAssignments(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListResourceRoleAssignmentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Sets the account messages.
     * </pre>
     */
    public void setAccountMessages(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetAccountMessagesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Terms acceptance
     * </pre>
     */
    public void acceptTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAcceptTermsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Clearing accepted terms. This will clear the accepted terms with the
     * same version as the current Terms found in the TermsProvider.
     * </pre>
     */
    public void clearAcceptedTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getClearAcceptedTermsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Terms description
     * </pre>
     */
    public void describeTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeTermsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Terms listing
     * </pre>
     */
    public void listTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListTermsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Entitlements listing
     * </pre>
     */
    public void listEntitlements(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListEntitlementsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set terms acceptance expiry
     * </pre>
     */
    public void setTermsAcceptanceExpiry(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetTermsAcceptanceExpiryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Confirm whether Altus account and Azure Subscription Id
     * </pre>
     */
    public void confirmAzureSubscriptionVerified(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getConfirmAzureSubscriptionVerifiedMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Insert Azure Subscriptions
     * </pre>
     */
    public void insertAzureSubscription(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getInsertAzureSubscriptionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create group
     * </pre>
     */
    public void createGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateGroupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete group
     * </pre>
     */
    public void deleteGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteGroupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get groups
     * </pre>
     */
    public void getGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetGroupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List groups
     * </pre>
     */
    public void listGroups(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListGroupsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Update group
     * </pre>
     */
    public void updateGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateGroupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Add member to group
     * </pre>
     */
    public void addMemberToGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAddMemberToGroupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Remove member from group
     * </pre>
     */
    public void removeMemberFromGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRemoveMemberFromGroupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List group members
     * </pre>
     */
    public void listGroupMembers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListGroupMembersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List of groupCRNs corresponding to the member
     * </pre>
     */
    public void listGroupsForMember(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListGroupsForMemberMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List workload administration group names corresponding to the member
     * </pre>
     */
    public void listWorkloadAdministrationGroupsForMember(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListWorkloadAdministrationGroupsForMemberMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates a new cluster ssh private key.
     * </pre>
     */
    public void createClusterSshPrivateKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateClusterSshPrivateKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get cluster ssh private key.
     * </pre>
     */
    public void getClusterSshPrivateKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetClusterSshPrivateKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the authorization information about an assignee.
     * </pre>
     */
    public void getAssigneeAuthorizationInformation(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAssigneeAuthorizationInformationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates the identity provider connector
     * </pre>
     */
    public void createIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateIdentityProviderConnectorMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists identity provider connectors
     * </pre>
     */
    public void listIdentityProviderConnectors(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListIdentityProviderConnectorsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes identity provider connector
     * </pre>
     */
    public void deleteIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteIdentityProviderConnectorMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describes an identity provider connector
     * </pre>
     */
    public void describeIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeIdentityProviderConnectorMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Update an identity provider connector
     * </pre>
     */
    public void updateIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateIdentityProviderConnectorMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set whether login using Cloudera SSO is enabled.
     * </pre>
     */
    public void setClouderaSSOLoginEnabled(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetClouderaSSOLoginEnabledMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Retrieves the control plane IdP metadata file for a workload
     * SSO service.
     * </pre>
     */
    public void getIdPMetadataForWorkloadSSO(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetIdPMetadataForWorkloadSSOMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Process a workload SSO AuthNRequest. If the user is already authenticated
     * an appropriate authn response will be generated to the workload SSO. If not,
     * an appropriate authn request will be generated to either cloudera-sso or
     * to one of the customer defined identity providers.
     * </pre>
     */
    public void processWorkloadSSOAuthnReq(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getProcessWorkloadSSOAuthnReqMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Generate a SSO AuthNRequest for control plane SP-initiated login.
     * </pre>
     */
    public void generateControlPlaneSSOAuthnReq(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGenerateControlPlaneSSOAuthnReqMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set the workload subdomain for an account if no such workload domain has
     * been set for a different account before.
     * </pre>
     */
    public void setWorkloadSubdomain(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetWorkloadSubdomainMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create a machine-user, assign the resource roles and roles to it, create
     * an access key for it, and return it. This is exposed as a convenience method
     * for applications. Callers must be internal actors. The call is idempotent
     * and safe to be called multiple times. Machine users created through this
     * interface should be deleted by called DeleteWorkloadMachineUser.
     * </pre>
     */
    public void createWorkloadMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateWorkloadMachineUserMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete the workload machine user, all the role and resource role assignments,
     * as well as any access keys. This is a convenience method for application who
     * created a machine user using CreateWorkloadMachineUser.
     * </pre>
     */
    public void deleteWorkloadMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteWorkloadMachineUserMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns the the workload administration group name for the
     * (account, right, resource) tuple. Will throw NOT_FOUND exception if no name
     * for the workload administration group has been set yet.
     * </pre>
     */
    public void getWorkloadAdministrationGroupName(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetWorkloadAdministrationGroupNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Sets the workload administration group name for the (account, right, resource)
     * tuple. If the name was already set for the workload administration group
     * this is a no-op and the name generated for the workload administration group
     * will be returned.
     * </pre>
     */
    public void setWorkloadAdministrationGroupName(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetWorkloadAdministrationGroupNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes the workload administration group name for the (account, right, resource)
     * tuple. Throws a NOT_FOUND exception if no such workload administration group
     * can be found.
     * </pre>
     */
    public void deleteWorkloadAdministrationGroupName(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteWorkloadAdministrationGroupNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists the workload administration groups in an account.
     * </pre>
     */
    public void listWorkloadAdministrationGroups(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListWorkloadAdministrationGroupsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Sets the actor workloads credentials. This will replace and overwrite any
     * existing actor credentials.
     * </pre>
     */
    public void setActorWorkloadCredentials(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetActorWorkloadCredentialsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Validates the actor workloads credentials based on the password policy for the account.
     * </pre>
     */
    public void validateActorWorkloadCredentials(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getValidateActorWorkloadCredentialsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Retrieves the actor workload credentials.
     * </pre>
     */
    public void getActorWorkloadCredentials(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetActorWorkloadCredentialsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns a unique ID for the following events:
     * * Role assignment events.
     * * Resource role assignment events.
     * * Group membership changes events.
     * * Actor deletion events.
     * * Actor workload credentials change events.
     * The IDs are guaranteed to be unique and can be used to track the above
     * changes in a specific account. If no such event has happened in the account
     * since tracking started an empty string will be returned instead of an ID.
     * </pre>
     */
    public void getEventGenerationIds(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetEventGenerationIdsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Adds an SSH public key for an actor.
     * </pre>
     */
    public void addActorSshPublicKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAddActorSshPublicKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists the SSH public keys for an actor.
     * </pre>
     */
    public void listActorSshPublicKeys(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListActorSshPublicKeysMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describes an SSH public key.
     * </pre>
     */
    public void describeActorSshPublicKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDescribeActorSshPublicKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes an SSH public key for an actor.
     * </pre>
     */
    public void deleteActorSshPublicKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteActorSshPublicKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Sets the workload password policy for an account.
     * </pre>
     */
    public void setWorkloadPasswordPolicy(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetWorkloadPasswordPolicyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unsets the workload password policy for an account.
     * </pre>
     */
    public void unsetWorkloadPasswordPolicy(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnsetWorkloadPasswordPolicyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Sets the authentication policy for an account.
     * </pre>
     */
    public void setAuthenticationPolicy(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetAuthenticationPolicyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Assign a cloud identity to an actor or group.
     * </pre>
     */
    public void assignCloudIdentity(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAssignCloudIdentityMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unassign a cloud identity from an actor or group.
     * </pre>
     */
    public void unassignCloudIdentity(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnassignCloudIdentityMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Assign a cloud identity to a service principal.
     * </pre>
     */
    public void assignServicePrincipalCloudIdentity(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAssignServicePrincipalCloudIdentityMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unassign a cloud identity from a service principal.
     * </pre>
     */
    public void unassignServicePrincipalCloudIdentity(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnassignServicePrincipalCloudIdentityMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List cloud identity mappings for service principals.
     * </pre>
     */
    public void listServicePrincipalCloudIdentities(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListServicePrincipalCloudIdentitiesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Retrieves the CRN of the default identity provider connector used for CDP initiated logins.
     * </pre>
     */
    public void getDefaultIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetDefaultIdentityProviderConnectorMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Sets the CRN of the default identity provider connector used for CDP initiated logins.
     * </pre>
     */
    public void setDefaultIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetDefaultIdentityProviderConnectorMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get user sync state model, including actors, groups, workload administration groups, etc
     * </pre>
     */
    public void getUserSyncStateModel(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetUserSyncStateModelMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List all role assignments in an account.
     * </pre>
     */
    public void listRoleAssignments(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListRoleAssignmentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Generate authentication token for workload API.
     * </pre>
     */
    public void generateWorkloadAuthToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGenerateWorkloadAuthTokenMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get authentication configuration for workload API.
     * </pre>
     */
    public void getWorkloadAuthConfiguration(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetWorkloadAuthConfigurationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Update user.
     * </pre>
     */
    public void updateUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateUserMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class UserManagementBlockingStub extends io.grpc.stub.AbstractBlockingStub<UserManagementBlockingStub> {
    private UserManagementBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UserManagementBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UserManagementBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Handles an interactive login for a user from a Cloudera identity provider.
     * The user record will be created if necessary.
     * The account record must already exist.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse interactiveLogin(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getInteractiveLoginMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in who is logging in using a
     * their own IdP. We assume that the account is created. The user will be
     * be created and their group membership synchronized with their Altus state.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse interactiveLogin3rdParty(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getInteractiveLogin3rdPartyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in who is logging in using the CDP control plane local
     * identity provider. We assume that the account had been created.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalResponse interactiveLoginLocal(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getInteractiveLoginLocalMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes the account from Altus tests only.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse deleteAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete an actor from Altus.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse deleteActor(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteActorMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete trial user from Altus for tests only.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse deleteTrialUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteTrialUserMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Gets all the information associated with an access key needed to verify a
     * request signature produced that key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse getAccessKeyVerificationData(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAccessKeyVerificationDataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Verifies an interactive user session key. If the session key is expired an
     * exception is thrown. If the session key is found and is valid,
     * information about the user and their account is returned.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse verifyInteractiveUserSessionToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getVerifyInteractiveUserSessionTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Verifies an access token. If the access token is invalid (not found, expired, doesn't match),
     * an exception is thrown. If the access token is valid, information about the actor and their
     * account is returned.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenResponse verifyAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getVerifyAccessTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Authenticate an actor. This method currently supports session tokens and
     * access key authentication.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse authenticate(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAuthenticateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Handles access key usage, marking the last time it was used and the last
     * service on which it was used.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse accessKeyUsage(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAccessKeyUsageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create user. Can only be used to create a user associated with a customer
     * identity provider connector.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserResponse createUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateUserMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get user.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse getUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetUserMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List users.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse listUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListUsersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Find users by Email.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse findUsersByEmail(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFindUsersByEmailMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Find users.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersResponse findUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFindUsersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates a new access key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse createAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateAccessKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Updates an access key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse updateAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateAccessKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes an access key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse deleteAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteAccessKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get access key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse getAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAccessKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List access keys.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse listAccessKeys(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListAccessKeysMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates a new access token.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenResponse createAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateAccessTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes an access token.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenResponse deleteAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteAccessTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get an access token.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenResponse getAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAccessTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List access tokens.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensResponse listAccessTokens(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListAccessTokensMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates a new SCIM access token.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenResponse createScimAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateScimAccessTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes a SCIM access token.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenResponse deleteScimAccessToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteScimAccessTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List SCIM access tokens.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensResponse listScimAccessTokens(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListScimAccessTokensMethod(), getCallOptions(), request);
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
     * Get account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse getAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse listAccounts(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListAccountsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the rights for an actor.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse getRights(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetRightsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Checks if an actor has the input rights on the input resources.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse checkRights(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCheckRightsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a regular account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse createAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a trial account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountResponse createTrialAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateTrialAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create an email based account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountResponse createC1CAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateC1CAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * An endpoint called when a user verifies their email by clicking the link we sent
     * them.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailResponse verifyC1CEmail(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getVerifyC1CEmailMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Grant Entitlement to an Account
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse grantEntitlement(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGrantEntitlementMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Revoke Entitlement from an Account
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse revokeEntitlement(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRevokeEntitlementMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Ensure default entitlements are granted to an Account
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedResponse ensureDefaultEntitlementsGranted(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getEnsureDefaultEntitlementsGrantedMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Assign a role to an assignee
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse assignRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAssignRoleMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unassign a role from an assignee
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse unassignRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnassignRoleMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List the assigned roles for an assignee:
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse listAssignedRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListAssignedRolesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Assign a resource role to an assignee
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse assignResourceRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAssignResourceRoleMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unassign a resource role from an assignee
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse unassignResourceRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnassignResourceRoleMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List the assigned resource roles for an assignee:
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse listAssignedResourceRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListAssignedResourceRolesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List roles.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse listRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListRolesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List resource roles.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse listResourceRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListResourceRolesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List resource assignees.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse listResourceAssignees(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListResourceAssigneesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update Cloudera Manager License Key
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse updateClouderaManagerLicenseKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateClouderaManagerLicenseKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Initiates a support case creation pipeline.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse initiateSupportCase(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getInitiateSupportCaseMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Notify that a resource was deleted. All resource role assignments
     * associated with this resource will be deleted.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse notifyResourceDeleted(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getNotifyResourceDeletedMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a machine user
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse createMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateMachineUserMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * list machine users
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse listMachineUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListMachineUsersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a machine user
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse deleteMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteMachineUserMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse listResourceRoleAssignments(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListResourceRoleAssignmentsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Sets the account messages.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse setAccountMessages(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetAccountMessagesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Terms acceptance
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse acceptTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAcceptTermsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Clearing accepted terms. This will clear the accepted terms with the
     * same version as the current Terms found in the TermsProvider.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse clearAcceptedTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getClearAcceptedTermsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Terms description
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse describeTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeTermsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Terms listing
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse listTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListTermsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Entitlements listing
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse listEntitlements(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListEntitlementsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set terms acceptance expiry
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse setTermsAcceptanceExpiry(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetTermsAcceptanceExpiryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Confirm whether Altus account and Azure Subscription Id
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse confirmAzureSubscriptionVerified(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getConfirmAzureSubscriptionVerifiedMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Insert Azure Subscriptions
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse insertAzureSubscription(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getInsertAzureSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create group
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse createGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateGroupMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete group
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse deleteGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteGroupMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get groups
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupResponse getGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetGroupMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List groups
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse listGroups(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListGroupsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update group
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupResponse updateGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateGroupMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Add member to group
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse addMemberToGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAddMemberToGroupMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Remove member from group
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse removeMemberFromGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRemoveMemberFromGroupMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List group members
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse listGroupMembers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListGroupMembersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List of groupCRNs corresponding to the member
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse listGroupsForMember(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListGroupsForMemberMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List workload administration group names corresponding to the member
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse listWorkloadAdministrationGroupsForMember(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListWorkloadAdministrationGroupsForMemberMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates a new cluster ssh private key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse createClusterSshPrivateKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateClusterSshPrivateKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get cluster ssh private key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse getClusterSshPrivateKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetClusterSshPrivateKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the authorization information about an assignee.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse getAssigneeAuthorizationInformation(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAssigneeAuthorizationInformationMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates the identity provider connector
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse createIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateIdentityProviderConnectorMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists identity provider connectors
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse listIdentityProviderConnectors(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListIdentityProviderConnectorsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes identity provider connector
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse deleteIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteIdentityProviderConnectorMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describes an identity provider connector
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse describeIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeIdentityProviderConnectorMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update an identity provider connector
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse updateIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateIdentityProviderConnectorMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set whether login using Cloudera SSO is enabled.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse setClouderaSSOLoginEnabled(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetClouderaSSOLoginEnabledMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Retrieves the control plane IdP metadata file for a workload
     * SSO service.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse getIdPMetadataForWorkloadSSO(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetIdPMetadataForWorkloadSSOMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Process a workload SSO AuthNRequest. If the user is already authenticated
     * an appropriate authn response will be generated to the workload SSO. If not,
     * an appropriate authn request will be generated to either cloudera-sso or
     * to one of the customer defined identity providers.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqResponse processWorkloadSSOAuthnReq(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getProcessWorkloadSSOAuthnReqMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Generate a SSO AuthNRequest for control plane SP-initiated login.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqResponse generateControlPlaneSSOAuthnReq(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGenerateControlPlaneSSOAuthnReqMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set the workload subdomain for an account if no such workload domain has
     * been set for a different account before.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainResponse setWorkloadSubdomain(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetWorkloadSubdomainMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a machine-user, assign the resource roles and roles to it, create
     * an access key for it, and return it. This is exposed as a convenience method
     * for applications. Callers must be internal actors. The call is idempotent
     * and safe to be called multiple times. Machine users created through this
     * interface should be deleted by called DeleteWorkloadMachineUser.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse createWorkloadMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateWorkloadMachineUserMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete the workload machine user, all the role and resource role assignments,
     * as well as any access keys. This is a convenience method for application who
     * created a machine user using CreateWorkloadMachineUser.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserResponse deleteWorkloadMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteWorkloadMachineUserMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns the the workload administration group name for the
     * (account, right, resource) tuple. Will throw NOT_FOUND exception if no name
     * for the workload administration group has been set yet.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse getWorkloadAdministrationGroupName(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetWorkloadAdministrationGroupNameMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Sets the workload administration group name for the (account, right, resource)
     * tuple. If the name was already set for the workload administration group
     * this is a no-op and the name generated for the workload administration group
     * will be returned.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse setWorkloadAdministrationGroupName(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetWorkloadAdministrationGroupNameMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes the workload administration group name for the (account, right, resource)
     * tuple. Throws a NOT_FOUND exception if no such workload administration group
     * can be found.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse deleteWorkloadAdministrationGroupName(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteWorkloadAdministrationGroupNameMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists the workload administration groups in an account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse listWorkloadAdministrationGroups(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListWorkloadAdministrationGroupsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Sets the actor workloads credentials. This will replace and overwrite any
     * existing actor credentials.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsResponse setActorWorkloadCredentials(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetActorWorkloadCredentialsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Validates the actor workloads credentials based on the password policy for the account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsResponse validateActorWorkloadCredentials(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getValidateActorWorkloadCredentialsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Retrieves the actor workload credentials.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse getActorWorkloadCredentials(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetActorWorkloadCredentialsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns a unique ID for the following events:
     * * Role assignment events.
     * * Resource role assignment events.
     * * Group membership changes events.
     * * Actor deletion events.
     * * Actor workload credentials change events.
     * The IDs are guaranteed to be unique and can be used to track the above
     * changes in a specific account. If no such event has happened in the account
     * since tracking started an empty string will be returned instead of an ID.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse getEventGenerationIds(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetEventGenerationIdsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Adds an SSH public key for an actor.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyResponse addActorSshPublicKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAddActorSshPublicKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists the SSH public keys for an actor.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysResponse listActorSshPublicKeys(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListActorSshPublicKeysMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describes an SSH public key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyResponse describeActorSshPublicKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDescribeActorSshPublicKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes an SSH public key for an actor.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyResponse deleteActorSshPublicKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteActorSshPublicKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Sets the workload password policy for an account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyResponse setWorkloadPasswordPolicy(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetWorkloadPasswordPolicyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unsets the workload password policy for an account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyResponse unsetWorkloadPasswordPolicy(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnsetWorkloadPasswordPolicyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Sets the authentication policy for an account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyResponse setAuthenticationPolicy(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetAuthenticationPolicyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Assign a cloud identity to an actor or group.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityResponse assignCloudIdentity(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAssignCloudIdentityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unassign a cloud identity from an actor or group.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityResponse unassignCloudIdentity(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnassignCloudIdentityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Assign a cloud identity to a service principal.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityResponse assignServicePrincipalCloudIdentity(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAssignServicePrincipalCloudIdentityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unassign a cloud identity from a service principal.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityResponse unassignServicePrincipalCloudIdentity(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnassignServicePrincipalCloudIdentityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List cloud identity mappings for service principals.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse listServicePrincipalCloudIdentities(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListServicePrincipalCloudIdentitiesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Retrieves the CRN of the default identity provider connector used for CDP initiated logins.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorResponse getDefaultIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetDefaultIdentityProviderConnectorMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Sets the CRN of the default identity provider connector used for CDP initiated logins.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorResponse setDefaultIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetDefaultIdentityProviderConnectorMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get user sync state model, including actors, groups, workload administration groups, etc
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse getUserSyncStateModel(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetUserSyncStateModelMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List all role assignments in an account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsResponse listRoleAssignments(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListRoleAssignmentsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Generate authentication token for workload API.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenResponse generateWorkloadAuthToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGenerateWorkloadAuthTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get authentication configuration for workload API.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationResponse getWorkloadAuthConfiguration(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetWorkloadAuthConfigurationMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update user.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserResponse updateUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateUserMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class UserManagementFutureStub extends io.grpc.stub.AbstractFutureStub<UserManagementFutureStub> {
    private UserManagementFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UserManagementFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UserManagementFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Handles an interactive login for a user from a Cloudera identity provider.
     * The user record will be created if necessary.
     * The account record must already exist.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> interactiveLogin(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getInteractiveLoginMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in who is logging in using a
     * their own IdP. We assume that the account is created. The user will be
     * be created and their group membership synchronized with their Altus state.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse> interactiveLogin3rdParty(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getInteractiveLogin3rdPartyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in who is logging in using the CDP control plane local
     * identity provider. We assume that the account had been created.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalResponse> interactiveLoginLocal(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getInteractiveLoginLocalMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes the account from Altus tests only.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> deleteAccount(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteAccountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete an actor from Altus.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> deleteActor(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteActorMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete trial user from Altus for tests only.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> deleteTrialUser(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteTrialUserMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Gets all the information associated with an access key needed to verify a
     * request signature produced that key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> getAccessKeyVerificationData(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAccessKeyVerificationDataMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Verifies an interactive user session key. If the session key is expired an
     * exception is thrown. If the session key is found and is valid,
     * information about the user and their account is returned.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse> verifyInteractiveUserSessionToken(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getVerifyInteractiveUserSessionTokenMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Verifies an access token. If the access token is invalid (not found, expired, doesn't match),
     * an exception is thrown. If the access token is valid, information about the actor and their
     * account is returned.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenResponse> verifyAccessToken(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getVerifyAccessTokenMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Authenticate an actor. This method currently supports session tokens and
     * access key authentication.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> authenticate(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAuthenticateMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Handles access key usage, marking the last time it was used and the last
     * service on which it was used.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> accessKeyUsage(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAccessKeyUsageMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create user. Can only be used to create a user associated with a customer
     * identity provider connector.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserResponse> createUser(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateUserMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get user.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> getUser(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetUserMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List users.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> listUsers(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListUsersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Find users by Email.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> findUsersByEmail(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFindUsersByEmailMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Find users.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersResponse> findUsers(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFindUsersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates a new access key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> createAccessKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateAccessKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Updates an access key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> updateAccessKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateAccessKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes an access key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> deleteAccessKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteAccessKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get access key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> getAccessKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAccessKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List access keys.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> listAccessKeys(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListAccessKeysMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates a new access token.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenResponse> createAccessToken(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateAccessTokenMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes an access token.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenResponse> deleteAccessToken(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteAccessTokenMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get an access token.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenResponse> getAccessToken(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAccessTokenMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List access tokens.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensResponse> listAccessTokens(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListAccessTokensMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates a new SCIM access token.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenResponse> createScimAccessToken(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateScimAccessTokenMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes a SCIM access token.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenResponse> deleteScimAccessToken(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteScimAccessTokenMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List SCIM access tokens.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensResponse> listScimAccessTokens(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListScimAccessTokensMethod(), getCallOptions()), request);
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
     * Get account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> getAccount(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAccountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> listAccounts(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListAccountsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the rights for an actor.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> getRights(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetRightsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Checks if an actor has the input rights on the input resources.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> checkRights(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCheckRightsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create a regular account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> createAccount(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateAccountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create a trial account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountResponse> createTrialAccount(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateTrialAccountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create an email based account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountResponse> createC1CAccount(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateC1CAccountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * An endpoint called when a user verifies their email by clicking the link we sent
     * them.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailResponse> verifyC1CEmail(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getVerifyC1CEmailMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Grant Entitlement to an Account
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> grantEntitlement(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGrantEntitlementMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Revoke Entitlement from an Account
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> revokeEntitlement(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRevokeEntitlementMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Ensure default entitlements are granted to an Account
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedResponse> ensureDefaultEntitlementsGranted(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getEnsureDefaultEntitlementsGrantedMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Assign a role to an assignee
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> assignRole(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAssignRoleMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unassign a role from an assignee
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> unassignRole(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnassignRoleMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List the assigned roles for an assignee:
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> listAssignedRoles(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListAssignedRolesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Assign a resource role to an assignee
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> assignResourceRole(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAssignResourceRoleMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unassign a resource role from an assignee
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> unassignResourceRole(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnassignResourceRoleMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List the assigned resource roles for an assignee:
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> listAssignedResourceRoles(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListAssignedResourceRolesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List roles.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> listRoles(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListRolesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List resource roles.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> listResourceRoles(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListResourceRolesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List resource assignees.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> listResourceAssignees(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListResourceAssigneesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Update Cloudera Manager License Key
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> updateClouderaManagerLicenseKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateClouderaManagerLicenseKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Initiates a support case creation pipeline.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> initiateSupportCase(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getInitiateSupportCaseMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Notify that a resource was deleted. All resource role assignments
     * associated with this resource will be deleted.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> notifyResourceDeleted(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getNotifyResourceDeletedMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create a machine user
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> createMachineUser(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateMachineUserMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * list machine users
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> listMachineUsers(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListMachineUsersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete a machine user
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> deleteMachineUser(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteMachineUserMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> listResourceRoleAssignments(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListResourceRoleAssignmentsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Sets the account messages.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> setAccountMessages(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetAccountMessagesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Terms acceptance
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> acceptTerms(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAcceptTermsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Clearing accepted terms. This will clear the accepted terms with the
     * same version as the current Terms found in the TermsProvider.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> clearAcceptedTerms(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getClearAcceptedTermsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Terms description
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> describeTerms(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeTermsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Terms listing
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> listTerms(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListTermsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Entitlements listing
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> listEntitlements(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListEntitlementsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set terms acceptance expiry
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> setTermsAcceptanceExpiry(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetTermsAcceptanceExpiryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Confirm whether Altus account and Azure Subscription Id
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> confirmAzureSubscriptionVerified(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getConfirmAzureSubscriptionVerifiedMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Insert Azure Subscriptions
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> insertAzureSubscription(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getInsertAzureSubscriptionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create group
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> createGroup(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateGroupMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete group
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> deleteGroup(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteGroupMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get groups
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupResponse> getGroup(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetGroupMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List groups
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> listGroups(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListGroupsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Update group
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupResponse> updateGroup(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateGroupMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Add member to group
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> addMemberToGroup(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAddMemberToGroupMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Remove member from group
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> removeMemberFromGroup(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRemoveMemberFromGroupMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List group members
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> listGroupMembers(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListGroupMembersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List of groupCRNs corresponding to the member
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> listGroupsForMember(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListGroupsForMemberMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List workload administration group names corresponding to the member
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse> listWorkloadAdministrationGroupsForMember(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListWorkloadAdministrationGroupsForMemberMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates a new cluster ssh private key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> createClusterSshPrivateKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateClusterSshPrivateKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get cluster ssh private key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> getClusterSshPrivateKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetClusterSshPrivateKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the authorization information about an assignee.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> getAssigneeAuthorizationInformation(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAssigneeAuthorizationInformationMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates the identity provider connector
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> createIdentityProviderConnector(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateIdentityProviderConnectorMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists identity provider connectors
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> listIdentityProviderConnectors(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListIdentityProviderConnectorsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes identity provider connector
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> deleteIdentityProviderConnector(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteIdentityProviderConnectorMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describes an identity provider connector
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> describeIdentityProviderConnector(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeIdentityProviderConnectorMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Update an identity provider connector
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> updateIdentityProviderConnector(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateIdentityProviderConnectorMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set whether login using Cloudera SSO is enabled.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> setClouderaSSOLoginEnabled(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetClouderaSSOLoginEnabledMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Retrieves the control plane IdP metadata file for a workload
     * SSO service.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse> getIdPMetadataForWorkloadSSO(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetIdPMetadataForWorkloadSSOMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Process a workload SSO AuthNRequest. If the user is already authenticated
     * an appropriate authn response will be generated to the workload SSO. If not,
     * an appropriate authn request will be generated to either cloudera-sso or
     * to one of the customer defined identity providers.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqResponse> processWorkloadSSOAuthnReq(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getProcessWorkloadSSOAuthnReqMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Generate a SSO AuthNRequest for control plane SP-initiated login.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqResponse> generateControlPlaneSSOAuthnReq(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGenerateControlPlaneSSOAuthnReqMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set the workload subdomain for an account if no such workload domain has
     * been set for a different account before.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainResponse> setWorkloadSubdomain(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetWorkloadSubdomainMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create a machine-user, assign the resource roles and roles to it, create
     * an access key for it, and return it. This is exposed as a convenience method
     * for applications. Callers must be internal actors. The call is idempotent
     * and safe to be called multiple times. Machine users created through this
     * interface should be deleted by called DeleteWorkloadMachineUser.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse> createWorkloadMachineUser(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateWorkloadMachineUserMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete the workload machine user, all the role and resource role assignments,
     * as well as any access keys. This is a convenience method for application who
     * created a machine user using CreateWorkloadMachineUser.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserResponse> deleteWorkloadMachineUser(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteWorkloadMachineUserMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns the the workload administration group name for the
     * (account, right, resource) tuple. Will throw NOT_FOUND exception if no name
     * for the workload administration group has been set yet.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse> getWorkloadAdministrationGroupName(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetWorkloadAdministrationGroupNameMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Sets the workload administration group name for the (account, right, resource)
     * tuple. If the name was already set for the workload administration group
     * this is a no-op and the name generated for the workload administration group
     * will be returned.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse> setWorkloadAdministrationGroupName(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetWorkloadAdministrationGroupNameMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes the workload administration group name for the (account, right, resource)
     * tuple. Throws a NOT_FOUND exception if no such workload administration group
     * can be found.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse> deleteWorkloadAdministrationGroupName(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteWorkloadAdministrationGroupNameMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists the workload administration groups in an account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse> listWorkloadAdministrationGroups(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListWorkloadAdministrationGroupsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Sets the actor workloads credentials. This will replace and overwrite any
     * existing actor credentials.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsResponse> setActorWorkloadCredentials(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetActorWorkloadCredentialsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Validates the actor workloads credentials based on the password policy for the account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsResponse> validateActorWorkloadCredentials(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getValidateActorWorkloadCredentialsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Retrieves the actor workload credentials.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse> getActorWorkloadCredentials(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetActorWorkloadCredentialsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns a unique ID for the following events:
     * * Role assignment events.
     * * Resource role assignment events.
     * * Group membership changes events.
     * * Actor deletion events.
     * * Actor workload credentials change events.
     * The IDs are guaranteed to be unique and can be used to track the above
     * changes in a specific account. If no such event has happened in the account
     * since tracking started an empty string will be returned instead of an ID.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse> getEventGenerationIds(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetEventGenerationIdsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Adds an SSH public key for an actor.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyResponse> addActorSshPublicKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAddActorSshPublicKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists the SSH public keys for an actor.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysResponse> listActorSshPublicKeys(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListActorSshPublicKeysMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describes an SSH public key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyResponse> describeActorSshPublicKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDescribeActorSshPublicKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes an SSH public key for an actor.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyResponse> deleteActorSshPublicKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteActorSshPublicKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Sets the workload password policy for an account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyResponse> setWorkloadPasswordPolicy(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetWorkloadPasswordPolicyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unsets the workload password policy for an account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyResponse> unsetWorkloadPasswordPolicy(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnsetWorkloadPasswordPolicyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Sets the authentication policy for an account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyResponse> setAuthenticationPolicy(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetAuthenticationPolicyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Assign a cloud identity to an actor or group.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityResponse> assignCloudIdentity(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAssignCloudIdentityMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unassign a cloud identity from an actor or group.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityResponse> unassignCloudIdentity(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnassignCloudIdentityMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Assign a cloud identity to a service principal.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityResponse> assignServicePrincipalCloudIdentity(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAssignServicePrincipalCloudIdentityMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unassign a cloud identity from a service principal.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityResponse> unassignServicePrincipalCloudIdentity(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnassignServicePrincipalCloudIdentityMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List cloud identity mappings for service principals.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse> listServicePrincipalCloudIdentities(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListServicePrincipalCloudIdentitiesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Retrieves the CRN of the default identity provider connector used for CDP initiated logins.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorResponse> getDefaultIdentityProviderConnector(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetDefaultIdentityProviderConnectorMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Sets the CRN of the default identity provider connector used for CDP initiated logins.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorResponse> setDefaultIdentityProviderConnector(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetDefaultIdentityProviderConnectorMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get user sync state model, including actors, groups, workload administration groups, etc
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse> getUserSyncStateModel(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetUserSyncStateModelMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List all role assignments in an account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsResponse> listRoleAssignments(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListRoleAssignmentsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Generate authentication token for workload API.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenResponse> generateWorkloadAuthToken(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGenerateWorkloadAuthTokenMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get authentication configuration for workload API.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationResponse> getWorkloadAuthConfiguration(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetWorkloadAuthConfigurationMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Update user.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserResponse> updateUser(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateUserMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_INTERACTIVE_LOGIN = 0;
  private static final int METHODID_INTERACTIVE_LOGIN3RD_PARTY = 1;
  private static final int METHODID_INTERACTIVE_LOGIN_LOCAL = 2;
  private static final int METHODID_DELETE_ACCOUNT = 3;
  private static final int METHODID_DELETE_ACTOR = 4;
  private static final int METHODID_DELETE_TRIAL_USER = 5;
  private static final int METHODID_GET_ACCESS_KEY_VERIFICATION_DATA = 6;
  private static final int METHODID_VERIFY_INTERACTIVE_USER_SESSION_TOKEN = 7;
  private static final int METHODID_VERIFY_ACCESS_TOKEN = 8;
  private static final int METHODID_AUTHENTICATE = 9;
  private static final int METHODID_ACCESS_KEY_USAGE = 10;
  private static final int METHODID_CREATE_USER = 11;
  private static final int METHODID_GET_USER = 12;
  private static final int METHODID_LIST_USERS = 13;
  private static final int METHODID_FIND_USERS_BY_EMAIL = 14;
  private static final int METHODID_FIND_USERS = 15;
  private static final int METHODID_CREATE_ACCESS_KEY = 16;
  private static final int METHODID_UPDATE_ACCESS_KEY = 17;
  private static final int METHODID_DELETE_ACCESS_KEY = 18;
  private static final int METHODID_GET_ACCESS_KEY = 19;
  private static final int METHODID_LIST_ACCESS_KEYS = 20;
  private static final int METHODID_CREATE_ACCESS_TOKEN = 21;
  private static final int METHODID_DELETE_ACCESS_TOKEN = 22;
  private static final int METHODID_GET_ACCESS_TOKEN = 23;
  private static final int METHODID_LIST_ACCESS_TOKENS = 24;
  private static final int METHODID_CREATE_SCIM_ACCESS_TOKEN = 25;
  private static final int METHODID_DELETE_SCIM_ACCESS_TOKEN = 26;
  private static final int METHODID_LIST_SCIM_ACCESS_TOKENS = 27;
  private static final int METHODID_GET_VERSION = 28;
  private static final int METHODID_GET_ACCOUNT = 29;
  private static final int METHODID_LIST_ACCOUNTS = 30;
  private static final int METHODID_GET_RIGHTS = 31;
  private static final int METHODID_CHECK_RIGHTS = 32;
  private static final int METHODID_CREATE_ACCOUNT = 33;
  private static final int METHODID_CREATE_TRIAL_ACCOUNT = 34;
  private static final int METHODID_CREATE_C1CACCOUNT = 35;
  private static final int METHODID_VERIFY_C1CEMAIL = 36;
  private static final int METHODID_GRANT_ENTITLEMENT = 37;
  private static final int METHODID_REVOKE_ENTITLEMENT = 38;
  private static final int METHODID_ENSURE_DEFAULT_ENTITLEMENTS_GRANTED = 39;
  private static final int METHODID_ASSIGN_ROLE = 40;
  private static final int METHODID_UNASSIGN_ROLE = 41;
  private static final int METHODID_LIST_ASSIGNED_ROLES = 42;
  private static final int METHODID_ASSIGN_RESOURCE_ROLE = 43;
  private static final int METHODID_UNASSIGN_RESOURCE_ROLE = 44;
  private static final int METHODID_LIST_ASSIGNED_RESOURCE_ROLES = 45;
  private static final int METHODID_LIST_ROLES = 46;
  private static final int METHODID_LIST_RESOURCE_ROLES = 47;
  private static final int METHODID_LIST_RESOURCE_ASSIGNEES = 48;
  private static final int METHODID_UPDATE_CLOUDERA_MANAGER_LICENSE_KEY = 49;
  private static final int METHODID_INITIATE_SUPPORT_CASE = 50;
  private static final int METHODID_NOTIFY_RESOURCE_DELETED = 51;
  private static final int METHODID_CREATE_MACHINE_USER = 52;
  private static final int METHODID_LIST_MACHINE_USERS = 53;
  private static final int METHODID_DELETE_MACHINE_USER = 54;
  private static final int METHODID_LIST_RESOURCE_ROLE_ASSIGNMENTS = 55;
  private static final int METHODID_SET_ACCOUNT_MESSAGES = 56;
  private static final int METHODID_ACCEPT_TERMS = 57;
  private static final int METHODID_CLEAR_ACCEPTED_TERMS = 58;
  private static final int METHODID_DESCRIBE_TERMS = 59;
  private static final int METHODID_LIST_TERMS = 60;
  private static final int METHODID_LIST_ENTITLEMENTS = 61;
  private static final int METHODID_SET_TERMS_ACCEPTANCE_EXPIRY = 62;
  private static final int METHODID_CONFIRM_AZURE_SUBSCRIPTION_VERIFIED = 63;
  private static final int METHODID_INSERT_AZURE_SUBSCRIPTION = 64;
  private static final int METHODID_CREATE_GROUP = 65;
  private static final int METHODID_DELETE_GROUP = 66;
  private static final int METHODID_GET_GROUP = 67;
  private static final int METHODID_LIST_GROUPS = 68;
  private static final int METHODID_UPDATE_GROUP = 69;
  private static final int METHODID_ADD_MEMBER_TO_GROUP = 70;
  private static final int METHODID_REMOVE_MEMBER_FROM_GROUP = 71;
  private static final int METHODID_LIST_GROUP_MEMBERS = 72;
  private static final int METHODID_LIST_GROUPS_FOR_MEMBER = 73;
  private static final int METHODID_LIST_WORKLOAD_ADMINISTRATION_GROUPS_FOR_MEMBER = 74;
  private static final int METHODID_CREATE_CLUSTER_SSH_PRIVATE_KEY = 75;
  private static final int METHODID_GET_CLUSTER_SSH_PRIVATE_KEY = 76;
  private static final int METHODID_GET_ASSIGNEE_AUTHORIZATION_INFORMATION = 77;
  private static final int METHODID_CREATE_IDENTITY_PROVIDER_CONNECTOR = 78;
  private static final int METHODID_LIST_IDENTITY_PROVIDER_CONNECTORS = 79;
  private static final int METHODID_DELETE_IDENTITY_PROVIDER_CONNECTOR = 80;
  private static final int METHODID_DESCRIBE_IDENTITY_PROVIDER_CONNECTOR = 81;
  private static final int METHODID_UPDATE_IDENTITY_PROVIDER_CONNECTOR = 82;
  private static final int METHODID_SET_CLOUDERA_SSOLOGIN_ENABLED = 83;
  private static final int METHODID_GET_ID_PMETADATA_FOR_WORKLOAD_SSO = 84;
  private static final int METHODID_PROCESS_WORKLOAD_SSOAUTHN_REQ = 85;
  private static final int METHODID_GENERATE_CONTROL_PLANE_SSOAUTHN_REQ = 86;
  private static final int METHODID_SET_WORKLOAD_SUBDOMAIN = 87;
  private static final int METHODID_CREATE_WORKLOAD_MACHINE_USER = 88;
  private static final int METHODID_DELETE_WORKLOAD_MACHINE_USER = 89;
  private static final int METHODID_GET_WORKLOAD_ADMINISTRATION_GROUP_NAME = 90;
  private static final int METHODID_SET_WORKLOAD_ADMINISTRATION_GROUP_NAME = 91;
  private static final int METHODID_DELETE_WORKLOAD_ADMINISTRATION_GROUP_NAME = 92;
  private static final int METHODID_LIST_WORKLOAD_ADMINISTRATION_GROUPS = 93;
  private static final int METHODID_SET_ACTOR_WORKLOAD_CREDENTIALS = 94;
  private static final int METHODID_VALIDATE_ACTOR_WORKLOAD_CREDENTIALS = 95;
  private static final int METHODID_GET_ACTOR_WORKLOAD_CREDENTIALS = 96;
  private static final int METHODID_GET_EVENT_GENERATION_IDS = 97;
  private static final int METHODID_ADD_ACTOR_SSH_PUBLIC_KEY = 98;
  private static final int METHODID_LIST_ACTOR_SSH_PUBLIC_KEYS = 99;
  private static final int METHODID_DESCRIBE_ACTOR_SSH_PUBLIC_KEY = 100;
  private static final int METHODID_DELETE_ACTOR_SSH_PUBLIC_KEY = 101;
  private static final int METHODID_SET_WORKLOAD_PASSWORD_POLICY = 102;
  private static final int METHODID_UNSET_WORKLOAD_PASSWORD_POLICY = 103;
  private static final int METHODID_SET_AUTHENTICATION_POLICY = 104;
  private static final int METHODID_ASSIGN_CLOUD_IDENTITY = 105;
  private static final int METHODID_UNASSIGN_CLOUD_IDENTITY = 106;
  private static final int METHODID_ASSIGN_SERVICE_PRINCIPAL_CLOUD_IDENTITY = 107;
  private static final int METHODID_UNASSIGN_SERVICE_PRINCIPAL_CLOUD_IDENTITY = 108;
  private static final int METHODID_LIST_SERVICE_PRINCIPAL_CLOUD_IDENTITIES = 109;
  private static final int METHODID_GET_DEFAULT_IDENTITY_PROVIDER_CONNECTOR = 110;
  private static final int METHODID_SET_DEFAULT_IDENTITY_PROVIDER_CONNECTOR = 111;
  private static final int METHODID_GET_USER_SYNC_STATE_MODEL = 112;
  private static final int METHODID_LIST_ROLE_ASSIGNMENTS = 113;
  private static final int METHODID_GENERATE_WORKLOAD_AUTH_TOKEN = 114;
  private static final int METHODID_GET_WORKLOAD_AUTH_CONFIGURATION = 115;
  private static final int METHODID_UPDATE_USER = 116;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final UserManagementImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(UserManagementImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_INTERACTIVE_LOGIN:
          serviceImpl.interactiveLogin((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse>) responseObserver);
          break;
        case METHODID_INTERACTIVE_LOGIN3RD_PARTY:
          serviceImpl.interactiveLogin3rdParty((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse>) responseObserver);
          break;
        case METHODID_INTERACTIVE_LOGIN_LOCAL:
          serviceImpl.interactiveLoginLocal((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginLocalResponse>) responseObserver);
          break;
        case METHODID_DELETE_ACCOUNT:
          serviceImpl.deleteAccount((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse>) responseObserver);
          break;
        case METHODID_DELETE_ACTOR:
          serviceImpl.deleteActor((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse>) responseObserver);
          break;
        case METHODID_DELETE_TRIAL_USER:
          serviceImpl.deleteTrialUser((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse>) responseObserver);
          break;
        case METHODID_GET_ACCESS_KEY_VERIFICATION_DATA:
          serviceImpl.getAccessKeyVerificationData((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse>) responseObserver);
          break;
        case METHODID_VERIFY_INTERACTIVE_USER_SESSION_TOKEN:
          serviceImpl.verifyInteractiveUserSessionToken((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse>) responseObserver);
          break;
        case METHODID_VERIFY_ACCESS_TOKEN:
          serviceImpl.verifyAccessToken((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyAccessTokenResponse>) responseObserver);
          break;
        case METHODID_AUTHENTICATE:
          serviceImpl.authenticate((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse>) responseObserver);
          break;
        case METHODID_ACCESS_KEY_USAGE:
          serviceImpl.accessKeyUsage((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse>) responseObserver);
          break;
        case METHODID_CREATE_USER:
          serviceImpl.createUser((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateUserResponse>) responseObserver);
          break;
        case METHODID_GET_USER:
          serviceImpl.getUser((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse>) responseObserver);
          break;
        case METHODID_LIST_USERS:
          serviceImpl.listUsers((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse>) responseObserver);
          break;
        case METHODID_FIND_USERS_BY_EMAIL:
          serviceImpl.findUsersByEmail((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse>) responseObserver);
          break;
        case METHODID_FIND_USERS:
          serviceImpl.findUsers((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersResponse>) responseObserver);
          break;
        case METHODID_CREATE_ACCESS_KEY:
          serviceImpl.createAccessKey((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse>) responseObserver);
          break;
        case METHODID_UPDATE_ACCESS_KEY:
          serviceImpl.updateAccessKey((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse>) responseObserver);
          break;
        case METHODID_DELETE_ACCESS_KEY:
          serviceImpl.deleteAccessKey((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse>) responseObserver);
          break;
        case METHODID_GET_ACCESS_KEY:
          serviceImpl.getAccessKey((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse>) responseObserver);
          break;
        case METHODID_LIST_ACCESS_KEYS:
          serviceImpl.listAccessKeys((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse>) responseObserver);
          break;
        case METHODID_CREATE_ACCESS_TOKEN:
          serviceImpl.createAccessToken((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessTokenResponse>) responseObserver);
          break;
        case METHODID_DELETE_ACCESS_TOKEN:
          serviceImpl.deleteAccessToken((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessTokenResponse>) responseObserver);
          break;
        case METHODID_GET_ACCESS_TOKEN:
          serviceImpl.getAccessToken((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessTokenResponse>) responseObserver);
          break;
        case METHODID_LIST_ACCESS_TOKENS:
          serviceImpl.listAccessTokens((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessTokensResponse>) responseObserver);
          break;
        case METHODID_CREATE_SCIM_ACCESS_TOKEN:
          serviceImpl.createScimAccessToken((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateScimAccessTokenResponse>) responseObserver);
          break;
        case METHODID_DELETE_SCIM_ACCESS_TOKEN:
          serviceImpl.deleteScimAccessToken((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteScimAccessTokenResponse>) responseObserver);
          break;
        case METHODID_LIST_SCIM_ACCESS_TOKENS:
          serviceImpl.listScimAccessTokens((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListScimAccessTokensResponse>) responseObserver);
          break;
        case METHODID_GET_VERSION:
          serviceImpl.getVersion((com.cloudera.thunderhead.service.common.version.Version.VersionRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.common.version.Version.VersionResponse>) responseObserver);
          break;
        case METHODID_GET_ACCOUNT:
          serviceImpl.getAccount((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse>) responseObserver);
          break;
        case METHODID_LIST_ACCOUNTS:
          serviceImpl.listAccounts((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse>) responseObserver);
          break;
        case METHODID_GET_RIGHTS:
          serviceImpl.getRights((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse>) responseObserver);
          break;
        case METHODID_CHECK_RIGHTS:
          serviceImpl.checkRights((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse>) responseObserver);
          break;
        case METHODID_CREATE_ACCOUNT:
          serviceImpl.createAccount((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse>) responseObserver);
          break;
        case METHODID_CREATE_TRIAL_ACCOUNT:
          serviceImpl.createTrialAccount((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateTrialAccountResponse>) responseObserver);
          break;
        case METHODID_CREATE_C1CACCOUNT:
          serviceImpl.createC1CAccount((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateC1CAccountResponse>) responseObserver);
          break;
        case METHODID_VERIFY_C1CEMAIL:
          serviceImpl.verifyC1CEmail((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyC1CEmailResponse>) responseObserver);
          break;
        case METHODID_GRANT_ENTITLEMENT:
          serviceImpl.grantEntitlement((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse>) responseObserver);
          break;
        case METHODID_REVOKE_ENTITLEMENT:
          serviceImpl.revokeEntitlement((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse>) responseObserver);
          break;
        case METHODID_ENSURE_DEFAULT_ENTITLEMENTS_GRANTED:
          serviceImpl.ensureDefaultEntitlementsGranted((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EnsureDefaultEntitlementsGrantedResponse>) responseObserver);
          break;
        case METHODID_ASSIGN_ROLE:
          serviceImpl.assignRole((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse>) responseObserver);
          break;
        case METHODID_UNASSIGN_ROLE:
          serviceImpl.unassignRole((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse>) responseObserver);
          break;
        case METHODID_LIST_ASSIGNED_ROLES:
          serviceImpl.listAssignedRoles((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse>) responseObserver);
          break;
        case METHODID_ASSIGN_RESOURCE_ROLE:
          serviceImpl.assignResourceRole((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse>) responseObserver);
          break;
        case METHODID_UNASSIGN_RESOURCE_ROLE:
          serviceImpl.unassignResourceRole((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse>) responseObserver);
          break;
        case METHODID_LIST_ASSIGNED_RESOURCE_ROLES:
          serviceImpl.listAssignedResourceRoles((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse>) responseObserver);
          break;
        case METHODID_LIST_ROLES:
          serviceImpl.listRoles((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse>) responseObserver);
          break;
        case METHODID_LIST_RESOURCE_ROLES:
          serviceImpl.listResourceRoles((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse>) responseObserver);
          break;
        case METHODID_LIST_RESOURCE_ASSIGNEES:
          serviceImpl.listResourceAssignees((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse>) responseObserver);
          break;
        case METHODID_UPDATE_CLOUDERA_MANAGER_LICENSE_KEY:
          serviceImpl.updateClouderaManagerLicenseKey((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse>) responseObserver);
          break;
        case METHODID_INITIATE_SUPPORT_CASE:
          serviceImpl.initiateSupportCase((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse>) responseObserver);
          break;
        case METHODID_NOTIFY_RESOURCE_DELETED:
          serviceImpl.notifyResourceDeleted((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse>) responseObserver);
          break;
        case METHODID_CREATE_MACHINE_USER:
          serviceImpl.createMachineUser((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse>) responseObserver);
          break;
        case METHODID_LIST_MACHINE_USERS:
          serviceImpl.listMachineUsers((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse>) responseObserver);
          break;
        case METHODID_DELETE_MACHINE_USER:
          serviceImpl.deleteMachineUser((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse>) responseObserver);
          break;
        case METHODID_LIST_RESOURCE_ROLE_ASSIGNMENTS:
          serviceImpl.listResourceRoleAssignments((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse>) responseObserver);
          break;
        case METHODID_SET_ACCOUNT_MESSAGES:
          serviceImpl.setAccountMessages((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse>) responseObserver);
          break;
        case METHODID_ACCEPT_TERMS:
          serviceImpl.acceptTerms((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse>) responseObserver);
          break;
        case METHODID_CLEAR_ACCEPTED_TERMS:
          serviceImpl.clearAcceptedTerms((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_TERMS:
          serviceImpl.describeTerms((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse>) responseObserver);
          break;
        case METHODID_LIST_TERMS:
          serviceImpl.listTerms((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse>) responseObserver);
          break;
        case METHODID_LIST_ENTITLEMENTS:
          serviceImpl.listEntitlements((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse>) responseObserver);
          break;
        case METHODID_SET_TERMS_ACCEPTANCE_EXPIRY:
          serviceImpl.setTermsAcceptanceExpiry((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse>) responseObserver);
          break;
        case METHODID_CONFIRM_AZURE_SUBSCRIPTION_VERIFIED:
          serviceImpl.confirmAzureSubscriptionVerified((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse>) responseObserver);
          break;
        case METHODID_INSERT_AZURE_SUBSCRIPTION:
          serviceImpl.insertAzureSubscription((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse>) responseObserver);
          break;
        case METHODID_CREATE_GROUP:
          serviceImpl.createGroup((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse>) responseObserver);
          break;
        case METHODID_DELETE_GROUP:
          serviceImpl.deleteGroup((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse>) responseObserver);
          break;
        case METHODID_GET_GROUP:
          serviceImpl.getGroup((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetGroupResponse>) responseObserver);
          break;
        case METHODID_LIST_GROUPS:
          serviceImpl.listGroups((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse>) responseObserver);
          break;
        case METHODID_UPDATE_GROUP:
          serviceImpl.updateGroup((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateGroupResponse>) responseObserver);
          break;
        case METHODID_ADD_MEMBER_TO_GROUP:
          serviceImpl.addMemberToGroup((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse>) responseObserver);
          break;
        case METHODID_REMOVE_MEMBER_FROM_GROUP:
          serviceImpl.removeMemberFromGroup((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse>) responseObserver);
          break;
        case METHODID_LIST_GROUP_MEMBERS:
          serviceImpl.listGroupMembers((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse>) responseObserver);
          break;
        case METHODID_LIST_GROUPS_FOR_MEMBER:
          serviceImpl.listGroupsForMember((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse>) responseObserver);
          break;
        case METHODID_LIST_WORKLOAD_ADMINISTRATION_GROUPS_FOR_MEMBER:
          serviceImpl.listWorkloadAdministrationGroupsForMember((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse>) responseObserver);
          break;
        case METHODID_CREATE_CLUSTER_SSH_PRIVATE_KEY:
          serviceImpl.createClusterSshPrivateKey((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse>) responseObserver);
          break;
        case METHODID_GET_CLUSTER_SSH_PRIVATE_KEY:
          serviceImpl.getClusterSshPrivateKey((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse>) responseObserver);
          break;
        case METHODID_GET_ASSIGNEE_AUTHORIZATION_INFORMATION:
          serviceImpl.getAssigneeAuthorizationInformation((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse>) responseObserver);
          break;
        case METHODID_CREATE_IDENTITY_PROVIDER_CONNECTOR:
          serviceImpl.createIdentityProviderConnector((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse>) responseObserver);
          break;
        case METHODID_LIST_IDENTITY_PROVIDER_CONNECTORS:
          serviceImpl.listIdentityProviderConnectors((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse>) responseObserver);
          break;
        case METHODID_DELETE_IDENTITY_PROVIDER_CONNECTOR:
          serviceImpl.deleteIdentityProviderConnector((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_IDENTITY_PROVIDER_CONNECTOR:
          serviceImpl.describeIdentityProviderConnector((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse>) responseObserver);
          break;
        case METHODID_UPDATE_IDENTITY_PROVIDER_CONNECTOR:
          serviceImpl.updateIdentityProviderConnector((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse>) responseObserver);
          break;
        case METHODID_SET_CLOUDERA_SSOLOGIN_ENABLED:
          serviceImpl.setClouderaSSOLoginEnabled((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse>) responseObserver);
          break;
        case METHODID_GET_ID_PMETADATA_FOR_WORKLOAD_SSO:
          serviceImpl.getIdPMetadataForWorkloadSSO((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse>) responseObserver);
          break;
        case METHODID_PROCESS_WORKLOAD_SSOAUTHN_REQ:
          serviceImpl.processWorkloadSSOAuthnReq((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ProcessWorkloadSSOAuthnReqResponse>) responseObserver);
          break;
        case METHODID_GENERATE_CONTROL_PLANE_SSOAUTHN_REQ:
          serviceImpl.generateControlPlaneSSOAuthnReq((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateControlPlaneSSOAuthnReqResponse>) responseObserver);
          break;
        case METHODID_SET_WORKLOAD_SUBDOMAIN:
          serviceImpl.setWorkloadSubdomain((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadSubdomainResponse>) responseObserver);
          break;
        case METHODID_CREATE_WORKLOAD_MACHINE_USER:
          serviceImpl.createWorkloadMachineUser((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse>) responseObserver);
          break;
        case METHODID_DELETE_WORKLOAD_MACHINE_USER:
          serviceImpl.deleteWorkloadMachineUser((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadMachineUserResponse>) responseObserver);
          break;
        case METHODID_GET_WORKLOAD_ADMINISTRATION_GROUP_NAME:
          serviceImpl.getWorkloadAdministrationGroupName((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse>) responseObserver);
          break;
        case METHODID_SET_WORKLOAD_ADMINISTRATION_GROUP_NAME:
          serviceImpl.setWorkloadAdministrationGroupName((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse>) responseObserver);
          break;
        case METHODID_DELETE_WORKLOAD_ADMINISTRATION_GROUP_NAME:
          serviceImpl.deleteWorkloadAdministrationGroupName((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse>) responseObserver);
          break;
        case METHODID_LIST_WORKLOAD_ADMINISTRATION_GROUPS:
          serviceImpl.listWorkloadAdministrationGroups((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse>) responseObserver);
          break;
        case METHODID_SET_ACTOR_WORKLOAD_CREDENTIALS:
          serviceImpl.setActorWorkloadCredentials((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetActorWorkloadCredentialsResponse>) responseObserver);
          break;
        case METHODID_VALIDATE_ACTOR_WORKLOAD_CREDENTIALS:
          serviceImpl.validateActorWorkloadCredentials((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ValidateActorWorkloadCredentialsResponse>) responseObserver);
          break;
        case METHODID_GET_ACTOR_WORKLOAD_CREDENTIALS:
          serviceImpl.getActorWorkloadCredentials((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse>) responseObserver);
          break;
        case METHODID_GET_EVENT_GENERATION_IDS:
          serviceImpl.getEventGenerationIds((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse>) responseObserver);
          break;
        case METHODID_ADD_ACTOR_SSH_PUBLIC_KEY:
          serviceImpl.addActorSshPublicKey((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddActorSshPublicKeyResponse>) responseObserver);
          break;
        case METHODID_LIST_ACTOR_SSH_PUBLIC_KEYS:
          serviceImpl.listActorSshPublicKeys((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListActorSshPublicKeysResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_ACTOR_SSH_PUBLIC_KEY:
          serviceImpl.describeActorSshPublicKey((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeActorSshPublicKeyResponse>) responseObserver);
          break;
        case METHODID_DELETE_ACTOR_SSH_PUBLIC_KEY:
          serviceImpl.deleteActorSshPublicKey((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorSshPublicKeyResponse>) responseObserver);
          break;
        case METHODID_SET_WORKLOAD_PASSWORD_POLICY:
          serviceImpl.setWorkloadPasswordPolicy((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadPasswordPolicyResponse>) responseObserver);
          break;
        case METHODID_UNSET_WORKLOAD_PASSWORD_POLICY:
          serviceImpl.unsetWorkloadPasswordPolicy((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnsetWorkloadPasswordPolicyResponse>) responseObserver);
          break;
        case METHODID_SET_AUTHENTICATION_POLICY:
          serviceImpl.setAuthenticationPolicy((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAuthenticationPolicyResponse>) responseObserver);
          break;
        case METHODID_ASSIGN_CLOUD_IDENTITY:
          serviceImpl.assignCloudIdentity((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignCloudIdentityResponse>) responseObserver);
          break;
        case METHODID_UNASSIGN_CLOUD_IDENTITY:
          serviceImpl.unassignCloudIdentity((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignCloudIdentityResponse>) responseObserver);
          break;
        case METHODID_ASSIGN_SERVICE_PRINCIPAL_CLOUD_IDENTITY:
          serviceImpl.assignServicePrincipalCloudIdentity((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignServicePrincipalCloudIdentityResponse>) responseObserver);
          break;
        case METHODID_UNASSIGN_SERVICE_PRINCIPAL_CLOUD_IDENTITY:
          serviceImpl.unassignServicePrincipalCloudIdentity((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignServicePrincipalCloudIdentityResponse>) responseObserver);
          break;
        case METHODID_LIST_SERVICE_PRINCIPAL_CLOUD_IDENTITIES:
          serviceImpl.listServicePrincipalCloudIdentities((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse>) responseObserver);
          break;
        case METHODID_GET_DEFAULT_IDENTITY_PROVIDER_CONNECTOR:
          serviceImpl.getDefaultIdentityProviderConnector((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetDefaultIdentityProviderConnectorResponse>) responseObserver);
          break;
        case METHODID_SET_DEFAULT_IDENTITY_PROVIDER_CONNECTOR:
          serviceImpl.setDefaultIdentityProviderConnector((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetDefaultIdentityProviderConnectorResponse>) responseObserver);
          break;
        case METHODID_GET_USER_SYNC_STATE_MODEL:
          serviceImpl.getUserSyncStateModel((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse>) responseObserver);
          break;
        case METHODID_LIST_ROLE_ASSIGNMENTS:
          serviceImpl.listRoleAssignments((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRoleAssignmentsResponse>) responseObserver);
          break;
        case METHODID_GENERATE_WORKLOAD_AUTH_TOKEN:
          serviceImpl.generateWorkloadAuthToken((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GenerateWorkloadAuthTokenResponse>) responseObserver);
          break;
        case METHODID_GET_WORKLOAD_AUTH_CONFIGURATION:
          serviceImpl.getWorkloadAuthConfiguration((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAuthConfigurationResponse>) responseObserver);
          break;
        case METHODID_UPDATE_USER:
          serviceImpl.updateUser((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateUserResponse>) responseObserver);
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

  private static abstract class UserManagementBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    UserManagementBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.usermanagement.UserManagementProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("UserManagement");
    }
  }

  private static final class UserManagementFileDescriptorSupplier
      extends UserManagementBaseDescriptorSupplier {
    UserManagementFileDescriptorSupplier() {}
  }

  private static final class UserManagementMethodDescriptorSupplier
      extends UserManagementBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    UserManagementMethodDescriptorSupplier(String methodName) {
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
      synchronized (UserManagementGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new UserManagementFileDescriptorSupplier())
              .addMethod(getInteractiveLoginMethod())
              .addMethod(getInteractiveLogin3rdPartyMethod())
              .addMethod(getInteractiveLoginLocalMethod())
              .addMethod(getDeleteAccountMethod())
              .addMethod(getDeleteActorMethod())
              .addMethod(getDeleteTrialUserMethod())
              .addMethod(getGetAccessKeyVerificationDataMethod())
              .addMethod(getVerifyInteractiveUserSessionTokenMethod())
              .addMethod(getVerifyAccessTokenMethod())
              .addMethod(getAuthenticateMethod())
              .addMethod(getAccessKeyUsageMethod())
              .addMethod(getCreateUserMethod())
              .addMethod(getGetUserMethod())
              .addMethod(getListUsersMethod())
              .addMethod(getFindUsersByEmailMethod())
              .addMethod(getFindUsersMethod())
              .addMethod(getCreateAccessKeyMethod())
              .addMethod(getUpdateAccessKeyMethod())
              .addMethod(getDeleteAccessKeyMethod())
              .addMethod(getGetAccessKeyMethod())
              .addMethod(getListAccessKeysMethod())
              .addMethod(getCreateAccessTokenMethod())
              .addMethod(getDeleteAccessTokenMethod())
              .addMethod(getGetAccessTokenMethod())
              .addMethod(getListAccessTokensMethod())
              .addMethod(getCreateScimAccessTokenMethod())
              .addMethod(getDeleteScimAccessTokenMethod())
              .addMethod(getListScimAccessTokensMethod())
              .addMethod(getGetVersionMethod())
              .addMethod(getGetAccountMethod())
              .addMethod(getListAccountsMethod())
              .addMethod(getGetRightsMethod())
              .addMethod(getCheckRightsMethod())
              .addMethod(getCreateAccountMethod())
              .addMethod(getCreateTrialAccountMethod())
              .addMethod(getCreateC1CAccountMethod())
              .addMethod(getVerifyC1CEmailMethod())
              .addMethod(getGrantEntitlementMethod())
              .addMethod(getRevokeEntitlementMethod())
              .addMethod(getEnsureDefaultEntitlementsGrantedMethod())
              .addMethod(getAssignRoleMethod())
              .addMethod(getUnassignRoleMethod())
              .addMethod(getListAssignedRolesMethod())
              .addMethod(getAssignResourceRoleMethod())
              .addMethod(getUnassignResourceRoleMethod())
              .addMethod(getListAssignedResourceRolesMethod())
              .addMethod(getListRolesMethod())
              .addMethod(getListResourceRolesMethod())
              .addMethod(getListResourceAssigneesMethod())
              .addMethod(getUpdateClouderaManagerLicenseKeyMethod())
              .addMethod(getInitiateSupportCaseMethod())
              .addMethod(getNotifyResourceDeletedMethod())
              .addMethod(getCreateMachineUserMethod())
              .addMethod(getListMachineUsersMethod())
              .addMethod(getDeleteMachineUserMethod())
              .addMethod(getListResourceRoleAssignmentsMethod())
              .addMethod(getSetAccountMessagesMethod())
              .addMethod(getAcceptTermsMethod())
              .addMethod(getClearAcceptedTermsMethod())
              .addMethod(getDescribeTermsMethod())
              .addMethod(getListTermsMethod())
              .addMethod(getListEntitlementsMethod())
              .addMethod(getSetTermsAcceptanceExpiryMethod())
              .addMethod(getConfirmAzureSubscriptionVerifiedMethod())
              .addMethod(getInsertAzureSubscriptionMethod())
              .addMethod(getCreateGroupMethod())
              .addMethod(getDeleteGroupMethod())
              .addMethod(getGetGroupMethod())
              .addMethod(getListGroupsMethod())
              .addMethod(getUpdateGroupMethod())
              .addMethod(getAddMemberToGroupMethod())
              .addMethod(getRemoveMemberFromGroupMethod())
              .addMethod(getListGroupMembersMethod())
              .addMethod(getListGroupsForMemberMethod())
              .addMethod(getListWorkloadAdministrationGroupsForMemberMethod())
              .addMethod(getCreateClusterSshPrivateKeyMethod())
              .addMethod(getGetClusterSshPrivateKeyMethod())
              .addMethod(getGetAssigneeAuthorizationInformationMethod())
              .addMethod(getCreateIdentityProviderConnectorMethod())
              .addMethod(getListIdentityProviderConnectorsMethod())
              .addMethod(getDeleteIdentityProviderConnectorMethod())
              .addMethod(getDescribeIdentityProviderConnectorMethod())
              .addMethod(getUpdateIdentityProviderConnectorMethod())
              .addMethod(getSetClouderaSSOLoginEnabledMethod())
              .addMethod(getGetIdPMetadataForWorkloadSSOMethod())
              .addMethod(getProcessWorkloadSSOAuthnReqMethod())
              .addMethod(getGenerateControlPlaneSSOAuthnReqMethod())
              .addMethod(getSetWorkloadSubdomainMethod())
              .addMethod(getCreateWorkloadMachineUserMethod())
              .addMethod(getDeleteWorkloadMachineUserMethod())
              .addMethod(getGetWorkloadAdministrationGroupNameMethod())
              .addMethod(getSetWorkloadAdministrationGroupNameMethod())
              .addMethod(getDeleteWorkloadAdministrationGroupNameMethod())
              .addMethod(getListWorkloadAdministrationGroupsMethod())
              .addMethod(getSetActorWorkloadCredentialsMethod())
              .addMethod(getValidateActorWorkloadCredentialsMethod())
              .addMethod(getGetActorWorkloadCredentialsMethod())
              .addMethod(getGetEventGenerationIdsMethod())
              .addMethod(getAddActorSshPublicKeyMethod())
              .addMethod(getListActorSshPublicKeysMethod())
              .addMethod(getDescribeActorSshPublicKeyMethod())
              .addMethod(getDeleteActorSshPublicKeyMethod())
              .addMethod(getSetWorkloadPasswordPolicyMethod())
              .addMethod(getUnsetWorkloadPasswordPolicyMethod())
              .addMethod(getSetAuthenticationPolicyMethod())
              .addMethod(getAssignCloudIdentityMethod())
              .addMethod(getUnassignCloudIdentityMethod())
              .addMethod(getAssignServicePrincipalCloudIdentityMethod())
              .addMethod(getUnassignServicePrincipalCloudIdentityMethod())
              .addMethod(getListServicePrincipalCloudIdentitiesMethod())
              .addMethod(getGetDefaultIdentityProviderConnectorMethod())
              .addMethod(getSetDefaultIdentityProviderConnectorMethod())
              .addMethod(getGetUserSyncStateModelMethod())
              .addMethod(getListRoleAssignmentsMethod())
              .addMethod(getGenerateWorkloadAuthTokenMethod())
              .addMethod(getGetWorkloadAuthConfigurationMethod())
              .addMethod(getUpdateUserMethod())
              .build();
        }
      }
    }
    return result;
  }
}
