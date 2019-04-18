package com.cloudera.thunderhead.service.usermanagement;

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
    comments = "Source: usermanagement.proto")
public final class UserManagementGrpc {

  private UserManagementGrpc() {}

  public static final String SERVICE_NAME = "usermanagement.UserManagement";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getInteractiveLoginMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> METHOD_INTERACTIVE_LOGIN = getInteractiveLoginMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> getInteractiveLoginMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> getInteractiveLoginMethod() {
    return getInteractiveLoginMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> getInteractiveLoginMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> getInteractiveLoginMethod;
    if ((getInteractiveLoginMethod = UserManagementGrpc.getInteractiveLoginMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getInteractiveLoginMethod = UserManagementGrpc.getInteractiveLoginMethod) == null) {
          UserManagementGrpc.getInteractiveLoginMethod = getInteractiveLoginMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "InteractiveLogin"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getInteractiveLoginTrialMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse> METHOD_INTERACTIVE_LOGIN_TRIAL = getInteractiveLoginTrialMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse> getInteractiveLoginTrialMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse> getInteractiveLoginTrialMethod() {
    return getInteractiveLoginTrialMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse> getInteractiveLoginTrialMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse> getInteractiveLoginTrialMethod;
    if ((getInteractiveLoginTrialMethod = UserManagementGrpc.getInteractiveLoginTrialMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getInteractiveLoginTrialMethod = UserManagementGrpc.getInteractiveLoginTrialMethod) == null) {
          UserManagementGrpc.getInteractiveLoginTrialMethod = getInteractiveLoginTrialMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "InteractiveLoginTrial"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new UserManagementMethodDescriptorSupplier("InteractiveLoginTrial"))
                  .build();
          }
        }
     }
     return getInteractiveLoginTrialMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getInteractiveLogin3rdPartyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse> METHOD_INTERACTIVE_LOGIN3RD_PARTY = getInteractiveLogin3rdPartyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse> getInteractiveLogin3rdPartyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse> getInteractiveLogin3rdPartyMethod() {
    return getInteractiveLogin3rdPartyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse> getInteractiveLogin3rdPartyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse> getInteractiveLogin3rdPartyMethod;
    if ((getInteractiveLogin3rdPartyMethod = UserManagementGrpc.getInteractiveLogin3rdPartyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getInteractiveLogin3rdPartyMethod = UserManagementGrpc.getInteractiveLogin3rdPartyMethod) == null) {
          UserManagementGrpc.getInteractiveLogin3rdPartyMethod = getInteractiveLogin3rdPartyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "InteractiveLogin3rdParty"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteAccountMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> METHOD_DELETE_ACCOUNT = getDeleteAccountMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> getDeleteAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> getDeleteAccountMethod() {
    return getDeleteAccountMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> getDeleteAccountMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> getDeleteAccountMethod;
    if ((getDeleteAccountMethod = UserManagementGrpc.getDeleteAccountMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteAccountMethod = UserManagementGrpc.getDeleteAccountMethod) == null) {
          UserManagementGrpc.getDeleteAccountMethod = getDeleteAccountMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "DeleteAccount"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteActorMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> METHOD_DELETE_ACTOR = getDeleteActorMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> getDeleteActorMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> getDeleteActorMethod() {
    return getDeleteActorMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> getDeleteActorMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> getDeleteActorMethod;
    if ((getDeleteActorMethod = UserManagementGrpc.getDeleteActorMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteActorMethod = UserManagementGrpc.getDeleteActorMethod) == null) {
          UserManagementGrpc.getDeleteActorMethod = getDeleteActorMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "DeleteActor"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteTrialUserMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> METHOD_DELETE_TRIAL_USER = getDeleteTrialUserMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> getDeleteTrialUserMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> getDeleteTrialUserMethod() {
    return getDeleteTrialUserMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> getDeleteTrialUserMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> getDeleteTrialUserMethod;
    if ((getDeleteTrialUserMethod = UserManagementGrpc.getDeleteTrialUserMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteTrialUserMethod = UserManagementGrpc.getDeleteTrialUserMethod) == null) {
          UserManagementGrpc.getDeleteTrialUserMethod = getDeleteTrialUserMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "DeleteTrialUser"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAccessKeyVerificationDataMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> METHOD_GET_ACCESS_KEY_VERIFICATION_DATA = getGetAccessKeyVerificationDataMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> getGetAccessKeyVerificationDataMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> getGetAccessKeyVerificationDataMethod() {
    return getGetAccessKeyVerificationDataMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> getGetAccessKeyVerificationDataMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> getGetAccessKeyVerificationDataMethod;
    if ((getGetAccessKeyVerificationDataMethod = UserManagementGrpc.getGetAccessKeyVerificationDataMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetAccessKeyVerificationDataMethod = UserManagementGrpc.getGetAccessKeyVerificationDataMethod) == null) {
          UserManagementGrpc.getGetAccessKeyVerificationDataMethod = getGetAccessKeyVerificationDataMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "GetAccessKeyVerificationData"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getVerifyInteractiveUserSessionTokenMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse> METHOD_VERIFY_INTERACTIVE_USER_SESSION_TOKEN = getVerifyInteractiveUserSessionTokenMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse> getVerifyInteractiveUserSessionTokenMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse> getVerifyInteractiveUserSessionTokenMethod() {
    return getVerifyInteractiveUserSessionTokenMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse> getVerifyInteractiveUserSessionTokenMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse> getVerifyInteractiveUserSessionTokenMethod;
    if ((getVerifyInteractiveUserSessionTokenMethod = UserManagementGrpc.getVerifyInteractiveUserSessionTokenMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getVerifyInteractiveUserSessionTokenMethod = UserManagementGrpc.getVerifyInteractiveUserSessionTokenMethod) == null) {
          UserManagementGrpc.getVerifyInteractiveUserSessionTokenMethod = getVerifyInteractiveUserSessionTokenMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "VerifyInteractiveUserSessionToken"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getAuthenticateMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> METHOD_AUTHENTICATE = getAuthenticateMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> getAuthenticateMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> getAuthenticateMethod() {
    return getAuthenticateMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> getAuthenticateMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> getAuthenticateMethod;
    if ((getAuthenticateMethod = UserManagementGrpc.getAuthenticateMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAuthenticateMethod = UserManagementGrpc.getAuthenticateMethod) == null) {
          UserManagementGrpc.getAuthenticateMethod = getAuthenticateMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "Authenticate"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getAccessKeyUsageMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> METHOD_ACCESS_KEY_USAGE = getAccessKeyUsageMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> getAccessKeyUsageMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> getAccessKeyUsageMethod() {
    return getAccessKeyUsageMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> getAccessKeyUsageMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> getAccessKeyUsageMethod;
    if ((getAccessKeyUsageMethod = UserManagementGrpc.getAccessKeyUsageMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAccessKeyUsageMethod = UserManagementGrpc.getAccessKeyUsageMethod) == null) {
          UserManagementGrpc.getAccessKeyUsageMethod = getAccessKeyUsageMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "AccessKeyUsage"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetUserMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> METHOD_GET_USER = getGetUserMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> getGetUserMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> getGetUserMethod() {
    return getGetUserMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> getGetUserMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> getGetUserMethod;
    if ((getGetUserMethod = UserManagementGrpc.getGetUserMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetUserMethod = UserManagementGrpc.getGetUserMethod) == null) {
          UserManagementGrpc.getGetUserMethod = getGetUserMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "GetUser"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListUsersMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> METHOD_LIST_USERS = getListUsersMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> getListUsersMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> getListUsersMethod() {
    return getListUsersMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> getListUsersMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> getListUsersMethod;
    if ((getListUsersMethod = UserManagementGrpc.getListUsersMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListUsersMethod = UserManagementGrpc.getListUsersMethod) == null) {
          UserManagementGrpc.getListUsersMethod = getListUsersMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListUsers"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getFindUsersByEmailMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> METHOD_FIND_USERS_BY_EMAIL = getFindUsersByEmailMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> getFindUsersByEmailMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> getFindUsersByEmailMethod() {
    return getFindUsersByEmailMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> getFindUsersByEmailMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> getFindUsersByEmailMethod;
    if ((getFindUsersByEmailMethod = UserManagementGrpc.getFindUsersByEmailMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getFindUsersByEmailMethod = UserManagementGrpc.getFindUsersByEmailMethod) == null) {
          UserManagementGrpc.getFindUsersByEmailMethod = getFindUsersByEmailMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "FindUsersByEmail"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateAccessKeyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> METHOD_CREATE_ACCESS_KEY = getCreateAccessKeyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> getCreateAccessKeyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> getCreateAccessKeyMethod() {
    return getCreateAccessKeyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> getCreateAccessKeyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> getCreateAccessKeyMethod;
    if ((getCreateAccessKeyMethod = UserManagementGrpc.getCreateAccessKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateAccessKeyMethod = UserManagementGrpc.getCreateAccessKeyMethod) == null) {
          UserManagementGrpc.getCreateAccessKeyMethod = getCreateAccessKeyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "CreateAccessKey"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateAccessKeyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> METHOD_UPDATE_ACCESS_KEY = getUpdateAccessKeyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> getUpdateAccessKeyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> getUpdateAccessKeyMethod() {
    return getUpdateAccessKeyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> getUpdateAccessKeyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> getUpdateAccessKeyMethod;
    if ((getUpdateAccessKeyMethod = UserManagementGrpc.getUpdateAccessKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUpdateAccessKeyMethod = UserManagementGrpc.getUpdateAccessKeyMethod) == null) {
          UserManagementGrpc.getUpdateAccessKeyMethod = getUpdateAccessKeyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "UpdateAccessKey"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteAccessKeyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> METHOD_DELETE_ACCESS_KEY = getDeleteAccessKeyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> getDeleteAccessKeyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> getDeleteAccessKeyMethod() {
    return getDeleteAccessKeyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> getDeleteAccessKeyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> getDeleteAccessKeyMethod;
    if ((getDeleteAccessKeyMethod = UserManagementGrpc.getDeleteAccessKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteAccessKeyMethod = UserManagementGrpc.getDeleteAccessKeyMethod) == null) {
          UserManagementGrpc.getDeleteAccessKeyMethod = getDeleteAccessKeyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "DeleteAccessKey"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAccessKeyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> METHOD_GET_ACCESS_KEY = getGetAccessKeyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> getGetAccessKeyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> getGetAccessKeyMethod() {
    return getGetAccessKeyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> getGetAccessKeyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> getGetAccessKeyMethod;
    if ((getGetAccessKeyMethod = UserManagementGrpc.getGetAccessKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetAccessKeyMethod = UserManagementGrpc.getGetAccessKeyMethod) == null) {
          UserManagementGrpc.getGetAccessKeyMethod = getGetAccessKeyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "GetAccessKey"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListAccessKeysMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> METHOD_LIST_ACCESS_KEYS = getListAccessKeysMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> getListAccessKeysMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> getListAccessKeysMethod() {
    return getListAccessKeysMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> getListAccessKeysMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> getListAccessKeysMethod;
    if ((getListAccessKeysMethod = UserManagementGrpc.getListAccessKeysMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListAccessKeysMethod = UserManagementGrpc.getListAccessKeysMethod) == null) {
          UserManagementGrpc.getListAccessKeysMethod = getListAccessKeysMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListAccessKeys"))
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
    if ((getGetVersionMethod = UserManagementGrpc.getGetVersionMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetVersionMethod = UserManagementGrpc.getGetVersionMethod) == null) {
          UserManagementGrpc.getGetVersionMethod = getGetVersionMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.common.version.Version.VersionRequest, com.cloudera.thunderhead.service.common.version.Version.VersionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "GetVersion"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAccountMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> METHOD_GET_ACCOUNT = getGetAccountMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> getGetAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> getGetAccountMethod() {
    return getGetAccountMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> getGetAccountMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> getGetAccountMethod;
    if ((getGetAccountMethod = UserManagementGrpc.getGetAccountMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetAccountMethod = UserManagementGrpc.getGetAccountMethod) == null) {
          UserManagementGrpc.getGetAccountMethod = getGetAccountMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "GetAccount"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListAccountsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> METHOD_LIST_ACCOUNTS = getListAccountsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> getListAccountsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> getListAccountsMethod() {
    return getListAccountsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> getListAccountsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> getListAccountsMethod;
    if ((getListAccountsMethod = UserManagementGrpc.getListAccountsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListAccountsMethod = UserManagementGrpc.getListAccountsMethod) == null) {
          UserManagementGrpc.getListAccountsMethod = getListAccountsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListAccounts"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetRightsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> METHOD_GET_RIGHTS = getGetRightsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> getGetRightsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> getGetRightsMethod() {
    return getGetRightsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> getGetRightsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> getGetRightsMethod;
    if ((getGetRightsMethod = UserManagementGrpc.getGetRightsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetRightsMethod = UserManagementGrpc.getGetRightsMethod) == null) {
          UserManagementGrpc.getGetRightsMethod = getGetRightsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "GetRights"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCheckRightsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> METHOD_CHECK_RIGHTS = getCheckRightsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> getCheckRightsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> getCheckRightsMethod() {
    return getCheckRightsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> getCheckRightsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> getCheckRightsMethod;
    if ((getCheckRightsMethod = UserManagementGrpc.getCheckRightsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCheckRightsMethod = UserManagementGrpc.getCheckRightsMethod) == null) {
          UserManagementGrpc.getCheckRightsMethod = getCheckRightsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "CheckRights"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateAccountMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> METHOD_CREATE_ACCOUNT = getCreateAccountMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> getCreateAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> getCreateAccountMethod() {
    return getCreateAccountMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> getCreateAccountMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> getCreateAccountMethod;
    if ((getCreateAccountMethod = UserManagementGrpc.getCreateAccountMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateAccountMethod = UserManagementGrpc.getCreateAccountMethod) == null) {
          UserManagementGrpc.getCreateAccountMethod = getCreateAccountMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "CreateAccount"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGrantEntitlementMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> METHOD_GRANT_ENTITLEMENT = getGrantEntitlementMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> getGrantEntitlementMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> getGrantEntitlementMethod() {
    return getGrantEntitlementMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> getGrantEntitlementMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> getGrantEntitlementMethod;
    if ((getGrantEntitlementMethod = UserManagementGrpc.getGrantEntitlementMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGrantEntitlementMethod = UserManagementGrpc.getGrantEntitlementMethod) == null) {
          UserManagementGrpc.getGrantEntitlementMethod = getGrantEntitlementMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "GrantEntitlement"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getRevokeEntitlementMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> METHOD_REVOKE_ENTITLEMENT = getRevokeEntitlementMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> getRevokeEntitlementMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> getRevokeEntitlementMethod() {
    return getRevokeEntitlementMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> getRevokeEntitlementMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> getRevokeEntitlementMethod;
    if ((getRevokeEntitlementMethod = UserManagementGrpc.getRevokeEntitlementMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getRevokeEntitlementMethod = UserManagementGrpc.getRevokeEntitlementMethod) == null) {
          UserManagementGrpc.getRevokeEntitlementMethod = getRevokeEntitlementMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "RevokeEntitlement"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getAssignRoleMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> METHOD_ASSIGN_ROLE = getAssignRoleMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> getAssignRoleMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> getAssignRoleMethod() {
    return getAssignRoleMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> getAssignRoleMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> getAssignRoleMethod;
    if ((getAssignRoleMethod = UserManagementGrpc.getAssignRoleMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAssignRoleMethod = UserManagementGrpc.getAssignRoleMethod) == null) {
          UserManagementGrpc.getAssignRoleMethod = getAssignRoleMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "AssignRole"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUnassignRoleMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> METHOD_UNASSIGN_ROLE = getUnassignRoleMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> getUnassignRoleMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> getUnassignRoleMethod() {
    return getUnassignRoleMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> getUnassignRoleMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> getUnassignRoleMethod;
    if ((getUnassignRoleMethod = UserManagementGrpc.getUnassignRoleMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUnassignRoleMethod = UserManagementGrpc.getUnassignRoleMethod) == null) {
          UserManagementGrpc.getUnassignRoleMethod = getUnassignRoleMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "UnassignRole"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListAssignedRolesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> METHOD_LIST_ASSIGNED_ROLES = getListAssignedRolesMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> getListAssignedRolesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> getListAssignedRolesMethod() {
    return getListAssignedRolesMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> getListAssignedRolesMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> getListAssignedRolesMethod;
    if ((getListAssignedRolesMethod = UserManagementGrpc.getListAssignedRolesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListAssignedRolesMethod = UserManagementGrpc.getListAssignedRolesMethod) == null) {
          UserManagementGrpc.getListAssignedRolesMethod = getListAssignedRolesMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListAssignedRoles"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getAssignResourceRoleMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> METHOD_ASSIGN_RESOURCE_ROLE = getAssignResourceRoleMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> getAssignResourceRoleMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> getAssignResourceRoleMethod() {
    return getAssignResourceRoleMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> getAssignResourceRoleMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> getAssignResourceRoleMethod;
    if ((getAssignResourceRoleMethod = UserManagementGrpc.getAssignResourceRoleMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAssignResourceRoleMethod = UserManagementGrpc.getAssignResourceRoleMethod) == null) {
          UserManagementGrpc.getAssignResourceRoleMethod = getAssignResourceRoleMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "AssignResourceRole"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUnassignResourceRoleMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> METHOD_UNASSIGN_RESOURCE_ROLE = getUnassignResourceRoleMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> getUnassignResourceRoleMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> getUnassignResourceRoleMethod() {
    return getUnassignResourceRoleMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> getUnassignResourceRoleMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> getUnassignResourceRoleMethod;
    if ((getUnassignResourceRoleMethod = UserManagementGrpc.getUnassignResourceRoleMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUnassignResourceRoleMethod = UserManagementGrpc.getUnassignResourceRoleMethod) == null) {
          UserManagementGrpc.getUnassignResourceRoleMethod = getUnassignResourceRoleMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "UnassignResourceRole"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListAssignedResourceRolesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> METHOD_LIST_ASSIGNED_RESOURCE_ROLES = getListAssignedResourceRolesMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> getListAssignedResourceRolesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> getListAssignedResourceRolesMethod() {
    return getListAssignedResourceRolesMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> getListAssignedResourceRolesMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> getListAssignedResourceRolesMethod;
    if ((getListAssignedResourceRolesMethod = UserManagementGrpc.getListAssignedResourceRolesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListAssignedResourceRolesMethod = UserManagementGrpc.getListAssignedResourceRolesMethod) == null) {
          UserManagementGrpc.getListAssignedResourceRolesMethod = getListAssignedResourceRolesMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListAssignedResourceRoles"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListRolesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> METHOD_LIST_ROLES = getListRolesMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> getListRolesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> getListRolesMethod() {
    return getListRolesMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> getListRolesMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> getListRolesMethod;
    if ((getListRolesMethod = UserManagementGrpc.getListRolesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListRolesMethod = UserManagementGrpc.getListRolesMethod) == null) {
          UserManagementGrpc.getListRolesMethod = getListRolesMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListRoles"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListResourceRolesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> METHOD_LIST_RESOURCE_ROLES = getListResourceRolesMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> getListResourceRolesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> getListResourceRolesMethod() {
    return getListResourceRolesMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> getListResourceRolesMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> getListResourceRolesMethod;
    if ((getListResourceRolesMethod = UserManagementGrpc.getListResourceRolesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListResourceRolesMethod = UserManagementGrpc.getListResourceRolesMethod) == null) {
          UserManagementGrpc.getListResourceRolesMethod = getListResourceRolesMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListResourceRoles"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListResourceAssigneesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> METHOD_LIST_RESOURCE_ASSIGNEES = getListResourceAssigneesMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> getListResourceAssigneesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> getListResourceAssigneesMethod() {
    return getListResourceAssigneesMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> getListResourceAssigneesMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> getListResourceAssigneesMethod;
    if ((getListResourceAssigneesMethod = UserManagementGrpc.getListResourceAssigneesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListResourceAssigneesMethod = UserManagementGrpc.getListResourceAssigneesMethod) == null) {
          UserManagementGrpc.getListResourceAssigneesMethod = getListResourceAssigneesMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListResourceAssignees"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateClouderaManagerLicenseKeyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> METHOD_UPDATE_CLOUDERA_MANAGER_LICENSE_KEY = getUpdateClouderaManagerLicenseKeyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> getUpdateClouderaManagerLicenseKeyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> getUpdateClouderaManagerLicenseKeyMethod() {
    return getUpdateClouderaManagerLicenseKeyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> getUpdateClouderaManagerLicenseKeyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> getUpdateClouderaManagerLicenseKeyMethod;
    if ((getUpdateClouderaManagerLicenseKeyMethod = UserManagementGrpc.getUpdateClouderaManagerLicenseKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUpdateClouderaManagerLicenseKeyMethod = UserManagementGrpc.getUpdateClouderaManagerLicenseKeyMethod) == null) {
          UserManagementGrpc.getUpdateClouderaManagerLicenseKeyMethod = getUpdateClouderaManagerLicenseKeyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "UpdateClouderaManagerLicenseKey"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getInitiateSupportCaseMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> METHOD_INITIATE_SUPPORT_CASE = getInitiateSupportCaseMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> getInitiateSupportCaseMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> getInitiateSupportCaseMethod() {
    return getInitiateSupportCaseMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> getInitiateSupportCaseMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> getInitiateSupportCaseMethod;
    if ((getInitiateSupportCaseMethod = UserManagementGrpc.getInitiateSupportCaseMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getInitiateSupportCaseMethod = UserManagementGrpc.getInitiateSupportCaseMethod) == null) {
          UserManagementGrpc.getInitiateSupportCaseMethod = getInitiateSupportCaseMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "InitiateSupportCase"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getNotifyResourceDeletedMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> METHOD_NOTIFY_RESOURCE_DELETED = getNotifyResourceDeletedMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> getNotifyResourceDeletedMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> getNotifyResourceDeletedMethod() {
    return getNotifyResourceDeletedMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> getNotifyResourceDeletedMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> getNotifyResourceDeletedMethod;
    if ((getNotifyResourceDeletedMethod = UserManagementGrpc.getNotifyResourceDeletedMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getNotifyResourceDeletedMethod = UserManagementGrpc.getNotifyResourceDeletedMethod) == null) {
          UserManagementGrpc.getNotifyResourceDeletedMethod = getNotifyResourceDeletedMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "NotifyResourceDeleted"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateMachineUserMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> METHOD_CREATE_MACHINE_USER = getCreateMachineUserMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> getCreateMachineUserMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> getCreateMachineUserMethod() {
    return getCreateMachineUserMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> getCreateMachineUserMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> getCreateMachineUserMethod;
    if ((getCreateMachineUserMethod = UserManagementGrpc.getCreateMachineUserMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateMachineUserMethod = UserManagementGrpc.getCreateMachineUserMethod) == null) {
          UserManagementGrpc.getCreateMachineUserMethod = getCreateMachineUserMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "CreateMachineUser"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListMachineUsersMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> METHOD_LIST_MACHINE_USERS = getListMachineUsersMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> getListMachineUsersMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> getListMachineUsersMethod() {
    return getListMachineUsersMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> getListMachineUsersMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> getListMachineUsersMethod;
    if ((getListMachineUsersMethod = UserManagementGrpc.getListMachineUsersMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListMachineUsersMethod = UserManagementGrpc.getListMachineUsersMethod) == null) {
          UserManagementGrpc.getListMachineUsersMethod = getListMachineUsersMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListMachineUsers"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteMachineUserMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> METHOD_DELETE_MACHINE_USER = getDeleteMachineUserMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> getDeleteMachineUserMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> getDeleteMachineUserMethod() {
    return getDeleteMachineUserMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> getDeleteMachineUserMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> getDeleteMachineUserMethod;
    if ((getDeleteMachineUserMethod = UserManagementGrpc.getDeleteMachineUserMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteMachineUserMethod = UserManagementGrpc.getDeleteMachineUserMethod) == null) {
          UserManagementGrpc.getDeleteMachineUserMethod = getDeleteMachineUserMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "DeleteMachineUser"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListResourceRoleAssignmentsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> METHOD_LIST_RESOURCE_ROLE_ASSIGNMENTS = getListResourceRoleAssignmentsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> getListResourceRoleAssignmentsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> getListResourceRoleAssignmentsMethod() {
    return getListResourceRoleAssignmentsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> getListResourceRoleAssignmentsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> getListResourceRoleAssignmentsMethod;
    if ((getListResourceRoleAssignmentsMethod = UserManagementGrpc.getListResourceRoleAssignmentsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListResourceRoleAssignmentsMethod = UserManagementGrpc.getListResourceRoleAssignmentsMethod) == null) {
          UserManagementGrpc.getListResourceRoleAssignmentsMethod = getListResourceRoleAssignmentsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListResourceRoleAssignments"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getSetAccountMessagesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> METHOD_SET_ACCOUNT_MESSAGES = getSetAccountMessagesMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> getSetAccountMessagesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> getSetAccountMessagesMethod() {
    return getSetAccountMessagesMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> getSetAccountMessagesMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> getSetAccountMessagesMethod;
    if ((getSetAccountMessagesMethod = UserManagementGrpc.getSetAccountMessagesMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getSetAccountMessagesMethod = UserManagementGrpc.getSetAccountMessagesMethod) == null) {
          UserManagementGrpc.getSetAccountMessagesMethod = getSetAccountMessagesMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "SetAccountMessages"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getAcceptTermsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> METHOD_ACCEPT_TERMS = getAcceptTermsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> getAcceptTermsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> getAcceptTermsMethod() {
    return getAcceptTermsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> getAcceptTermsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> getAcceptTermsMethod;
    if ((getAcceptTermsMethod = UserManagementGrpc.getAcceptTermsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAcceptTermsMethod = UserManagementGrpc.getAcceptTermsMethod) == null) {
          UserManagementGrpc.getAcceptTermsMethod = getAcceptTermsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "AcceptTerms"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getClearAcceptedTermsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> METHOD_CLEAR_ACCEPTED_TERMS = getClearAcceptedTermsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> getClearAcceptedTermsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> getClearAcceptedTermsMethod() {
    return getClearAcceptedTermsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> getClearAcceptedTermsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> getClearAcceptedTermsMethod;
    if ((getClearAcceptedTermsMethod = UserManagementGrpc.getClearAcceptedTermsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getClearAcceptedTermsMethod = UserManagementGrpc.getClearAcceptedTermsMethod) == null) {
          UserManagementGrpc.getClearAcceptedTermsMethod = getClearAcceptedTermsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ClearAcceptedTerms"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDescribeTermsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> METHOD_DESCRIBE_TERMS = getDescribeTermsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> getDescribeTermsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> getDescribeTermsMethod() {
    return getDescribeTermsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> getDescribeTermsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> getDescribeTermsMethod;
    if ((getDescribeTermsMethod = UserManagementGrpc.getDescribeTermsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDescribeTermsMethod = UserManagementGrpc.getDescribeTermsMethod) == null) {
          UserManagementGrpc.getDescribeTermsMethod = getDescribeTermsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "DescribeTerms"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListTermsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> METHOD_LIST_TERMS = getListTermsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> getListTermsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> getListTermsMethod() {
    return getListTermsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> getListTermsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> getListTermsMethod;
    if ((getListTermsMethod = UserManagementGrpc.getListTermsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListTermsMethod = UserManagementGrpc.getListTermsMethod) == null) {
          UserManagementGrpc.getListTermsMethod = getListTermsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListTerms"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListEntitlementsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> METHOD_LIST_ENTITLEMENTS = getListEntitlementsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> getListEntitlementsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> getListEntitlementsMethod() {
    return getListEntitlementsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> getListEntitlementsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> getListEntitlementsMethod;
    if ((getListEntitlementsMethod = UserManagementGrpc.getListEntitlementsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListEntitlementsMethod = UserManagementGrpc.getListEntitlementsMethod) == null) {
          UserManagementGrpc.getListEntitlementsMethod = getListEntitlementsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListEntitlements"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getSetTermsAcceptanceExpiryMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> METHOD_SET_TERMS_ACCEPTANCE_EXPIRY = getSetTermsAcceptanceExpiryMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> getSetTermsAcceptanceExpiryMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> getSetTermsAcceptanceExpiryMethod() {
    return getSetTermsAcceptanceExpiryMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> getSetTermsAcceptanceExpiryMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> getSetTermsAcceptanceExpiryMethod;
    if ((getSetTermsAcceptanceExpiryMethod = UserManagementGrpc.getSetTermsAcceptanceExpiryMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getSetTermsAcceptanceExpiryMethod = UserManagementGrpc.getSetTermsAcceptanceExpiryMethod) == null) {
          UserManagementGrpc.getSetTermsAcceptanceExpiryMethod = getSetTermsAcceptanceExpiryMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "SetTermsAcceptanceExpiry"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getConfirmAzureSubscriptionVerifiedMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> METHOD_CONFIRM_AZURE_SUBSCRIPTION_VERIFIED = getConfirmAzureSubscriptionVerifiedMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> getConfirmAzureSubscriptionVerifiedMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> getConfirmAzureSubscriptionVerifiedMethod() {
    return getConfirmAzureSubscriptionVerifiedMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> getConfirmAzureSubscriptionVerifiedMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> getConfirmAzureSubscriptionVerifiedMethod;
    if ((getConfirmAzureSubscriptionVerifiedMethod = UserManagementGrpc.getConfirmAzureSubscriptionVerifiedMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getConfirmAzureSubscriptionVerifiedMethod = UserManagementGrpc.getConfirmAzureSubscriptionVerifiedMethod) == null) {
          UserManagementGrpc.getConfirmAzureSubscriptionVerifiedMethod = getConfirmAzureSubscriptionVerifiedMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ConfirmAzureSubscriptionVerified"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getInsertAzureSubscriptionMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> METHOD_INSERT_AZURE_SUBSCRIPTION = getInsertAzureSubscriptionMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> getInsertAzureSubscriptionMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> getInsertAzureSubscriptionMethod() {
    return getInsertAzureSubscriptionMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> getInsertAzureSubscriptionMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> getInsertAzureSubscriptionMethod;
    if ((getInsertAzureSubscriptionMethod = UserManagementGrpc.getInsertAzureSubscriptionMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getInsertAzureSubscriptionMethod = UserManagementGrpc.getInsertAzureSubscriptionMethod) == null) {
          UserManagementGrpc.getInsertAzureSubscriptionMethod = getInsertAzureSubscriptionMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "InsertAzureSubscription"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateGroupMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> METHOD_CREATE_GROUP = getCreateGroupMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> getCreateGroupMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> getCreateGroupMethod() {
    return getCreateGroupMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> getCreateGroupMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> getCreateGroupMethod;
    if ((getCreateGroupMethod = UserManagementGrpc.getCreateGroupMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateGroupMethod = UserManagementGrpc.getCreateGroupMethod) == null) {
          UserManagementGrpc.getCreateGroupMethod = getCreateGroupMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "CreateGroup"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteGroupMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> METHOD_DELETE_GROUP = getDeleteGroupMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> getDeleteGroupMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> getDeleteGroupMethod() {
    return getDeleteGroupMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> getDeleteGroupMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> getDeleteGroupMethod;
    if ((getDeleteGroupMethod = UserManagementGrpc.getDeleteGroupMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteGroupMethod = UserManagementGrpc.getDeleteGroupMethod) == null) {
          UserManagementGrpc.getDeleteGroupMethod = getDeleteGroupMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "DeleteGroup"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListGroupsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> METHOD_LIST_GROUPS = getListGroupsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> getListGroupsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> getListGroupsMethod() {
    return getListGroupsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> getListGroupsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> getListGroupsMethod;
    if ((getListGroupsMethod = UserManagementGrpc.getListGroupsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListGroupsMethod = UserManagementGrpc.getListGroupsMethod) == null) {
          UserManagementGrpc.getListGroupsMethod = getListGroupsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListGroups"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getAddMemberToGroupMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> METHOD_ADD_MEMBER_TO_GROUP = getAddMemberToGroupMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> getAddMemberToGroupMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> getAddMemberToGroupMethod() {
    return getAddMemberToGroupMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> getAddMemberToGroupMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> getAddMemberToGroupMethod;
    if ((getAddMemberToGroupMethod = UserManagementGrpc.getAddMemberToGroupMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getAddMemberToGroupMethod = UserManagementGrpc.getAddMemberToGroupMethod) == null) {
          UserManagementGrpc.getAddMemberToGroupMethod = getAddMemberToGroupMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "AddMemberToGroup"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getRemoveMemberFromGroupMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> METHOD_REMOVE_MEMBER_FROM_GROUP = getRemoveMemberFromGroupMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> getRemoveMemberFromGroupMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> getRemoveMemberFromGroupMethod() {
    return getRemoveMemberFromGroupMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> getRemoveMemberFromGroupMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> getRemoveMemberFromGroupMethod;
    if ((getRemoveMemberFromGroupMethod = UserManagementGrpc.getRemoveMemberFromGroupMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getRemoveMemberFromGroupMethod = UserManagementGrpc.getRemoveMemberFromGroupMethod) == null) {
          UserManagementGrpc.getRemoveMemberFromGroupMethod = getRemoveMemberFromGroupMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "RemoveMemberFromGroup"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListGroupMembersMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> METHOD_LIST_GROUP_MEMBERS = getListGroupMembersMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> getListGroupMembersMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> getListGroupMembersMethod() {
    return getListGroupMembersMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> getListGroupMembersMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> getListGroupMembersMethod;
    if ((getListGroupMembersMethod = UserManagementGrpc.getListGroupMembersMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListGroupMembersMethod = UserManagementGrpc.getListGroupMembersMethod) == null) {
          UserManagementGrpc.getListGroupMembersMethod = getListGroupMembersMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListGroupMembers"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListGroupsForMemberMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> METHOD_LIST_GROUPS_FOR_MEMBER = getListGroupsForMemberMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> getListGroupsForMemberMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> getListGroupsForMemberMethod() {
    return getListGroupsForMemberMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> getListGroupsForMemberMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> getListGroupsForMemberMethod;
    if ((getListGroupsForMemberMethod = UserManagementGrpc.getListGroupsForMemberMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListGroupsForMemberMethod = UserManagementGrpc.getListGroupsForMemberMethod) == null) {
          UserManagementGrpc.getListGroupsForMemberMethod = getListGroupsForMemberMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListGroupsForMember"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateClusterSshPrivateKeyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> METHOD_CREATE_CLUSTER_SSH_PRIVATE_KEY = getCreateClusterSshPrivateKeyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> getCreateClusterSshPrivateKeyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> getCreateClusterSshPrivateKeyMethod() {
    return getCreateClusterSshPrivateKeyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> getCreateClusterSshPrivateKeyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> getCreateClusterSshPrivateKeyMethod;
    if ((getCreateClusterSshPrivateKeyMethod = UserManagementGrpc.getCreateClusterSshPrivateKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateClusterSshPrivateKeyMethod = UserManagementGrpc.getCreateClusterSshPrivateKeyMethod) == null) {
          UserManagementGrpc.getCreateClusterSshPrivateKeyMethod = getCreateClusterSshPrivateKeyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "CreateClusterSshPrivateKey"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetClusterSshPrivateKeyMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> METHOD_GET_CLUSTER_SSH_PRIVATE_KEY = getGetClusterSshPrivateKeyMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> getGetClusterSshPrivateKeyMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> getGetClusterSshPrivateKeyMethod() {
    return getGetClusterSshPrivateKeyMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> getGetClusterSshPrivateKeyMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> getGetClusterSshPrivateKeyMethod;
    if ((getGetClusterSshPrivateKeyMethod = UserManagementGrpc.getGetClusterSshPrivateKeyMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetClusterSshPrivateKeyMethod = UserManagementGrpc.getGetClusterSshPrivateKeyMethod) == null) {
          UserManagementGrpc.getGetClusterSshPrivateKeyMethod = getGetClusterSshPrivateKeyMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "GetClusterSshPrivateKey"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAssigneeAuthorizationInformationMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> METHOD_GET_ASSIGNEE_AUTHORIZATION_INFORMATION = getGetAssigneeAuthorizationInformationMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> getGetAssigneeAuthorizationInformationMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> getGetAssigneeAuthorizationInformationMethod() {
    return getGetAssigneeAuthorizationInformationMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> getGetAssigneeAuthorizationInformationMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> getGetAssigneeAuthorizationInformationMethod;
    if ((getGetAssigneeAuthorizationInformationMethod = UserManagementGrpc.getGetAssigneeAuthorizationInformationMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getGetAssigneeAuthorizationInformationMethod = UserManagementGrpc.getGetAssigneeAuthorizationInformationMethod) == null) {
          UserManagementGrpc.getGetAssigneeAuthorizationInformationMethod = getGetAssigneeAuthorizationInformationMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "GetAssigneeAuthorizationInformation"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateIdentityProviderConnectorMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> METHOD_CREATE_IDENTITY_PROVIDER_CONNECTOR = getCreateIdentityProviderConnectorMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> getCreateIdentityProviderConnectorMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> getCreateIdentityProviderConnectorMethod() {
    return getCreateIdentityProviderConnectorMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> getCreateIdentityProviderConnectorMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> getCreateIdentityProviderConnectorMethod;
    if ((getCreateIdentityProviderConnectorMethod = UserManagementGrpc.getCreateIdentityProviderConnectorMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getCreateIdentityProviderConnectorMethod = UserManagementGrpc.getCreateIdentityProviderConnectorMethod) == null) {
          UserManagementGrpc.getCreateIdentityProviderConnectorMethod = getCreateIdentityProviderConnectorMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "CreateIdentityProviderConnector"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListIdentityProviderConnectorsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> METHOD_LIST_IDENTITY_PROVIDER_CONNECTORS = getListIdentityProviderConnectorsMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> getListIdentityProviderConnectorsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> getListIdentityProviderConnectorsMethod() {
    return getListIdentityProviderConnectorsMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> getListIdentityProviderConnectorsMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> getListIdentityProviderConnectorsMethod;
    if ((getListIdentityProviderConnectorsMethod = UserManagementGrpc.getListIdentityProviderConnectorsMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getListIdentityProviderConnectorsMethod = UserManagementGrpc.getListIdentityProviderConnectorsMethod) == null) {
          UserManagementGrpc.getListIdentityProviderConnectorsMethod = getListIdentityProviderConnectorsMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "ListIdentityProviderConnectors"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeleteIdentityProviderConnectorMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> METHOD_DELETE_IDENTITY_PROVIDER_CONNECTOR = getDeleteIdentityProviderConnectorMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> getDeleteIdentityProviderConnectorMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> getDeleteIdentityProviderConnectorMethod() {
    return getDeleteIdentityProviderConnectorMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> getDeleteIdentityProviderConnectorMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> getDeleteIdentityProviderConnectorMethod;
    if ((getDeleteIdentityProviderConnectorMethod = UserManagementGrpc.getDeleteIdentityProviderConnectorMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDeleteIdentityProviderConnectorMethod = UserManagementGrpc.getDeleteIdentityProviderConnectorMethod) == null) {
          UserManagementGrpc.getDeleteIdentityProviderConnectorMethod = getDeleteIdentityProviderConnectorMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "DeleteIdentityProviderConnector"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDescribeIdentityProviderConnectorMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> METHOD_DESCRIBE_IDENTITY_PROVIDER_CONNECTOR = getDescribeIdentityProviderConnectorMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> getDescribeIdentityProviderConnectorMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> getDescribeIdentityProviderConnectorMethod() {
    return getDescribeIdentityProviderConnectorMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> getDescribeIdentityProviderConnectorMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> getDescribeIdentityProviderConnectorMethod;
    if ((getDescribeIdentityProviderConnectorMethod = UserManagementGrpc.getDescribeIdentityProviderConnectorMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getDescribeIdentityProviderConnectorMethod = UserManagementGrpc.getDescribeIdentityProviderConnectorMethod) == null) {
          UserManagementGrpc.getDescribeIdentityProviderConnectorMethod = getDescribeIdentityProviderConnectorMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "DescribeIdentityProviderConnector"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateIdentityProviderConnectorMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> METHOD_UPDATE_IDENTITY_PROVIDER_CONNECTOR = getUpdateIdentityProviderConnectorMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> getUpdateIdentityProviderConnectorMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> getUpdateIdentityProviderConnectorMethod() {
    return getUpdateIdentityProviderConnectorMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> getUpdateIdentityProviderConnectorMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> getUpdateIdentityProviderConnectorMethod;
    if ((getUpdateIdentityProviderConnectorMethod = UserManagementGrpc.getUpdateIdentityProviderConnectorMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getUpdateIdentityProviderConnectorMethod = UserManagementGrpc.getUpdateIdentityProviderConnectorMethod) == null) {
          UserManagementGrpc.getUpdateIdentityProviderConnectorMethod = getUpdateIdentityProviderConnectorMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "UpdateIdentityProviderConnector"))
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
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getSetClouderaSSOLoginEnabledMethod()} instead. 
  public static final io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> METHOD_SET_CLOUDERA_SSOLOGIN_ENABLED = getSetClouderaSSOLoginEnabledMethodHelper();

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> getSetClouderaSSOLoginEnabledMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> getSetClouderaSSOLoginEnabledMethod() {
    return getSetClouderaSSOLoginEnabledMethodHelper();
  }

  private static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest,
      com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> getSetClouderaSSOLoginEnabledMethodHelper() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> getSetClouderaSSOLoginEnabledMethod;
    if ((getSetClouderaSSOLoginEnabledMethod = UserManagementGrpc.getSetClouderaSSOLoginEnabledMethod) == null) {
      synchronized (UserManagementGrpc.class) {
        if ((getSetClouderaSSOLoginEnabledMethod = UserManagementGrpc.getSetClouderaSSOLoginEnabledMethod) == null) {
          UserManagementGrpc.getSetClouderaSSOLoginEnabledMethod = getSetClouderaSSOLoginEnabledMethod = 
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "usermanagement.UserManagement", "SetClouderaSSOLoginEnabled"))
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

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static UserManagementStub newStub(io.grpc.Channel channel) {
    return new UserManagementStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static UserManagementBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new UserManagementBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static UserManagementFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new UserManagementFutureStub(channel);
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
     * Handles an interactive login for a user in a regular account.
     * A user record will be created if necessary.
     * The account record must alreeady exist.
     * </pre>
     */
    public void interactiveLogin(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getInteractiveLoginMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in a trial account.
     * User and account records will be created if necessary.
     * </pre>
     */
    public void interactiveLoginTrial(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getInteractiveLoginTrialMethodHelper(), responseObserver);
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
      asyncUnimplementedUnaryCall(getInteractiveLogin3rdPartyMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Deletes the account from Altus tests only.
     * </pre>
     */
    public void deleteAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteAccountMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Delete an actor from Altus.
     * </pre>
     */
    public void deleteActor(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteActorMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Delete trial user from Altus for tests only.
     * </pre>
     */
    public void deleteTrialUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteTrialUserMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Gets all the information associated with an access key needed to verify a
     * request signature produced that key.
     * </pre>
     */
    public void getAccessKeyVerificationData(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAccessKeyVerificationDataMethodHelper(), responseObserver);
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
      asyncUnimplementedUnaryCall(getVerifyInteractiveUserSessionTokenMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Authenticate an actor. This method currently supports session tokens and
     * access key authentication.
     * </pre>
     */
    public void authenticate(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAuthenticateMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Handles access key usage, marking the last time it was used and the last
     * service on which it was used.
     * </pre>
     */
    public void accessKeyUsage(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAccessKeyUsageMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Get user.
     * </pre>
     */
    public void getUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetUserMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * List users.
     * </pre>
     */
    public void listUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListUsersMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Find users.
     * </pre>
     */
    public void findUsersByEmail(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getFindUsersByEmailMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Creates a new access key.
     * </pre>
     */
    public void createAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAccessKeyMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Updates an access key.
     * </pre>
     */
    public void updateAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateAccessKeyMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Deletes an access key.
     * </pre>
     */
    public void deleteAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteAccessKeyMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Get access key.
     * </pre>
     */
    public void getAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAccessKeyMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * List access keys.
     * </pre>
     */
    public void listAccessKeys(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListAccessKeysMethodHelper(), responseObserver);
    }

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
     * Get account.
     * </pre>
     */
    public void getAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAccountMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Get account.
     * </pre>
     */
    public void listAccounts(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListAccountsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Get the rights for an actor.
     * </pre>
     */
    public void getRights(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetRightsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Checks if an actor has the input rights on the input resources.
     * </pre>
     */
    public void checkRights(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCheckRightsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Create an account.
     * </pre>
     */
    public void createAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAccountMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Grant Entitlement to an Account
     * </pre>
     */
    public void grantEntitlement(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGrantEntitlementMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Revoke Entitlement from an Account
     * </pre>
     */
    public void revokeEntitlement(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRevokeEntitlementMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Assign a role to an assignee
     * </pre>
     */
    public void assignRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAssignRoleMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Unassign a role from an assignee
     * </pre>
     */
    public void unassignRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getUnassignRoleMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * List the assigned roles for an assignee:
     * </pre>
     */
    public void listAssignedRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListAssignedRolesMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Assign a resource role to an assignee
     * </pre>
     */
    public void assignResourceRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAssignResourceRoleMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Unassign a resource role from an assignee
     * </pre>
     */
    public void unassignResourceRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getUnassignResourceRoleMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * List the assigned resource roles for an assignee:
     * </pre>
     */
    public void listAssignedResourceRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListAssignedResourceRolesMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * List roles.
     * </pre>
     */
    public void listRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListRolesMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * List resource roles.
     * </pre>
     */
    public void listResourceRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListResourceRolesMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * List resource assignees.
     * </pre>
     */
    public void listResourceAssignees(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListResourceAssigneesMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Update Cloudera Manager License Key
     * </pre>
     */
    public void updateClouderaManagerLicenseKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateClouderaManagerLicenseKeyMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Initiates a support case creation pipeline.
     * </pre>
     */
    public void initiateSupportCase(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getInitiateSupportCaseMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Notify that a resource was deleted. All resource role assignments
     * associated with this resource will be deleted.
     * </pre>
     */
    public void notifyResourceDeleted(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getNotifyResourceDeletedMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Create a machine user
     * </pre>
     */
    public void createMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateMachineUserMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * list machine users
     * </pre>
     */
    public void listMachineUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListMachineUsersMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Delete a machine user
     * </pre>
     */
    public void deleteMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteMachineUserMethodHelper(), responseObserver);
    }

    /**
     */
    public void listResourceRoleAssignments(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListResourceRoleAssignmentsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Sets the account messages.
     * </pre>
     */
    public void setAccountMessages(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSetAccountMessagesMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Terms acceptance
     * </pre>
     */
    public void acceptTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAcceptTermsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Clearing accepted terms. This will clear the accepted terms with the
     * same version as the current Terms found in the TermsProvider.
     * </pre>
     */
    public void clearAcceptedTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getClearAcceptedTermsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Terms description
     * </pre>
     */
    public void describeTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeTermsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Terms listing
     * </pre>
     */
    public void listTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListTermsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Entitlements listing
     * </pre>
     */
    public void listEntitlements(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListEntitlementsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Set terms acceptance expiry
     * </pre>
     */
    public void setTermsAcceptanceExpiry(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSetTermsAcceptanceExpiryMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Confirm whether Altus account and Azure Subscription Id
     * </pre>
     */
    public void confirmAzureSubscriptionVerified(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getConfirmAzureSubscriptionVerifiedMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Insert Azure Subscriptions
     * </pre>
     */
    public void insertAzureSubscription(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getInsertAzureSubscriptionMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Create group
     * </pre>
     */
    public void createGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateGroupMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Delete group
     * </pre>
     */
    public void deleteGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteGroupMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * List groups
     * </pre>
     */
    public void listGroups(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListGroupsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Add member to group
     * </pre>
     */
    public void addMemberToGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAddMemberToGroupMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Remove member from group
     * </pre>
     */
    public void removeMemberFromGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRemoveMemberFromGroupMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * List group members
     * </pre>
     */
    public void listGroupMembers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListGroupMembersMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * List of groupCRNs corresponding to the member
     * </pre>
     */
    public void listGroupsForMember(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListGroupsForMemberMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Creates a new cluster ssh private key.
     * </pre>
     */
    public void createClusterSshPrivateKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateClusterSshPrivateKeyMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Get cluster ssh private key.
     * </pre>
     */
    public void getClusterSshPrivateKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetClusterSshPrivateKeyMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Get the authorization information about an assignee.
     * </pre>
     */
    public void getAssigneeAuthorizationInformation(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssigneeAuthorizationInformationMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Creates the identity provider connector
     * </pre>
     */
    public void createIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateIdentityProviderConnectorMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Lists identity provider connectors
     * </pre>
     */
    public void listIdentityProviderConnectors(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListIdentityProviderConnectorsMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Deletes identity provider connector
     * </pre>
     */
    public void deleteIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteIdentityProviderConnectorMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Describes an identity provider connector
     * </pre>
     */
    public void describeIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeIdentityProviderConnectorMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Update an identity provider connector
     * </pre>
     */
    public void updateIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateIdentityProviderConnectorMethodHelper(), responseObserver);
    }

    /**
     * <pre>
     * Set whether login using Cloudera SSO is enabled.
     * </pre>
     */
    public void setClouderaSSOLoginEnabled(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSetClouderaSSOLoginEnabledMethodHelper(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getInteractiveLoginMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse>(
                  this, METHODID_INTERACTIVE_LOGIN)))
          .addMethod(
            getInteractiveLoginTrialMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse>(
                  this, METHODID_INTERACTIVE_LOGIN_TRIAL)))
          .addMethod(
            getInteractiveLogin3rdPartyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse>(
                  this, METHODID_INTERACTIVE_LOGIN3RD_PARTY)))
          .addMethod(
            getDeleteAccountMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse>(
                  this, METHODID_DELETE_ACCOUNT)))
          .addMethod(
            getDeleteActorMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse>(
                  this, METHODID_DELETE_ACTOR)))
          .addMethod(
            getDeleteTrialUserMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse>(
                  this, METHODID_DELETE_TRIAL_USER)))
          .addMethod(
            getGetAccessKeyVerificationDataMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse>(
                  this, METHODID_GET_ACCESS_KEY_VERIFICATION_DATA)))
          .addMethod(
            getVerifyInteractiveUserSessionTokenMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse>(
                  this, METHODID_VERIFY_INTERACTIVE_USER_SESSION_TOKEN)))
          .addMethod(
            getAuthenticateMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse>(
                  this, METHODID_AUTHENTICATE)))
          .addMethod(
            getAccessKeyUsageMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse>(
                  this, METHODID_ACCESS_KEY_USAGE)))
          .addMethod(
            getGetUserMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse>(
                  this, METHODID_GET_USER)))
          .addMethod(
            getListUsersMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse>(
                  this, METHODID_LIST_USERS)))
          .addMethod(
            getFindUsersByEmailMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse>(
                  this, METHODID_FIND_USERS_BY_EMAIL)))
          .addMethod(
            getCreateAccessKeyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse>(
                  this, METHODID_CREATE_ACCESS_KEY)))
          .addMethod(
            getUpdateAccessKeyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse>(
                  this, METHODID_UPDATE_ACCESS_KEY)))
          .addMethod(
            getDeleteAccessKeyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse>(
                  this, METHODID_DELETE_ACCESS_KEY)))
          .addMethod(
            getGetAccessKeyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse>(
                  this, METHODID_GET_ACCESS_KEY)))
          .addMethod(
            getListAccessKeysMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse>(
                  this, METHODID_LIST_ACCESS_KEYS)))
          .addMethod(
            getGetVersionMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.common.version.Version.VersionRequest,
                com.cloudera.thunderhead.service.common.version.Version.VersionResponse>(
                  this, METHODID_GET_VERSION)))
          .addMethod(
            getGetAccountMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse>(
                  this, METHODID_GET_ACCOUNT)))
          .addMethod(
            getListAccountsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse>(
                  this, METHODID_LIST_ACCOUNTS)))
          .addMethod(
            getGetRightsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse>(
                  this, METHODID_GET_RIGHTS)))
          .addMethod(
            getCheckRightsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse>(
                  this, METHODID_CHECK_RIGHTS)))
          .addMethod(
            getCreateAccountMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse>(
                  this, METHODID_CREATE_ACCOUNT)))
          .addMethod(
            getGrantEntitlementMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse>(
                  this, METHODID_GRANT_ENTITLEMENT)))
          .addMethod(
            getRevokeEntitlementMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse>(
                  this, METHODID_REVOKE_ENTITLEMENT)))
          .addMethod(
            getAssignRoleMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse>(
                  this, METHODID_ASSIGN_ROLE)))
          .addMethod(
            getUnassignRoleMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse>(
                  this, METHODID_UNASSIGN_ROLE)))
          .addMethod(
            getListAssignedRolesMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse>(
                  this, METHODID_LIST_ASSIGNED_ROLES)))
          .addMethod(
            getAssignResourceRoleMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse>(
                  this, METHODID_ASSIGN_RESOURCE_ROLE)))
          .addMethod(
            getUnassignResourceRoleMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse>(
                  this, METHODID_UNASSIGN_RESOURCE_ROLE)))
          .addMethod(
            getListAssignedResourceRolesMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse>(
                  this, METHODID_LIST_ASSIGNED_RESOURCE_ROLES)))
          .addMethod(
            getListRolesMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse>(
                  this, METHODID_LIST_ROLES)))
          .addMethod(
            getListResourceRolesMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse>(
                  this, METHODID_LIST_RESOURCE_ROLES)))
          .addMethod(
            getListResourceAssigneesMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse>(
                  this, METHODID_LIST_RESOURCE_ASSIGNEES)))
          .addMethod(
            getUpdateClouderaManagerLicenseKeyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse>(
                  this, METHODID_UPDATE_CLOUDERA_MANAGER_LICENSE_KEY)))
          .addMethod(
            getInitiateSupportCaseMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse>(
                  this, METHODID_INITIATE_SUPPORT_CASE)))
          .addMethod(
            getNotifyResourceDeletedMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse>(
                  this, METHODID_NOTIFY_RESOURCE_DELETED)))
          .addMethod(
            getCreateMachineUserMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse>(
                  this, METHODID_CREATE_MACHINE_USER)))
          .addMethod(
            getListMachineUsersMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse>(
                  this, METHODID_LIST_MACHINE_USERS)))
          .addMethod(
            getDeleteMachineUserMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse>(
                  this, METHODID_DELETE_MACHINE_USER)))
          .addMethod(
            getListResourceRoleAssignmentsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse>(
                  this, METHODID_LIST_RESOURCE_ROLE_ASSIGNMENTS)))
          .addMethod(
            getSetAccountMessagesMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse>(
                  this, METHODID_SET_ACCOUNT_MESSAGES)))
          .addMethod(
            getAcceptTermsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse>(
                  this, METHODID_ACCEPT_TERMS)))
          .addMethod(
            getClearAcceptedTermsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse>(
                  this, METHODID_CLEAR_ACCEPTED_TERMS)))
          .addMethod(
            getDescribeTermsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse>(
                  this, METHODID_DESCRIBE_TERMS)))
          .addMethod(
            getListTermsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse>(
                  this, METHODID_LIST_TERMS)))
          .addMethod(
            getListEntitlementsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse>(
                  this, METHODID_LIST_ENTITLEMENTS)))
          .addMethod(
            getSetTermsAcceptanceExpiryMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse>(
                  this, METHODID_SET_TERMS_ACCEPTANCE_EXPIRY)))
          .addMethod(
            getConfirmAzureSubscriptionVerifiedMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse>(
                  this, METHODID_CONFIRM_AZURE_SUBSCRIPTION_VERIFIED)))
          .addMethod(
            getInsertAzureSubscriptionMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse>(
                  this, METHODID_INSERT_AZURE_SUBSCRIPTION)))
          .addMethod(
            getCreateGroupMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse>(
                  this, METHODID_CREATE_GROUP)))
          .addMethod(
            getDeleteGroupMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse>(
                  this, METHODID_DELETE_GROUP)))
          .addMethod(
            getListGroupsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse>(
                  this, METHODID_LIST_GROUPS)))
          .addMethod(
            getAddMemberToGroupMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse>(
                  this, METHODID_ADD_MEMBER_TO_GROUP)))
          .addMethod(
            getRemoveMemberFromGroupMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse>(
                  this, METHODID_REMOVE_MEMBER_FROM_GROUP)))
          .addMethod(
            getListGroupMembersMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse>(
                  this, METHODID_LIST_GROUP_MEMBERS)))
          .addMethod(
            getListGroupsForMemberMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse>(
                  this, METHODID_LIST_GROUPS_FOR_MEMBER)))
          .addMethod(
            getCreateClusterSshPrivateKeyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse>(
                  this, METHODID_CREATE_CLUSTER_SSH_PRIVATE_KEY)))
          .addMethod(
            getGetClusterSshPrivateKeyMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse>(
                  this, METHODID_GET_CLUSTER_SSH_PRIVATE_KEY)))
          .addMethod(
            getGetAssigneeAuthorizationInformationMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse>(
                  this, METHODID_GET_ASSIGNEE_AUTHORIZATION_INFORMATION)))
          .addMethod(
            getCreateIdentityProviderConnectorMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse>(
                  this, METHODID_CREATE_IDENTITY_PROVIDER_CONNECTOR)))
          .addMethod(
            getListIdentityProviderConnectorsMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse>(
                  this, METHODID_LIST_IDENTITY_PROVIDER_CONNECTORS)))
          .addMethod(
            getDeleteIdentityProviderConnectorMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse>(
                  this, METHODID_DELETE_IDENTITY_PROVIDER_CONNECTOR)))
          .addMethod(
            getDescribeIdentityProviderConnectorMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse>(
                  this, METHODID_DESCRIBE_IDENTITY_PROVIDER_CONNECTOR)))
          .addMethod(
            getUpdateIdentityProviderConnectorMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse>(
                  this, METHODID_UPDATE_IDENTITY_PROVIDER_CONNECTOR)))
          .addMethod(
            getSetClouderaSSOLoginEnabledMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest,
                com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse>(
                  this, METHODID_SET_CLOUDERA_SSOLOGIN_ENABLED)))
          .build();
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class UserManagementStub extends io.grpc.stub.AbstractStub<UserManagementStub> {
    private UserManagementStub(io.grpc.Channel channel) {
      super(channel);
    }

    private UserManagementStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UserManagementStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new UserManagementStub(channel, callOptions);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in a regular account.
     * A user record will be created if necessary.
     * The account record must alreeady exist.
     * </pre>
     */
    public void interactiveLogin(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getInteractiveLoginMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in a trial account.
     * User and account records will be created if necessary.
     * </pre>
     */
    public void interactiveLoginTrial(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getInteractiveLoginTrialMethodHelper(), getCallOptions()), request, responseObserver);
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
      asyncUnaryCall(
          getChannel().newCall(getInteractiveLogin3rdPartyMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes the account from Altus tests only.
     * </pre>
     */
    public void deleteAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteAccountMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete an actor from Altus.
     * </pre>
     */
    public void deleteActor(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteActorMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete trial user from Altus for tests only.
     * </pre>
     */
    public void deleteTrialUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteTrialUserMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Gets all the information associated with an access key needed to verify a
     * request signature produced that key.
     * </pre>
     */
    public void getAccessKeyVerificationData(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAccessKeyVerificationDataMethodHelper(), getCallOptions()), request, responseObserver);
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
      asyncUnaryCall(
          getChannel().newCall(getVerifyInteractiveUserSessionTokenMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Authenticate an actor. This method currently supports session tokens and
     * access key authentication.
     * </pre>
     */
    public void authenticate(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAuthenticateMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Handles access key usage, marking the last time it was used and the last
     * service on which it was used.
     * </pre>
     */
    public void accessKeyUsage(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAccessKeyUsageMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get user.
     * </pre>
     */
    public void getUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetUserMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List users.
     * </pre>
     */
    public void listUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListUsersMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Find users.
     * </pre>
     */
    public void findUsersByEmail(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getFindUsersByEmailMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates a new access key.
     * </pre>
     */
    public void createAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAccessKeyMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Updates an access key.
     * </pre>
     */
    public void updateAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateAccessKeyMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes an access key.
     * </pre>
     */
    public void deleteAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteAccessKeyMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get access key.
     * </pre>
     */
    public void getAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAccessKeyMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List access keys.
     * </pre>
     */
    public void listAccessKeys(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListAccessKeysMethodHelper(), getCallOptions()), request, responseObserver);
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
     * Get account.
     * </pre>
     */
    public void getAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAccountMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get account.
     * </pre>
     */
    public void listAccounts(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListAccountsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the rights for an actor.
     * </pre>
     */
    public void getRights(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetRightsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Checks if an actor has the input rights on the input resources.
     * </pre>
     */
    public void checkRights(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCheckRightsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create an account.
     * </pre>
     */
    public void createAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAccountMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Grant Entitlement to an Account
     * </pre>
     */
    public void grantEntitlement(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGrantEntitlementMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Revoke Entitlement from an Account
     * </pre>
     */
    public void revokeEntitlement(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRevokeEntitlementMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Assign a role to an assignee
     * </pre>
     */
    public void assignRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAssignRoleMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unassign a role from an assignee
     * </pre>
     */
    public void unassignRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnassignRoleMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List the assigned roles for an assignee:
     * </pre>
     */
    public void listAssignedRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListAssignedRolesMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Assign a resource role to an assignee
     * </pre>
     */
    public void assignResourceRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAssignResourceRoleMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unassign a resource role from an assignee
     * </pre>
     */
    public void unassignResourceRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnassignResourceRoleMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List the assigned resource roles for an assignee:
     * </pre>
     */
    public void listAssignedResourceRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListAssignedResourceRolesMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List roles.
     * </pre>
     */
    public void listRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListRolesMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List resource roles.
     * </pre>
     */
    public void listResourceRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListResourceRolesMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List resource assignees.
     * </pre>
     */
    public void listResourceAssignees(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListResourceAssigneesMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Update Cloudera Manager License Key
     * </pre>
     */
    public void updateClouderaManagerLicenseKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateClouderaManagerLicenseKeyMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Initiates a support case creation pipeline.
     * </pre>
     */
    public void initiateSupportCase(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getInitiateSupportCaseMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Notify that a resource was deleted. All resource role assignments
     * associated with this resource will be deleted.
     * </pre>
     */
    public void notifyResourceDeleted(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getNotifyResourceDeletedMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create a machine user
     * </pre>
     */
    public void createMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateMachineUserMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * list machine users
     * </pre>
     */
    public void listMachineUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListMachineUsersMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete a machine user
     * </pre>
     */
    public void deleteMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteMachineUserMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listResourceRoleAssignments(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListResourceRoleAssignmentsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Sets the account messages.
     * </pre>
     */
    public void setAccountMessages(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetAccountMessagesMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Terms acceptance
     * </pre>
     */
    public void acceptTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAcceptTermsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Clearing accepted terms. This will clear the accepted terms with the
     * same version as the current Terms found in the TermsProvider.
     * </pre>
     */
    public void clearAcceptedTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getClearAcceptedTermsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Terms description
     * </pre>
     */
    public void describeTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeTermsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Terms listing
     * </pre>
     */
    public void listTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListTermsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Entitlements listing
     * </pre>
     */
    public void listEntitlements(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListEntitlementsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set terms acceptance expiry
     * </pre>
     */
    public void setTermsAcceptanceExpiry(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetTermsAcceptanceExpiryMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Confirm whether Altus account and Azure Subscription Id
     * </pre>
     */
    public void confirmAzureSubscriptionVerified(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getConfirmAzureSubscriptionVerifiedMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Insert Azure Subscriptions
     * </pre>
     */
    public void insertAzureSubscription(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getInsertAzureSubscriptionMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create group
     * </pre>
     */
    public void createGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateGroupMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete group
     * </pre>
     */
    public void deleteGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteGroupMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List groups
     * </pre>
     */
    public void listGroups(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListGroupsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Add member to group
     * </pre>
     */
    public void addMemberToGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAddMemberToGroupMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Remove member from group
     * </pre>
     */
    public void removeMemberFromGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRemoveMemberFromGroupMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List group members
     * </pre>
     */
    public void listGroupMembers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListGroupMembersMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List of groupCRNs corresponding to the member
     * </pre>
     */
    public void listGroupsForMember(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListGroupsForMemberMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates a new cluster ssh private key.
     * </pre>
     */
    public void createClusterSshPrivateKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateClusterSshPrivateKeyMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get cluster ssh private key.
     * </pre>
     */
    public void getClusterSshPrivateKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetClusterSshPrivateKeyMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the authorization information about an assignee.
     * </pre>
     */
    public void getAssigneeAuthorizationInformation(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssigneeAuthorizationInformationMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates the identity provider connector
     * </pre>
     */
    public void createIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateIdentityProviderConnectorMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists identity provider connectors
     * </pre>
     */
    public void listIdentityProviderConnectors(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListIdentityProviderConnectorsMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes identity provider connector
     * </pre>
     */
    public void deleteIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteIdentityProviderConnectorMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describes an identity provider connector
     * </pre>
     */
    public void describeIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeIdentityProviderConnectorMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Update an identity provider connector
     * </pre>
     */
    public void updateIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateIdentityProviderConnectorMethodHelper(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set whether login using Cloudera SSO is enabled.
     * </pre>
     */
    public void setClouderaSSOLoginEnabled(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetClouderaSSOLoginEnabledMethodHelper(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class UserManagementBlockingStub extends io.grpc.stub.AbstractStub<UserManagementBlockingStub> {
    private UserManagementBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private UserManagementBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UserManagementBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new UserManagementBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in a regular account.
     * A user record will be created if necessary.
     * The account record must alreeady exist.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse interactiveLogin(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest request) {
      return blockingUnaryCall(
          getChannel(), getInteractiveLoginMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in a trial account.
     * User and account records will be created if necessary.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse interactiveLoginTrial(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest request) {
      return blockingUnaryCall(
          getChannel(), getInteractiveLoginTrialMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in who is logging in using a
     * their own IdP. We assume that the account is created. The user will be
     * be created and their group membership synchronized with their Altus state.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse interactiveLogin3rdParty(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest request) {
      return blockingUnaryCall(
          getChannel(), getInteractiveLogin3rdPartyMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes the account from Altus tests only.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse deleteAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteAccountMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete an actor from Altus.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse deleteActor(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteActorMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete trial user from Altus for tests only.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse deleteTrialUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteTrialUserMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Gets all the information associated with an access key needed to verify a
     * request signature produced that key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse getAccessKeyVerificationData(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetAccessKeyVerificationDataMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Verifies an interactive user session key. If the session key is expired an
     * exception is thrown. If the session key is found and is valid,
     * information about the user and their account is returned.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse verifyInteractiveUserSessionToken(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest request) {
      return blockingUnaryCall(
          getChannel(), getVerifyInteractiveUserSessionTokenMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Authenticate an actor. This method currently supports session tokens and
     * access key authentication.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse authenticate(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest request) {
      return blockingUnaryCall(
          getChannel(), getAuthenticateMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Handles access key usage, marking the last time it was used and the last
     * service on which it was used.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse accessKeyUsage(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest request) {
      return blockingUnaryCall(
          getChannel(), getAccessKeyUsageMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get user.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse getUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetUserMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List users.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse listUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest request) {
      return blockingUnaryCall(
          getChannel(), getListUsersMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Find users.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse findUsersByEmail(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest request) {
      return blockingUnaryCall(
          getChannel(), getFindUsersByEmailMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates a new access key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse createAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateAccessKeyMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Updates an access key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse updateAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest request) {
      return blockingUnaryCall(
          getChannel(), getUpdateAccessKeyMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes an access key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse deleteAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteAccessKeyMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get access key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse getAccessKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetAccessKeyMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List access keys.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse listAccessKeys(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest request) {
      return blockingUnaryCall(
          getChannel(), getListAccessKeysMethodHelper(), getCallOptions(), request);
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
     * Get account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse getAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetAccountMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse listAccounts(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListAccountsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the rights for an actor.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse getRights(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetRightsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Checks if an actor has the input rights on the input resources.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse checkRights(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest request) {
      return blockingUnaryCall(
          getChannel(), getCheckRightsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create an account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse createAccount(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateAccountMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Grant Entitlement to an Account
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse grantEntitlement(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest request) {
      return blockingUnaryCall(
          getChannel(), getGrantEntitlementMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Revoke Entitlement from an Account
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse revokeEntitlement(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest request) {
      return blockingUnaryCall(
          getChannel(), getRevokeEntitlementMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Assign a role to an assignee
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse assignRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest request) {
      return blockingUnaryCall(
          getChannel(), getAssignRoleMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unassign a role from an assignee
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse unassignRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest request) {
      return blockingUnaryCall(
          getChannel(), getUnassignRoleMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List the assigned roles for an assignee:
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse listAssignedRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest request) {
      return blockingUnaryCall(
          getChannel(), getListAssignedRolesMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Assign a resource role to an assignee
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse assignResourceRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest request) {
      return blockingUnaryCall(
          getChannel(), getAssignResourceRoleMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unassign a resource role from an assignee
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse unassignResourceRole(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest request) {
      return blockingUnaryCall(
          getChannel(), getUnassignResourceRoleMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List the assigned resource roles for an assignee:
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse listAssignedResourceRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest request) {
      return blockingUnaryCall(
          getChannel(), getListAssignedResourceRolesMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List roles.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse listRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest request) {
      return blockingUnaryCall(
          getChannel(), getListRolesMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List resource roles.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse listResourceRoles(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest request) {
      return blockingUnaryCall(
          getChannel(), getListResourceRolesMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List resource assignees.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse listResourceAssignees(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest request) {
      return blockingUnaryCall(
          getChannel(), getListResourceAssigneesMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update Cloudera Manager License Key
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse updateClouderaManagerLicenseKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest request) {
      return blockingUnaryCall(
          getChannel(), getUpdateClouderaManagerLicenseKeyMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Initiates a support case creation pipeline.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse initiateSupportCase(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest request) {
      return blockingUnaryCall(
          getChannel(), getInitiateSupportCaseMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Notify that a resource was deleted. All resource role assignments
     * associated with this resource will be deleted.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse notifyResourceDeleted(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest request) {
      return blockingUnaryCall(
          getChannel(), getNotifyResourceDeletedMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a machine user
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse createMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateMachineUserMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * list machine users
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse listMachineUsers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest request) {
      return blockingUnaryCall(
          getChannel(), getListMachineUsersMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a machine user
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse deleteMachineUser(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteMachineUserMethodHelper(), getCallOptions(), request);
    }

    /**
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse listResourceRoleAssignments(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListResourceRoleAssignmentsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Sets the account messages.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse setAccountMessages(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest request) {
      return blockingUnaryCall(
          getChannel(), getSetAccountMessagesMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Terms acceptance
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse acceptTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest request) {
      return blockingUnaryCall(
          getChannel(), getAcceptTermsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Clearing accepted terms. This will clear the accepted terms with the
     * same version as the current Terms found in the TermsProvider.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse clearAcceptedTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest request) {
      return blockingUnaryCall(
          getChannel(), getClearAcceptedTermsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Terms description
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse describeTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeTermsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Terms listing
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse listTerms(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListTermsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Entitlements listing
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse listEntitlements(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListEntitlementsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set terms acceptance expiry
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse setTermsAcceptanceExpiry(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest request) {
      return blockingUnaryCall(
          getChannel(), getSetTermsAcceptanceExpiryMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Confirm whether Altus account and Azure Subscription Id
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse confirmAzureSubscriptionVerified(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest request) {
      return blockingUnaryCall(
          getChannel(), getConfirmAzureSubscriptionVerifiedMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Insert Azure Subscriptions
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse insertAzureSubscription(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest request) {
      return blockingUnaryCall(
          getChannel(), getInsertAzureSubscriptionMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create group
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse createGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateGroupMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete group
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse deleteGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteGroupMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List groups
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse listGroups(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListGroupsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Add member to group
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse addMemberToGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest request) {
      return blockingUnaryCall(
          getChannel(), getAddMemberToGroupMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Remove member from group
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse removeMemberFromGroup(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest request) {
      return blockingUnaryCall(
          getChannel(), getRemoveMemberFromGroupMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List group members
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse listGroupMembers(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest request) {
      return blockingUnaryCall(
          getChannel(), getListGroupMembersMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List of groupCRNs corresponding to the member
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse listGroupsForMember(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest request) {
      return blockingUnaryCall(
          getChannel(), getListGroupsForMemberMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates a new cluster ssh private key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse createClusterSshPrivateKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateClusterSshPrivateKeyMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get cluster ssh private key.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse getClusterSshPrivateKey(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetClusterSshPrivateKeyMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the authorization information about an assignee.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse getAssigneeAuthorizationInformation(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetAssigneeAuthorizationInformationMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates the identity provider connector
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse createIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateIdentityProviderConnectorMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists identity provider connectors
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse listIdentityProviderConnectors(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListIdentityProviderConnectorsMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes identity provider connector
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse deleteIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteIdentityProviderConnectorMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describes an identity provider connector
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse describeIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeIdentityProviderConnectorMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update an identity provider connector
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse updateIdentityProviderConnector(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest request) {
      return blockingUnaryCall(
          getChannel(), getUpdateIdentityProviderConnectorMethodHelper(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set whether login using Cloudera SSO is enabled.
     * </pre>
     */
    public com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse setClouderaSSOLoginEnabled(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest request) {
      return blockingUnaryCall(
          getChannel(), getSetClouderaSSOLoginEnabledMethodHelper(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * For future compatibility, all rpcs must take a request and return a response
   * even if there is initially no content for these messages.
   * </pre>
   */
  public static final class UserManagementFutureStub extends io.grpc.stub.AbstractStub<UserManagementFutureStub> {
    private UserManagementFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private UserManagementFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UserManagementFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new UserManagementFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in a regular account.
     * A user record will be created if necessary.
     * The account record must alreeady exist.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginResponse> interactiveLogin(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getInteractiveLoginMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Handles an interactive login for a user in a trial account.
     * User and account records will be created if necessary.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse> interactiveLoginTrial(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getInteractiveLoginTrialMethodHelper(), getCallOptions()), request);
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
      return futureUnaryCall(
          getChannel().newCall(getInteractiveLogin3rdPartyMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes the account from Altus tests only.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountResponse> deleteAccount(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccountRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteAccountMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete an actor from Altus.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorResponse> deleteActor(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteActorRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteActorMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete trial user from Altus for tests only.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserResponse> deleteTrialUser(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteTrialUserRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteTrialUserMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Gets all the information associated with an access key needed to verify a
     * request signature produced that key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataResponse> getAccessKeyVerificationData(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyVerificationDataRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAccessKeyVerificationDataMethodHelper(), getCallOptions()), request);
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
      return futureUnaryCall(
          getChannel().newCall(getVerifyInteractiveUserSessionTokenMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Authenticate an actor. This method currently supports session tokens and
     * access key authentication.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse> authenticate(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAuthenticateMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Handles access key usage, marking the last time it was used and the last
     * service on which it was used.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse> accessKeyUsage(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAccessKeyUsageMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get user.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse> getUser(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetUserMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List users.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse> listUsers(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListUsersMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Find users.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailResponse> findUsersByEmail(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.FindUsersByEmailRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getFindUsersByEmailMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates a new access key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse> createAccessKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAccessKeyMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Updates an access key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyResponse> updateAccessKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateAccessKeyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateAccessKeyMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes an access key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse> deleteAccessKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteAccessKeyMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get access key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyResponse> getAccessKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccessKeyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAccessKeyMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List access keys.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse> listAccessKeys(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListAccessKeysMethodHelper(), getCallOptions()), request);
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
     * Get account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse> getAccount(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAccountMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsResponse> listAccounts(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccountsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListAccountsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the rights for an actor.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse> getRights(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetRightsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Checks if an actor has the input rights on the input resources.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse> checkRights(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCheckRightsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create an account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountResponse> createAccount(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccountRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAccountMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Grant Entitlement to an Account
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse> grantEntitlement(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGrantEntitlementMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Revoke Entitlement from an Account
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse> revokeEntitlement(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRevokeEntitlementMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Assign a role to an assignee
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse> assignRole(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAssignRoleMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unassign a role from an assignee
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse> unassignRole(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getUnassignRoleMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List the assigned roles for an assignee:
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesResponse> listAssignedRoles(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedRolesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListAssignedRolesMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Assign a resource role to an assignee
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse> assignResourceRole(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAssignResourceRoleMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unassign a resource role from an assignee
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse> unassignResourceRole(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getUnassignResourceRoleMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List the assigned resource roles for an assignee:
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesResponse> listAssignedResourceRoles(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAssignedResourceRolesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListAssignedResourceRolesMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List roles.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse> listRoles(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListRolesMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List resource roles.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesResponse> listResourceRoles(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRolesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListResourceRolesMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List resource assignees.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse> listResourceAssignees(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListResourceAssigneesMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Update Cloudera Manager License Key
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyResponse> updateClouderaManagerLicenseKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateClouderaManagerLicenseKeyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateClouderaManagerLicenseKeyMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Initiates a support case creation pipeline.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseResponse> initiateSupportCase(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InitiateSupportCaseRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getInitiateSupportCaseMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Notify that a resource was deleted. All resource role assignments
     * associated with this resource will be deleted.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse> notifyResourceDeleted(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getNotifyResourceDeletedMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create a machine user
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse> createMachineUser(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateMachineUserMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * list machine users
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse> listMachineUsers(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListMachineUsersMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete a machine user
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse> deleteMachineUser(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteMachineUserMethodHelper(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsResponse> listResourceRoleAssignments(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceRoleAssignmentsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListResourceRoleAssignmentsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Sets the account messages.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesResponse> setAccountMessages(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetAccountMessagesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSetAccountMessagesMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Terms acceptance
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsResponse> acceptTerms(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AcceptTermsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAcceptTermsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Clearing accepted terms. This will clear the accepted terms with the
     * same version as the current Terms found in the TermsProvider.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsResponse> clearAcceptedTerms(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ClearAcceptedTermsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getClearAcceptedTermsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Terms description
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsResponse> describeTerms(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeTermsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeTermsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Terms listing
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse> listTerms(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListTermsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Entitlements listing
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsResponse> listEntitlements(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListEntitlementsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListEntitlementsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set terms acceptance expiry
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryResponse> setTermsAcceptanceExpiry(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetTermsAcceptanceExpiryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSetTermsAcceptanceExpiryMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Confirm whether Altus account and Azure Subscription Id
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedResponse> confirmAzureSubscriptionVerified(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ConfirmAzureSubscriptionVerifiedRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getConfirmAzureSubscriptionVerifiedMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Insert Azure Subscriptions
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationResponse> insertAzureSubscription(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InsertAzureSubscriptionInformationRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getInsertAzureSubscriptionMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create group
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse> createGroup(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateGroupMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete group
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse> deleteGroup(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteGroupMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List groups
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse> listGroups(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListGroupsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Add member to group
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse> addMemberToGroup(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAddMemberToGroupMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Remove member from group
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse> removeMemberFromGroup(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRemoveMemberFromGroupMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List group members
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse> listGroupMembers(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListGroupMembersMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List of groupCRNs corresponding to the member
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse> listGroupsForMember(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListGroupsForMemberMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates a new cluster ssh private key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyResponse> createClusterSshPrivateKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateClusterSshPrivateKeyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateClusterSshPrivateKeyMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get cluster ssh private key.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyResponse> getClusterSshPrivateKey(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetClusterSshPrivateKeyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetClusterSshPrivateKeyMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the authorization information about an assignee.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse> getAssigneeAuthorizationInformation(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssigneeAuthorizationInformationMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates the identity provider connector
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorResponse> createIdentityProviderConnector(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateIdentityProviderConnectorRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateIdentityProviderConnectorMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists identity provider connectors
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsResponse> listIdentityProviderConnectors(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListIdentityProviderConnectorsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListIdentityProviderConnectorsMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes identity provider connector
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorResponse> deleteIdentityProviderConnector(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteIdentityProviderConnectorRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteIdentityProviderConnectorMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describes an identity provider connector
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorResponse> describeIdentityProviderConnector(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DescribeIdentityProviderConnectorRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeIdentityProviderConnectorMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Update an identity provider connector
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorResponse> updateIdentityProviderConnector(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UpdateIdentityProviderConnectorRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateIdentityProviderConnectorMethodHelper(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set whether login using Cloudera SSO is enabled.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledResponse> setClouderaSSOLoginEnabled(
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetClouderaSSOLoginEnabledRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSetClouderaSSOLoginEnabledMethodHelper(), getCallOptions()), request);
    }
  }

  private static final int METHODID_INTERACTIVE_LOGIN = 0;
  private static final int METHODID_INTERACTIVE_LOGIN_TRIAL = 1;
  private static final int METHODID_INTERACTIVE_LOGIN3RD_PARTY = 2;
  private static final int METHODID_DELETE_ACCOUNT = 3;
  private static final int METHODID_DELETE_ACTOR = 4;
  private static final int METHODID_DELETE_TRIAL_USER = 5;
  private static final int METHODID_GET_ACCESS_KEY_VERIFICATION_DATA = 6;
  private static final int METHODID_VERIFY_INTERACTIVE_USER_SESSION_TOKEN = 7;
  private static final int METHODID_AUTHENTICATE = 8;
  private static final int METHODID_ACCESS_KEY_USAGE = 9;
  private static final int METHODID_GET_USER = 10;
  private static final int METHODID_LIST_USERS = 11;
  private static final int METHODID_FIND_USERS_BY_EMAIL = 12;
  private static final int METHODID_CREATE_ACCESS_KEY = 13;
  private static final int METHODID_UPDATE_ACCESS_KEY = 14;
  private static final int METHODID_DELETE_ACCESS_KEY = 15;
  private static final int METHODID_GET_ACCESS_KEY = 16;
  private static final int METHODID_LIST_ACCESS_KEYS = 17;
  private static final int METHODID_GET_VERSION = 18;
  private static final int METHODID_GET_ACCOUNT = 19;
  private static final int METHODID_LIST_ACCOUNTS = 20;
  private static final int METHODID_GET_RIGHTS = 21;
  private static final int METHODID_CHECK_RIGHTS = 22;
  private static final int METHODID_CREATE_ACCOUNT = 23;
  private static final int METHODID_GRANT_ENTITLEMENT = 24;
  private static final int METHODID_REVOKE_ENTITLEMENT = 25;
  private static final int METHODID_ASSIGN_ROLE = 26;
  private static final int METHODID_UNASSIGN_ROLE = 27;
  private static final int METHODID_LIST_ASSIGNED_ROLES = 28;
  private static final int METHODID_ASSIGN_RESOURCE_ROLE = 29;
  private static final int METHODID_UNASSIGN_RESOURCE_ROLE = 30;
  private static final int METHODID_LIST_ASSIGNED_RESOURCE_ROLES = 31;
  private static final int METHODID_LIST_ROLES = 32;
  private static final int METHODID_LIST_RESOURCE_ROLES = 33;
  private static final int METHODID_LIST_RESOURCE_ASSIGNEES = 34;
  private static final int METHODID_UPDATE_CLOUDERA_MANAGER_LICENSE_KEY = 35;
  private static final int METHODID_INITIATE_SUPPORT_CASE = 36;
  private static final int METHODID_NOTIFY_RESOURCE_DELETED = 37;
  private static final int METHODID_CREATE_MACHINE_USER = 38;
  private static final int METHODID_LIST_MACHINE_USERS = 39;
  private static final int METHODID_DELETE_MACHINE_USER = 40;
  private static final int METHODID_LIST_RESOURCE_ROLE_ASSIGNMENTS = 41;
  private static final int METHODID_SET_ACCOUNT_MESSAGES = 42;
  private static final int METHODID_ACCEPT_TERMS = 43;
  private static final int METHODID_CLEAR_ACCEPTED_TERMS = 44;
  private static final int METHODID_DESCRIBE_TERMS = 45;
  private static final int METHODID_LIST_TERMS = 46;
  private static final int METHODID_LIST_ENTITLEMENTS = 47;
  private static final int METHODID_SET_TERMS_ACCEPTANCE_EXPIRY = 48;
  private static final int METHODID_CONFIRM_AZURE_SUBSCRIPTION_VERIFIED = 49;
  private static final int METHODID_INSERT_AZURE_SUBSCRIPTION = 50;
  private static final int METHODID_CREATE_GROUP = 51;
  private static final int METHODID_DELETE_GROUP = 52;
  private static final int METHODID_LIST_GROUPS = 53;
  private static final int METHODID_ADD_MEMBER_TO_GROUP = 54;
  private static final int METHODID_REMOVE_MEMBER_FROM_GROUP = 55;
  private static final int METHODID_LIST_GROUP_MEMBERS = 56;
  private static final int METHODID_LIST_GROUPS_FOR_MEMBER = 57;
  private static final int METHODID_CREATE_CLUSTER_SSH_PRIVATE_KEY = 58;
  private static final int METHODID_GET_CLUSTER_SSH_PRIVATE_KEY = 59;
  private static final int METHODID_GET_ASSIGNEE_AUTHORIZATION_INFORMATION = 60;
  private static final int METHODID_CREATE_IDENTITY_PROVIDER_CONNECTOR = 61;
  private static final int METHODID_LIST_IDENTITY_PROVIDER_CONNECTORS = 62;
  private static final int METHODID_DELETE_IDENTITY_PROVIDER_CONNECTOR = 63;
  private static final int METHODID_DESCRIBE_IDENTITY_PROVIDER_CONNECTOR = 64;
  private static final int METHODID_UPDATE_IDENTITY_PROVIDER_CONNECTOR = 65;
  private static final int METHODID_SET_CLOUDERA_SSOLOGIN_ENABLED = 66;

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
        case METHODID_INTERACTIVE_LOGIN_TRIAL:
          serviceImpl.interactiveLoginTrial((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLoginTrialResponse>) responseObserver);
          break;
        case METHODID_INTERACTIVE_LOGIN3RD_PARTY:
          serviceImpl.interactiveLogin3rdParty((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.InteractiveLogin3rdPartyResponse>) responseObserver);
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
        case METHODID_AUTHENTICATE:
          serviceImpl.authenticate((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse>) responseObserver);
          break;
        case METHODID_ACCESS_KEY_USAGE:
          serviceImpl.accessKeyUsage((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyUsageResponse>) responseObserver);
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
        case METHODID_GRANT_ENTITLEMENT:
          serviceImpl.grantEntitlement((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse>) responseObserver);
          break;
        case METHODID_REVOKE_ENTITLEMENT:
          serviceImpl.revokeEntitlement((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse>) responseObserver);
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
        case METHODID_LIST_GROUPS:
          serviceImpl.listGroups((com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse>) responseObserver);
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
              .addMethod(getInteractiveLoginMethodHelper())
              .addMethod(getInteractiveLoginTrialMethodHelper())
              .addMethod(getInteractiveLogin3rdPartyMethodHelper())
              .addMethod(getDeleteAccountMethodHelper())
              .addMethod(getDeleteActorMethodHelper())
              .addMethod(getDeleteTrialUserMethodHelper())
              .addMethod(getGetAccessKeyVerificationDataMethodHelper())
              .addMethod(getVerifyInteractiveUserSessionTokenMethodHelper())
              .addMethod(getAuthenticateMethodHelper())
              .addMethod(getAccessKeyUsageMethodHelper())
              .addMethod(getGetUserMethodHelper())
              .addMethod(getListUsersMethodHelper())
              .addMethod(getFindUsersByEmailMethodHelper())
              .addMethod(getCreateAccessKeyMethodHelper())
              .addMethod(getUpdateAccessKeyMethodHelper())
              .addMethod(getDeleteAccessKeyMethodHelper())
              .addMethod(getGetAccessKeyMethodHelper())
              .addMethod(getListAccessKeysMethodHelper())
              .addMethod(getGetVersionMethodHelper())
              .addMethod(getGetAccountMethodHelper())
              .addMethod(getListAccountsMethodHelper())
              .addMethod(getGetRightsMethodHelper())
              .addMethod(getCheckRightsMethodHelper())
              .addMethod(getCreateAccountMethodHelper())
              .addMethod(getGrantEntitlementMethodHelper())
              .addMethod(getRevokeEntitlementMethodHelper())
              .addMethod(getAssignRoleMethodHelper())
              .addMethod(getUnassignRoleMethodHelper())
              .addMethod(getListAssignedRolesMethodHelper())
              .addMethod(getAssignResourceRoleMethodHelper())
              .addMethod(getUnassignResourceRoleMethodHelper())
              .addMethod(getListAssignedResourceRolesMethodHelper())
              .addMethod(getListRolesMethodHelper())
              .addMethod(getListResourceRolesMethodHelper())
              .addMethod(getListResourceAssigneesMethodHelper())
              .addMethod(getUpdateClouderaManagerLicenseKeyMethodHelper())
              .addMethod(getInitiateSupportCaseMethodHelper())
              .addMethod(getNotifyResourceDeletedMethodHelper())
              .addMethod(getCreateMachineUserMethodHelper())
              .addMethod(getListMachineUsersMethodHelper())
              .addMethod(getDeleteMachineUserMethodHelper())
              .addMethod(getListResourceRoleAssignmentsMethodHelper())
              .addMethod(getSetAccountMessagesMethodHelper())
              .addMethod(getAcceptTermsMethodHelper())
              .addMethod(getClearAcceptedTermsMethodHelper())
              .addMethod(getDescribeTermsMethodHelper())
              .addMethod(getListTermsMethodHelper())
              .addMethod(getListEntitlementsMethodHelper())
              .addMethod(getSetTermsAcceptanceExpiryMethodHelper())
              .addMethod(getConfirmAzureSubscriptionVerifiedMethodHelper())
              .addMethod(getInsertAzureSubscriptionMethodHelper())
              .addMethod(getCreateGroupMethodHelper())
              .addMethod(getDeleteGroupMethodHelper())
              .addMethod(getListGroupsMethodHelper())
              .addMethod(getAddMemberToGroupMethodHelper())
              .addMethod(getRemoveMemberFromGroupMethodHelper())
              .addMethod(getListGroupMembersMethodHelper())
              .addMethod(getListGroupsForMemberMethodHelper())
              .addMethod(getCreateClusterSshPrivateKeyMethodHelper())
              .addMethod(getGetClusterSshPrivateKeyMethodHelper())
              .addMethod(getGetAssigneeAuthorizationInformationMethodHelper())
              .addMethod(getCreateIdentityProviderConnectorMethodHelper())
              .addMethod(getListIdentityProviderConnectorsMethodHelper())
              .addMethod(getDeleteIdentityProviderConnectorMethodHelper())
              .addMethod(getDescribeIdentityProviderConnectorMethodHelper())
              .addMethod(getUpdateIdentityProviderConnectorMethodHelper())
              .addMethod(getSetClouderaSSOLoginEnabledMethodHelper())
              .build();
        }
      }
    }
    return result;
  }
}
