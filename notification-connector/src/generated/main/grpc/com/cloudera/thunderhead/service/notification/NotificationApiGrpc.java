package com.cloudera.thunderhead.service.notification;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Notification API Service :: Provides an interface to the notification service.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class NotificationApiGrpc {

  private NotificationApiGrpc() {}

  public static final java.lang.String SERVICE_NAME = "notificationapi.NotificationApi";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse> getListNotificationMessagesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListNotificationMessages",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse> getListNotificationMessagesMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse> getListNotificationMessagesMethod;
    if ((getListNotificationMessagesMethod = NotificationApiGrpc.getListNotificationMessagesMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getListNotificationMessagesMethod = NotificationApiGrpc.getListNotificationMessagesMethod) == null) {
          NotificationApiGrpc.getListNotificationMessagesMethod = getListNotificationMessagesMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListNotificationMessages"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("ListNotificationMessages"))
              .build();
        }
      }
    }
    return getListNotificationMessagesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse> getListBroadcastNotificationsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListBroadcastNotifications",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse> getListBroadcastNotificationsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse> getListBroadcastNotificationsMethod;
    if ((getListBroadcastNotificationsMethod = NotificationApiGrpc.getListBroadcastNotificationsMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getListBroadcastNotificationsMethod = NotificationApiGrpc.getListBroadcastNotificationsMethod) == null) {
          NotificationApiGrpc.getListBroadcastNotificationsMethod = getListBroadcastNotificationsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListBroadcastNotifications"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("ListBroadcastNotifications"))
              .build();
        }
      }
    }
    return getListBroadcastNotificationsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse> getListResourceNotificationsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListResourceNotifications",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse> getListResourceNotificationsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse> getListResourceNotificationsMethod;
    if ((getListResourceNotificationsMethod = NotificationApiGrpc.getListResourceNotificationsMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getListResourceNotificationsMethod = NotificationApiGrpc.getListResourceNotificationsMethod) == null) {
          NotificationApiGrpc.getListResourceNotificationsMethod = getListResourceNotificationsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListResourceNotifications"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("ListResourceNotifications"))
              .build();
        }
      }
    }
    return getListResourceNotificationsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse> getListBroadcastEventCatalogMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListBroadcastEventCatalog",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse> getListBroadcastEventCatalogMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse> getListBroadcastEventCatalogMethod;
    if ((getListBroadcastEventCatalogMethod = NotificationApiGrpc.getListBroadcastEventCatalogMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getListBroadcastEventCatalogMethod = NotificationApiGrpc.getListBroadcastEventCatalogMethod) == null) {
          NotificationApiGrpc.getListBroadcastEventCatalogMethod = getListBroadcastEventCatalogMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListBroadcastEventCatalog"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("ListBroadcastEventCatalog"))
              .build();
        }
      }
    }
    return getListBroadcastEventCatalogMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse> getListResourceEventCatalogMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListResourceEventCatalog",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse> getListResourceEventCatalogMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse> getListResourceEventCatalogMethod;
    if ((getListResourceEventCatalogMethod = NotificationApiGrpc.getListResourceEventCatalogMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getListResourceEventCatalogMethod = NotificationApiGrpc.getListResourceEventCatalogMethod) == null) {
          NotificationApiGrpc.getListResourceEventCatalogMethod = getListResourceEventCatalogMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListResourceEventCatalog"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("ListResourceEventCatalog"))
              .build();
        }
      }
    }
    return getListResourceEventCatalogMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse> getMarkResourceNotificationsAsReadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MarkResourceNotificationsAsRead",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse> getMarkResourceNotificationsAsReadMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse> getMarkResourceNotificationsAsReadMethod;
    if ((getMarkResourceNotificationsAsReadMethod = NotificationApiGrpc.getMarkResourceNotificationsAsReadMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getMarkResourceNotificationsAsReadMethod = NotificationApiGrpc.getMarkResourceNotificationsAsReadMethod) == null) {
          NotificationApiGrpc.getMarkResourceNotificationsAsReadMethod = getMarkResourceNotificationsAsReadMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MarkResourceNotificationsAsRead"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("MarkResourceNotificationsAsRead"))
              .build();
        }
      }
    }
    return getMarkResourceNotificationsAsReadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse> getCreateOrUpdateResourceSubscriptionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateOrUpdateResourceSubscription",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse> getCreateOrUpdateResourceSubscriptionMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse> getCreateOrUpdateResourceSubscriptionMethod;
    if ((getCreateOrUpdateResourceSubscriptionMethod = NotificationApiGrpc.getCreateOrUpdateResourceSubscriptionMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getCreateOrUpdateResourceSubscriptionMethod = NotificationApiGrpc.getCreateOrUpdateResourceSubscriptionMethod) == null) {
          NotificationApiGrpc.getCreateOrUpdateResourceSubscriptionMethod = getCreateOrUpdateResourceSubscriptionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateOrUpdateResourceSubscription"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("CreateOrUpdateResourceSubscription"))
              .build();
        }
      }
    }
    return getCreateOrUpdateResourceSubscriptionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse> getCreateOrUpdateBroadcastSubscriptionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateOrUpdateBroadcastSubscription",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse> getCreateOrUpdateBroadcastSubscriptionMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse> getCreateOrUpdateBroadcastSubscriptionMethod;
    if ((getCreateOrUpdateBroadcastSubscriptionMethod = NotificationApiGrpc.getCreateOrUpdateBroadcastSubscriptionMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getCreateOrUpdateBroadcastSubscriptionMethod = NotificationApiGrpc.getCreateOrUpdateBroadcastSubscriptionMethod) == null) {
          NotificationApiGrpc.getCreateOrUpdateBroadcastSubscriptionMethod = getCreateOrUpdateBroadcastSubscriptionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateOrUpdateBroadcastSubscription"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("CreateOrUpdateBroadcastSubscription"))
              .build();
        }
      }
    }
    return getCreateOrUpdateBroadcastSubscriptionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse> getGetResourceSubscriptionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetResourceSubscription",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse> getGetResourceSubscriptionMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse> getGetResourceSubscriptionMethod;
    if ((getGetResourceSubscriptionMethod = NotificationApiGrpc.getGetResourceSubscriptionMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getGetResourceSubscriptionMethod = NotificationApiGrpc.getGetResourceSubscriptionMethod) == null) {
          NotificationApiGrpc.getGetResourceSubscriptionMethod = getGetResourceSubscriptionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetResourceSubscription"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("GetResourceSubscription"))
              .build();
        }
      }
    }
    return getGetResourceSubscriptionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse> getGetBroadcastSubscriptionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBroadcastSubscription",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse> getGetBroadcastSubscriptionMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse> getGetBroadcastSubscriptionMethod;
    if ((getGetBroadcastSubscriptionMethod = NotificationApiGrpc.getGetBroadcastSubscriptionMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getGetBroadcastSubscriptionMethod = NotificationApiGrpc.getGetBroadcastSubscriptionMethod) == null) {
          NotificationApiGrpc.getGetBroadcastSubscriptionMethod = getGetBroadcastSubscriptionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetBroadcastSubscription"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("GetBroadcastSubscription"))
              .build();
        }
      }
    }
    return getGetBroadcastSubscriptionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse> getDeleteResourceSubscriptionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteResourceSubscription",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse> getDeleteResourceSubscriptionMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse> getDeleteResourceSubscriptionMethod;
    if ((getDeleteResourceSubscriptionMethod = NotificationApiGrpc.getDeleteResourceSubscriptionMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getDeleteResourceSubscriptionMethod = NotificationApiGrpc.getDeleteResourceSubscriptionMethod) == null) {
          NotificationApiGrpc.getDeleteResourceSubscriptionMethod = getDeleteResourceSubscriptionMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteResourceSubscription"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("DeleteResourceSubscription"))
              .build();
        }
      }
    }
    return getDeleteResourceSubscriptionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse> getListResourceSubscriptionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListResourceSubscriptions",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse> getListResourceSubscriptionsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse> getListResourceSubscriptionsMethod;
    if ((getListResourceSubscriptionsMethod = NotificationApiGrpc.getListResourceSubscriptionsMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getListResourceSubscriptionsMethod = NotificationApiGrpc.getListResourceSubscriptionsMethod) == null) {
          NotificationApiGrpc.getListResourceSubscriptionsMethod = getListResourceSubscriptionsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListResourceSubscriptions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("ListResourceSubscriptions"))
              .build();
        }
      }
    }
    return getListResourceSubscriptionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse> getCreateOrUpdateDistributionListMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateOrUpdateDistributionList",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse> getCreateOrUpdateDistributionListMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse> getCreateOrUpdateDistributionListMethod;
    if ((getCreateOrUpdateDistributionListMethod = NotificationApiGrpc.getCreateOrUpdateDistributionListMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getCreateOrUpdateDistributionListMethod = NotificationApiGrpc.getCreateOrUpdateDistributionListMethod) == null) {
          NotificationApiGrpc.getCreateOrUpdateDistributionListMethod = getCreateOrUpdateDistributionListMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateOrUpdateDistributionList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("CreateOrUpdateDistributionList"))
              .build();
        }
      }
    }
    return getCreateOrUpdateDistributionListMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse> getListDistributionListsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListDistributionLists",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse> getListDistributionListsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse> getListDistributionListsMethod;
    if ((getListDistributionListsMethod = NotificationApiGrpc.getListDistributionListsMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getListDistributionListsMethod = NotificationApiGrpc.getListDistributionListsMethod) == null) {
          NotificationApiGrpc.getListDistributionListsMethod = getListDistributionListsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListDistributionLists"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("ListDistributionLists"))
              .build();
        }
      }
    }
    return getListDistributionListsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse> getDeleteDistributionListMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteDistributionList",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse> getDeleteDistributionListMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse> getDeleteDistributionListMethod;
    if ((getDeleteDistributionListMethod = NotificationApiGrpc.getDeleteDistributionListMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getDeleteDistributionListMethod = NotificationApiGrpc.getDeleteDistributionListMethod) == null) {
          NotificationApiGrpc.getDeleteDistributionListMethod = getDeleteDistributionListMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteDistributionList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("DeleteDistributionList"))
              .build();
        }
      }
    }
    return getDeleteDistributionListMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse> getGetDistributionListMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetDistributionList",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse> getGetDistributionListMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse> getGetDistributionListMethod;
    if ((getGetDistributionListMethod = NotificationApiGrpc.getGetDistributionListMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getGetDistributionListMethod = NotificationApiGrpc.getGetDistributionListMethod) == null) {
          NotificationApiGrpc.getGetDistributionListMethod = getGetDistributionListMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetDistributionList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("GetDistributionList"))
              .build();
        }
      }
    }
    return getGetDistributionListMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse> getCreateOrUpdateAccountMetadataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateOrUpdateAccountMetadata",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse> getCreateOrUpdateAccountMetadataMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse> getCreateOrUpdateAccountMetadataMethod;
    if ((getCreateOrUpdateAccountMetadataMethod = NotificationApiGrpc.getCreateOrUpdateAccountMetadataMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getCreateOrUpdateAccountMetadataMethod = NotificationApiGrpc.getCreateOrUpdateAccountMetadataMethod) == null) {
          NotificationApiGrpc.getCreateOrUpdateAccountMetadataMethod = getCreateOrUpdateAccountMetadataMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateOrUpdateAccountMetadata"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("CreateOrUpdateAccountMetadata"))
              .build();
        }
      }
    }
    return getCreateOrUpdateAccountMetadataMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse> getGetAccountMetadataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAccountMetadata",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse> getGetAccountMetadataMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse> getGetAccountMetadataMethod;
    if ((getGetAccountMetadataMethod = NotificationApiGrpc.getGetAccountMetadataMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getGetAccountMetadataMethod = NotificationApiGrpc.getGetAccountMetadataMethod) == null) {
          NotificationApiGrpc.getGetAccountMetadataMethod = getGetAccountMetadataMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAccountMetadata"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("GetAccountMetadata"))
              .build();
        }
      }
    }
    return getGetAccountMetadataMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse> getDeleteAppRegistrationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteAppRegistration",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse> getDeleteAppRegistrationMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse> getDeleteAppRegistrationMethod;
    if ((getDeleteAppRegistrationMethod = NotificationApiGrpc.getDeleteAppRegistrationMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getDeleteAppRegistrationMethod = NotificationApiGrpc.getDeleteAppRegistrationMethod) == null) {
          NotificationApiGrpc.getDeleteAppRegistrationMethod = getDeleteAppRegistrationMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteAppRegistration"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("DeleteAppRegistration"))
              .build();
        }
      }
    }
    return getDeleteAppRegistrationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse> getGetOAuthUrlMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetOAuthUrl",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse> getGetOAuthUrlMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse> getGetOAuthUrlMethod;
    if ((getGetOAuthUrlMethod = NotificationApiGrpc.getGetOAuthUrlMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getGetOAuthUrlMethod = NotificationApiGrpc.getGetOAuthUrlMethod) == null) {
          NotificationApiGrpc.getGetOAuthUrlMethod = getGetOAuthUrlMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetOAuthUrl"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("GetOAuthUrl"))
              .build();
        }
      }
    }
    return getGetOAuthUrlMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse> getGetAppRegistrationStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAppRegistrationStatus",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse> getGetAppRegistrationStatusMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse> getGetAppRegistrationStatusMethod;
    if ((getGetAppRegistrationStatusMethod = NotificationApiGrpc.getGetAppRegistrationStatusMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getGetAppRegistrationStatusMethod = NotificationApiGrpc.getGetAppRegistrationStatusMethod) == null) {
          NotificationApiGrpc.getGetAppRegistrationStatusMethod = getGetAppRegistrationStatusMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAppRegistrationStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("GetAppRegistrationStatus"))
              .build();
        }
      }
    }
    return getGetAppRegistrationStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse> getUpdateAppAuthorizationCodeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateAppAuthorizationCode",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse> getUpdateAppAuthorizationCodeMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse> getUpdateAppAuthorizationCodeMethod;
    if ((getUpdateAppAuthorizationCodeMethod = NotificationApiGrpc.getUpdateAppAuthorizationCodeMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getUpdateAppAuthorizationCodeMethod = NotificationApiGrpc.getUpdateAppAuthorizationCodeMethod) == null) {
          NotificationApiGrpc.getUpdateAppAuthorizationCodeMethod = getUpdateAppAuthorizationCodeMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateAppAuthorizationCode"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("UpdateAppAuthorizationCode"))
              .build();
        }
      }
    }
    return getUpdateAppAuthorizationCodeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse> getCreateSmtpEmailConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateSmtpEmailConfig",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse> getCreateSmtpEmailConfigMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse> getCreateSmtpEmailConfigMethod;
    if ((getCreateSmtpEmailConfigMethod = NotificationApiGrpc.getCreateSmtpEmailConfigMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getCreateSmtpEmailConfigMethod = NotificationApiGrpc.getCreateSmtpEmailConfigMethod) == null) {
          NotificationApiGrpc.getCreateSmtpEmailConfigMethod = getCreateSmtpEmailConfigMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateSmtpEmailConfig"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("CreateSmtpEmailConfig"))
              .build();
        }
      }
    }
    return getCreateSmtpEmailConfigMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse> getGetSmtpEmailConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetSmtpEmailConfig",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse> getGetSmtpEmailConfigMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse> getGetSmtpEmailConfigMethod;
    if ((getGetSmtpEmailConfigMethod = NotificationApiGrpc.getGetSmtpEmailConfigMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getGetSmtpEmailConfigMethod = NotificationApiGrpc.getGetSmtpEmailConfigMethod) == null) {
          NotificationApiGrpc.getGetSmtpEmailConfigMethod = getGetSmtpEmailConfigMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetSmtpEmailConfig"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("GetSmtpEmailConfig"))
              .build();
        }
      }
    }
    return getGetSmtpEmailConfigMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse> getDeleteSmtpEmailConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteSmtpEmailConfig",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse> getDeleteSmtpEmailConfigMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse> getDeleteSmtpEmailConfigMethod;
    if ((getDeleteSmtpEmailConfigMethod = NotificationApiGrpc.getDeleteSmtpEmailConfigMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getDeleteSmtpEmailConfigMethod = NotificationApiGrpc.getDeleteSmtpEmailConfigMethod) == null) {
          NotificationApiGrpc.getDeleteSmtpEmailConfigMethod = getDeleteSmtpEmailConfigMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteSmtpEmailConfig"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("DeleteSmtpEmailConfig"))
              .build();
        }
      }
    }
    return getDeleteSmtpEmailConfigMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse> getDeleteResourceSubscriptionsForResourceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteResourceSubscriptionsForResource",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse> getDeleteResourceSubscriptionsForResourceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse> getDeleteResourceSubscriptionsForResourceMethod;
    if ((getDeleteResourceSubscriptionsForResourceMethod = NotificationApiGrpc.getDeleteResourceSubscriptionsForResourceMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getDeleteResourceSubscriptionsForResourceMethod = NotificationApiGrpc.getDeleteResourceSubscriptionsForResourceMethod) == null) {
          NotificationApiGrpc.getDeleteResourceSubscriptionsForResourceMethod = getDeleteResourceSubscriptionsForResourceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteResourceSubscriptionsForResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("DeleteResourceSubscriptionsForResource"))
              .build();
        }
      }
    }
    return getDeleteResourceSubscriptionsForResourceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse> getDeleteDistributionListsForResourceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteDistributionListsForResource",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse> getDeleteDistributionListsForResourceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse> getDeleteDistributionListsForResourceMethod;
    if ((getDeleteDistributionListsForResourceMethod = NotificationApiGrpc.getDeleteDistributionListsForResourceMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getDeleteDistributionListsForResourceMethod = NotificationApiGrpc.getDeleteDistributionListsForResourceMethod) == null) {
          NotificationApiGrpc.getDeleteDistributionListsForResourceMethod = getDeleteDistributionListsForResourceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteDistributionListsForResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("DeleteDistributionListsForResource"))
              .build();
        }
      }
    }
    return getDeleteDistributionListsForResourceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse> getDeleteAllSubscriptionsForResourceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteAllSubscriptionsForResource",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse> getDeleteAllSubscriptionsForResourceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse> getDeleteAllSubscriptionsForResourceMethod;
    if ((getDeleteAllSubscriptionsForResourceMethod = NotificationApiGrpc.getDeleteAllSubscriptionsForResourceMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getDeleteAllSubscriptionsForResourceMethod = NotificationApiGrpc.getDeleteAllSubscriptionsForResourceMethod) == null) {
          NotificationApiGrpc.getDeleteAllSubscriptionsForResourceMethod = getDeleteAllSubscriptionsForResourceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteAllSubscriptionsForResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("DeleteAllSubscriptionsForResource"))
              .build();
        }
      }
    }
    return getDeleteAllSubscriptionsForResourceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse> getGetPublishedEventStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetPublishedEventStatus",
      requestType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest.class,
      responseType = com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest,
      com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse> getGetPublishedEventStatusMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse> getGetPublishedEventStatusMethod;
    if ((getGetPublishedEventStatusMethod = NotificationApiGrpc.getGetPublishedEventStatusMethod) == null) {
      synchronized (NotificationApiGrpc.class) {
        if ((getGetPublishedEventStatusMethod = NotificationApiGrpc.getGetPublishedEventStatusMethod) == null) {
          NotificationApiGrpc.getGetPublishedEventStatusMethod = getGetPublishedEventStatusMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest, com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetPublishedEventStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationApiMethodDescriptorSupplier("GetPublishedEventStatus"))
              .build();
        }
      }
    }
    return getGetPublishedEventStatusMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static NotificationApiStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NotificationApiStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NotificationApiStub>() {
        @java.lang.Override
        public NotificationApiStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NotificationApiStub(channel, callOptions);
        }
      };
    return NotificationApiStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static NotificationApiBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NotificationApiBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NotificationApiBlockingV2Stub>() {
        @java.lang.Override
        public NotificationApiBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NotificationApiBlockingV2Stub(channel, callOptions);
        }
      };
    return NotificationApiBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static NotificationApiBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NotificationApiBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NotificationApiBlockingStub>() {
        @java.lang.Override
        public NotificationApiBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NotificationApiBlockingStub(channel, callOptions);
        }
      };
    return NotificationApiBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static NotificationApiFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NotificationApiFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NotificationApiFutureStub>() {
        @java.lang.Override
        public NotificationApiFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NotificationApiFutureStub(channel, callOptions);
        }
      };
    return NotificationApiFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Notification API Service :: Provides an interface to the notification service.
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * ListNotificationMessages :: List all the notifications from different clients which user is authorised for.
     * </pre>
     */
    default void listNotificationMessages(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListNotificationMessagesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists all the broadcast messages. :: Lists all the broadcast messages.
     * </pre>
     */
    default void listBroadcastNotifications(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListBroadcastNotificationsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists all the resource messages :: Lists all the resource messages.
     * </pre>
     */
    default void listResourceNotifications(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListResourceNotificationsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists all the types of broadcast events available in the catalog. :: Lists all the types of broadcast events available in the catalog.
     * </pre>
     */
    default void listBroadcastEventCatalog(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListBroadcastEventCatalogMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists all the types of resource events available in the catalog. :: Lists all the types of resource events available in the catalog.
     * </pre>
     */
    default void listResourceEventCatalog(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListResourceEventCatalogMethod(), responseObserver);
    }

    /**
     * <pre>
     * Marks all the resource notifications as read :: Marks all the resource notifications as read
     * </pre>
     */
    default void markResourceNotificationsAsRead(com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getMarkResourceNotificationsAsReadMethod(), responseObserver);
    }

    /**
     * <pre>
     * CreateOrUpdateResourceSubscription :: Creates or Updates the subscription with events and channel preferences provided by user.
     * </pre>
     */
    default void createOrUpdateResourceSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateOrUpdateResourceSubscriptionMethod(), responseObserver);
    }

    /**
     * <pre>
     * CreateOrUpdateBroadcastSubscription :: Creates new broadcast subscription using the channel preferences provided by the user or updates the existing preferences in subscription with the events and channel preferences provided by the user.
     * </pre>
     */
    default void createOrUpdateBroadcastSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateOrUpdateBroadcastSubscriptionMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetResourceSubscription :: Fetches existing resource subscription with events and channel preferences.
     * </pre>
     */
    default void getResourceSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetResourceSubscriptionMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetBroadcastSubscription :: Fetches existing broadcast subscription with events and channel preferences.
     * </pre>
     */
    default void getBroadcastSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetBroadcastSubscriptionMethod(), responseObserver);
    }

    /**
     * <pre>
     * DeleteResourceSubscription :: Deletes user subscription for the resourceCrn.
     * </pre>
     */
    default void deleteResourceSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteResourceSubscriptionMethod(), responseObserver);
    }

    /**
     * <pre>
     * ListResourceSubscriptions :: Lists all resource subscriptions for a user.
     * </pre>
     */
    default void listResourceSubscriptions(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListResourceSubscriptionsMethod(), responseObserver);
    }

    /**
     * <pre>
     * CreateOrUpdateDistributionList :: Creates or updates a distribution list with the given preferences.
     * </pre>
     */
    default void createOrUpdateDistributionList(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateOrUpdateDistributionListMethod(), responseObserver);
    }

    /**
     * <pre>
     * ListDistributionLists :: Lists all distribution lists.
     * </pre>
     */
    default void listDistributionLists(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListDistributionListsMethod(), responseObserver);
    }

    /**
     * <pre>
     * DeleteDistributionList :: Deletes a distribution list.
     * </pre>
     */
    default void deleteDistributionList(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteDistributionListMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetDistributionList :: Fetches an existing distribution list with events and channel preferences.
     * </pre>
     */
    default void getDistributionList(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetDistributionListMethod(), responseObserver);
    }

    /**
     * <pre>
     * CreateOrUpdateAccountMetadata :: Creates or Updates Account metadata, such as allowed domains list for the Account.
     * </pre>
     */
    default void createOrUpdateAccountMetadata(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateOrUpdateAccountMetadataMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetAccountMetadata :: Fetches the Account metadata such as allowed email domains.
     * </pre>
     */
    default void getAccountMetadata(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAccountMetadataMethod(), responseObserver);
    }

    /**
     * <pre>
     * API endpoint used to delete app registration :: Used to delete App Registration.
     * </pre>
     */
    default void deleteAppRegistration(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteAppRegistrationMethod(), responseObserver);
    }

    /**
     * <pre>
     *GetOAuthURL :: Fetches the OAuth Request Url for the App Integrations.
     * </pre>
     */
    default void getOAuthUrl(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetOAuthUrlMethod(), responseObserver);
    }

    /**
     * <pre>
     *API endpoint to get AppRegistrationStatus :: Fetches the app registration status for the tenant.
     * </pre>
     */
    default void getAppRegistrationStatus(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAppRegistrationStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     *API endpoint to get UpdateAppAuthorizationCode :: Registers/Updates the App temporary authorization code with the Notification Service.
     * </pre>
     */
    default void updateAppAuthorizationCode(com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateAppAuthorizationCodeMethod(), responseObserver);
    }

    /**
     * <pre>
     * CreateSmtpEmailConfig :: Creates SMTP email configuration to DB.
     * </pre>
     */
    default void createSmtpEmailConfig(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateSmtpEmailConfigMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetSmtpEmailConfig :: Fetches the registered SMTP email configuration.
     * </pre>
     */
    default void getSmtpEmailConfig(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSmtpEmailConfigMethod(), responseObserver);
    }

    /**
     * <pre>
     * DeleteSmtpEmailConfig :: Deletes the registered SMTP email configuration.
     * </pre>
     */
    default void deleteSmtpEmailConfig(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteSmtpEmailConfigMethod(), responseObserver);
    }

    /**
     * <pre>
     * DeleteResourceSubscriptionsForResource :: Deletes Resource subscription by resource or user CRN.
     * </pre>
     */
    default void deleteResourceSubscriptionsForResource(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteResourceSubscriptionsForResourceMethod(), responseObserver);
    }

    /**
     * <pre>
     * DeleteDistributionListsForResource :: Deletes distribution lists for the resource CRN.
     * </pre>
     */
    default void deleteDistributionListsForResource(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteDistributionListsForResourceMethod(), responseObserver);
    }

    /**
     * <pre>
     * DeleteAllSubscriptionsForResource :: Deletes Resource subscription by resource or user CRN.
     * </pre>
     */
    default void deleteAllSubscriptionsForResource(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteAllSubscriptionsForResourceMethod(), responseObserver);
    }

    /**
     * <pre>
     * API endpoint to get PublishedEventStatus :: Fetches the published event delivery status.
     * </pre>
     */
    default void getPublishedEventStatus(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetPublishedEventStatusMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service NotificationApi.
   * <pre>
   * Notification API Service :: Provides an interface to the notification service.
   * </pre>
   */
  public static abstract class NotificationApiImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return NotificationApiGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service NotificationApi.
   * <pre>
   * Notification API Service :: Provides an interface to the notification service.
   * </pre>
   */
  public static final class NotificationApiStub
      extends io.grpc.stub.AbstractAsyncStub<NotificationApiStub> {
    private NotificationApiStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NotificationApiStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NotificationApiStub(channel, callOptions);
    }

    /**
     * <pre>
     * ListNotificationMessages :: List all the notifications from different clients which user is authorised for.
     * </pre>
     */
    public void listNotificationMessages(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListNotificationMessagesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists all the broadcast messages. :: Lists all the broadcast messages.
     * </pre>
     */
    public void listBroadcastNotifications(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListBroadcastNotificationsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists all the resource messages :: Lists all the resource messages.
     * </pre>
     */
    public void listResourceNotifications(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListResourceNotificationsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists all the types of broadcast events available in the catalog. :: Lists all the types of broadcast events available in the catalog.
     * </pre>
     */
    public void listBroadcastEventCatalog(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListBroadcastEventCatalogMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists all the types of resource events available in the catalog. :: Lists all the types of resource events available in the catalog.
     * </pre>
     */
    public void listResourceEventCatalog(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListResourceEventCatalogMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Marks all the resource notifications as read :: Marks all the resource notifications as read
     * </pre>
     */
    public void markResourceNotificationsAsRead(com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getMarkResourceNotificationsAsReadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * CreateOrUpdateResourceSubscription :: Creates or Updates the subscription with events and channel preferences provided by user.
     * </pre>
     */
    public void createOrUpdateResourceSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateOrUpdateResourceSubscriptionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * CreateOrUpdateBroadcastSubscription :: Creates new broadcast subscription using the channel preferences provided by the user or updates the existing preferences in subscription with the events and channel preferences provided by the user.
     * </pre>
     */
    public void createOrUpdateBroadcastSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateOrUpdateBroadcastSubscriptionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetResourceSubscription :: Fetches existing resource subscription with events and channel preferences.
     * </pre>
     */
    public void getResourceSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetResourceSubscriptionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetBroadcastSubscription :: Fetches existing broadcast subscription with events and channel preferences.
     * </pre>
     */
    public void getBroadcastSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetBroadcastSubscriptionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * DeleteResourceSubscription :: Deletes user subscription for the resourceCrn.
     * </pre>
     */
    public void deleteResourceSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteResourceSubscriptionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * ListResourceSubscriptions :: Lists all resource subscriptions for a user.
     * </pre>
     */
    public void listResourceSubscriptions(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListResourceSubscriptionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * CreateOrUpdateDistributionList :: Creates or updates a distribution list with the given preferences.
     * </pre>
     */
    public void createOrUpdateDistributionList(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateOrUpdateDistributionListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * ListDistributionLists :: Lists all distribution lists.
     * </pre>
     */
    public void listDistributionLists(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListDistributionListsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * DeleteDistributionList :: Deletes a distribution list.
     * </pre>
     */
    public void deleteDistributionList(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteDistributionListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetDistributionList :: Fetches an existing distribution list with events and channel preferences.
     * </pre>
     */
    public void getDistributionList(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetDistributionListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * CreateOrUpdateAccountMetadata :: Creates or Updates Account metadata, such as allowed domains list for the Account.
     * </pre>
     */
    public void createOrUpdateAccountMetadata(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateOrUpdateAccountMetadataMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetAccountMetadata :: Fetches the Account metadata such as allowed email domains.
     * </pre>
     */
    public void getAccountMetadata(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAccountMetadataMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * API endpoint used to delete app registration :: Used to delete App Registration.
     * </pre>
     */
    public void deleteAppRegistration(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteAppRegistrationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *GetOAuthURL :: Fetches the OAuth Request Url for the App Integrations.
     * </pre>
     */
    public void getOAuthUrl(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetOAuthUrlMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *API endpoint to get AppRegistrationStatus :: Fetches the app registration status for the tenant.
     * </pre>
     */
    public void getAppRegistrationStatus(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAppRegistrationStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *API endpoint to get UpdateAppAuthorizationCode :: Registers/Updates the App temporary authorization code with the Notification Service.
     * </pre>
     */
    public void updateAppAuthorizationCode(com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateAppAuthorizationCodeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * CreateSmtpEmailConfig :: Creates SMTP email configuration to DB.
     * </pre>
     */
    public void createSmtpEmailConfig(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateSmtpEmailConfigMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetSmtpEmailConfig :: Fetches the registered SMTP email configuration.
     * </pre>
     */
    public void getSmtpEmailConfig(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetSmtpEmailConfigMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * DeleteSmtpEmailConfig :: Deletes the registered SMTP email configuration.
     * </pre>
     */
    public void deleteSmtpEmailConfig(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteSmtpEmailConfigMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * DeleteResourceSubscriptionsForResource :: Deletes Resource subscription by resource or user CRN.
     * </pre>
     */
    public void deleteResourceSubscriptionsForResource(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteResourceSubscriptionsForResourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * DeleteDistributionListsForResource :: Deletes distribution lists for the resource CRN.
     * </pre>
     */
    public void deleteDistributionListsForResource(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteDistributionListsForResourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * DeleteAllSubscriptionsForResource :: Deletes Resource subscription by resource or user CRN.
     * </pre>
     */
    public void deleteAllSubscriptionsForResource(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteAllSubscriptionsForResourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * API endpoint to get PublishedEventStatus :: Fetches the published event delivery status.
     * </pre>
     */
    public void getPublishedEventStatus(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetPublishedEventStatusMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service NotificationApi.
   * <pre>
   * Notification API Service :: Provides an interface to the notification service.
   * </pre>
   */
  public static final class NotificationApiBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<NotificationApiBlockingV2Stub> {
    private NotificationApiBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NotificationApiBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NotificationApiBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * ListNotificationMessages :: List all the notifications from different clients which user is authorised for.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse listNotificationMessages(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListNotificationMessagesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the broadcast messages. :: Lists all the broadcast messages.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse listBroadcastNotifications(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListBroadcastNotificationsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the resource messages :: Lists all the resource messages.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse listResourceNotifications(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListResourceNotificationsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the types of broadcast events available in the catalog. :: Lists all the types of broadcast events available in the catalog.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse listBroadcastEventCatalog(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListBroadcastEventCatalogMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the types of resource events available in the catalog. :: Lists all the types of resource events available in the catalog.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse listResourceEventCatalog(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListResourceEventCatalogMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Marks all the resource notifications as read :: Marks all the resource notifications as read
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse markResourceNotificationsAsRead(com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getMarkResourceNotificationsAsReadMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateOrUpdateResourceSubscription :: Creates or Updates the subscription with events and channel preferences provided by user.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse createOrUpdateResourceSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateOrUpdateResourceSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateOrUpdateBroadcastSubscription :: Creates new broadcast subscription using the channel preferences provided by the user or updates the existing preferences in subscription with the events and channel preferences provided by the user.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse createOrUpdateBroadcastSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateOrUpdateBroadcastSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetResourceSubscription :: Fetches existing resource subscription with events and channel preferences.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse getResourceSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetResourceSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetBroadcastSubscription :: Fetches existing broadcast subscription with events and channel preferences.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse getBroadcastSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetBroadcastSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteResourceSubscription :: Deletes user subscription for the resourceCrn.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse deleteResourceSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteResourceSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * ListResourceSubscriptions :: Lists all resource subscriptions for a user.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse listResourceSubscriptions(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListResourceSubscriptionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateOrUpdateDistributionList :: Creates or updates a distribution list with the given preferences.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse createOrUpdateDistributionList(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateOrUpdateDistributionListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * ListDistributionLists :: Lists all distribution lists.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse listDistributionLists(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListDistributionListsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteDistributionList :: Deletes a distribution list.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse deleteDistributionList(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteDistributionListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetDistributionList :: Fetches an existing distribution list with events and channel preferences.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse getDistributionList(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetDistributionListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateOrUpdateAccountMetadata :: Creates or Updates Account metadata, such as allowed domains list for the Account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse createOrUpdateAccountMetadata(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateOrUpdateAccountMetadataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetAccountMetadata :: Fetches the Account metadata such as allowed email domains.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse getAccountMetadata(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetAccountMetadataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * API endpoint used to delete app registration :: Used to delete App Registration.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse deleteAppRegistration(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteAppRegistrationMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *GetOAuthURL :: Fetches the OAuth Request Url for the App Integrations.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse getOAuthUrl(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetOAuthUrlMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *API endpoint to get AppRegistrationStatus :: Fetches the app registration status for the tenant.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse getAppRegistrationStatus(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetAppRegistrationStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *API endpoint to get UpdateAppAuthorizationCode :: Registers/Updates the App temporary authorization code with the Notification Service.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse updateAppAuthorizationCode(com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUpdateAppAuthorizationCodeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateSmtpEmailConfig :: Creates SMTP email configuration to DB.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse createSmtpEmailConfig(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateSmtpEmailConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetSmtpEmailConfig :: Fetches the registered SMTP email configuration.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse getSmtpEmailConfig(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetSmtpEmailConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteSmtpEmailConfig :: Deletes the registered SMTP email configuration.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse deleteSmtpEmailConfig(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteSmtpEmailConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteResourceSubscriptionsForResource :: Deletes Resource subscription by resource or user CRN.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse deleteResourceSubscriptionsForResource(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteResourceSubscriptionsForResourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteDistributionListsForResource :: Deletes distribution lists for the resource CRN.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse deleteDistributionListsForResource(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteDistributionListsForResourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteAllSubscriptionsForResource :: Deletes Resource subscription by resource or user CRN.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse deleteAllSubscriptionsForResource(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteAllSubscriptionsForResourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * API endpoint to get PublishedEventStatus :: Fetches the published event delivery status.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse getPublishedEventStatus(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetPublishedEventStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service NotificationApi.
   * <pre>
   * Notification API Service :: Provides an interface to the notification service.
   * </pre>
   */
  public static final class NotificationApiBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<NotificationApiBlockingStub> {
    private NotificationApiBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NotificationApiBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NotificationApiBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * ListNotificationMessages :: List all the notifications from different clients which user is authorised for.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse listNotificationMessages(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListNotificationMessagesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the broadcast messages. :: Lists all the broadcast messages.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse listBroadcastNotifications(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListBroadcastNotificationsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the resource messages :: Lists all the resource messages.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse listResourceNotifications(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListResourceNotificationsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the types of broadcast events available in the catalog. :: Lists all the types of broadcast events available in the catalog.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse listBroadcastEventCatalog(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListBroadcastEventCatalogMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the types of resource events available in the catalog. :: Lists all the types of resource events available in the catalog.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse listResourceEventCatalog(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListResourceEventCatalogMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Marks all the resource notifications as read :: Marks all the resource notifications as read
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse markResourceNotificationsAsRead(com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getMarkResourceNotificationsAsReadMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateOrUpdateResourceSubscription :: Creates or Updates the subscription with events and channel preferences provided by user.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse createOrUpdateResourceSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateOrUpdateResourceSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateOrUpdateBroadcastSubscription :: Creates new broadcast subscription using the channel preferences provided by the user or updates the existing preferences in subscription with the events and channel preferences provided by the user.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse createOrUpdateBroadcastSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateOrUpdateBroadcastSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetResourceSubscription :: Fetches existing resource subscription with events and channel preferences.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse getResourceSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetResourceSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetBroadcastSubscription :: Fetches existing broadcast subscription with events and channel preferences.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse getBroadcastSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetBroadcastSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteResourceSubscription :: Deletes user subscription for the resourceCrn.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse deleteResourceSubscription(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteResourceSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * ListResourceSubscriptions :: Lists all resource subscriptions for a user.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse listResourceSubscriptions(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListResourceSubscriptionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateOrUpdateDistributionList :: Creates or updates a distribution list with the given preferences.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse createOrUpdateDistributionList(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateOrUpdateDistributionListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * ListDistributionLists :: Lists all distribution lists.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse listDistributionLists(com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListDistributionListsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteDistributionList :: Deletes a distribution list.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse deleteDistributionList(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteDistributionListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetDistributionList :: Fetches an existing distribution list with events and channel preferences.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse getDistributionList(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetDistributionListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateOrUpdateAccountMetadata :: Creates or Updates Account metadata, such as allowed domains list for the Account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse createOrUpdateAccountMetadata(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateOrUpdateAccountMetadataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetAccountMetadata :: Fetches the Account metadata such as allowed email domains.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse getAccountMetadata(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAccountMetadataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * API endpoint used to delete app registration :: Used to delete App Registration.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse deleteAppRegistration(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteAppRegistrationMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *GetOAuthURL :: Fetches the OAuth Request Url for the App Integrations.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse getOAuthUrl(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetOAuthUrlMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *API endpoint to get AppRegistrationStatus :: Fetches the app registration status for the tenant.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse getAppRegistrationStatus(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAppRegistrationStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *API endpoint to get UpdateAppAuthorizationCode :: Registers/Updates the App temporary authorization code with the Notification Service.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse updateAppAuthorizationCode(com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateAppAuthorizationCodeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateSmtpEmailConfig :: Creates SMTP email configuration to DB.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse createSmtpEmailConfig(com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateSmtpEmailConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetSmtpEmailConfig :: Fetches the registered SMTP email configuration.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse getSmtpEmailConfig(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetSmtpEmailConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteSmtpEmailConfig :: Deletes the registered SMTP email configuration.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse deleteSmtpEmailConfig(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteSmtpEmailConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteResourceSubscriptionsForResource :: Deletes Resource subscription by resource or user CRN.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse deleteResourceSubscriptionsForResource(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteResourceSubscriptionsForResourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteDistributionListsForResource :: Deletes distribution lists for the resource CRN.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse deleteDistributionListsForResource(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteDistributionListsForResourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteAllSubscriptionsForResource :: Deletes Resource subscription by resource or user CRN.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse deleteAllSubscriptionsForResource(com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteAllSubscriptionsForResourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * API endpoint to get PublishedEventStatus :: Fetches the published event delivery status.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse getPublishedEventStatus(com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetPublishedEventStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service NotificationApi.
   * <pre>
   * Notification API Service :: Provides an interface to the notification service.
   * </pre>
   */
  public static final class NotificationApiFutureStub
      extends io.grpc.stub.AbstractFutureStub<NotificationApiFutureStub> {
    private NotificationApiFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NotificationApiFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NotificationApiFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * ListNotificationMessages :: List all the notifications from different clients which user is authorised for.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse> listNotificationMessages(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListNotificationMessagesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists all the broadcast messages. :: Lists all the broadcast messages.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse> listBroadcastNotifications(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListBroadcastNotificationsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists all the resource messages :: Lists all the resource messages.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse> listResourceNotifications(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListResourceNotificationsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists all the types of broadcast events available in the catalog. :: Lists all the types of broadcast events available in the catalog.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse> listBroadcastEventCatalog(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListBroadcastEventCatalogMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists all the types of resource events available in the catalog. :: Lists all the types of resource events available in the catalog.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse> listResourceEventCatalog(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListResourceEventCatalogMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Marks all the resource notifications as read :: Marks all the resource notifications as read
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse> markResourceNotificationsAsRead(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getMarkResourceNotificationsAsReadMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * CreateOrUpdateResourceSubscription :: Creates or Updates the subscription with events and channel preferences provided by user.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse> createOrUpdateResourceSubscription(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateOrUpdateResourceSubscriptionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * CreateOrUpdateBroadcastSubscription :: Creates new broadcast subscription using the channel preferences provided by the user or updates the existing preferences in subscription with the events and channel preferences provided by the user.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse> createOrUpdateBroadcastSubscription(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateOrUpdateBroadcastSubscriptionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetResourceSubscription :: Fetches existing resource subscription with events and channel preferences.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse> getResourceSubscription(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetResourceSubscriptionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetBroadcastSubscription :: Fetches existing broadcast subscription with events and channel preferences.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse> getBroadcastSubscription(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetBroadcastSubscriptionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * DeleteResourceSubscription :: Deletes user subscription for the resourceCrn.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse> deleteResourceSubscription(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteResourceSubscriptionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * ListResourceSubscriptions :: Lists all resource subscriptions for a user.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse> listResourceSubscriptions(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListResourceSubscriptionsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * CreateOrUpdateDistributionList :: Creates or updates a distribution list with the given preferences.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse> createOrUpdateDistributionList(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateOrUpdateDistributionListMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * ListDistributionLists :: Lists all distribution lists.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse> listDistributionLists(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListDistributionListsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * DeleteDistributionList :: Deletes a distribution list.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse> deleteDistributionList(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteDistributionListMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetDistributionList :: Fetches an existing distribution list with events and channel preferences.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse> getDistributionList(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetDistributionListMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * CreateOrUpdateAccountMetadata :: Creates or Updates Account metadata, such as allowed domains list for the Account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse> createOrUpdateAccountMetadata(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateOrUpdateAccountMetadataMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetAccountMetadata :: Fetches the Account metadata such as allowed email domains.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse> getAccountMetadata(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAccountMetadataMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * API endpoint used to delete app registration :: Used to delete App Registration.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse> deleteAppRegistration(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteAppRegistrationMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *GetOAuthURL :: Fetches the OAuth Request Url for the App Integrations.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse> getOAuthUrl(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetOAuthUrlMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *API endpoint to get AppRegistrationStatus :: Fetches the app registration status for the tenant.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse> getAppRegistrationStatus(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAppRegistrationStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *API endpoint to get UpdateAppAuthorizationCode :: Registers/Updates the App temporary authorization code with the Notification Service.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse> updateAppAuthorizationCode(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateAppAuthorizationCodeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * CreateSmtpEmailConfig :: Creates SMTP email configuration to DB.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse> createSmtpEmailConfig(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateSmtpEmailConfigMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetSmtpEmailConfig :: Fetches the registered SMTP email configuration.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse> getSmtpEmailConfig(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetSmtpEmailConfigMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * DeleteSmtpEmailConfig :: Deletes the registered SMTP email configuration.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse> deleteSmtpEmailConfig(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteSmtpEmailConfigMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * DeleteResourceSubscriptionsForResource :: Deletes Resource subscription by resource or user CRN.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse> deleteResourceSubscriptionsForResource(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteResourceSubscriptionsForResourceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * DeleteDistributionListsForResource :: Deletes distribution lists for the resource CRN.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse> deleteDistributionListsForResource(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteDistributionListsForResourceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * DeleteAllSubscriptionsForResource :: Deletes Resource subscription by resource or user CRN.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse> deleteAllSubscriptionsForResource(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteAllSubscriptionsForResourceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * API endpoint to get PublishedEventStatus :: Fetches the published event delivery status.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse> getPublishedEventStatus(
        com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetPublishedEventStatusMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_NOTIFICATION_MESSAGES = 0;
  private static final int METHODID_LIST_BROADCAST_NOTIFICATIONS = 1;
  private static final int METHODID_LIST_RESOURCE_NOTIFICATIONS = 2;
  private static final int METHODID_LIST_BROADCAST_EVENT_CATALOG = 3;
  private static final int METHODID_LIST_RESOURCE_EVENT_CATALOG = 4;
  private static final int METHODID_MARK_RESOURCE_NOTIFICATIONS_AS_READ = 5;
  private static final int METHODID_CREATE_OR_UPDATE_RESOURCE_SUBSCRIPTION = 6;
  private static final int METHODID_CREATE_OR_UPDATE_BROADCAST_SUBSCRIPTION = 7;
  private static final int METHODID_GET_RESOURCE_SUBSCRIPTION = 8;
  private static final int METHODID_GET_BROADCAST_SUBSCRIPTION = 9;
  private static final int METHODID_DELETE_RESOURCE_SUBSCRIPTION = 10;
  private static final int METHODID_LIST_RESOURCE_SUBSCRIPTIONS = 11;
  private static final int METHODID_CREATE_OR_UPDATE_DISTRIBUTION_LIST = 12;
  private static final int METHODID_LIST_DISTRIBUTION_LISTS = 13;
  private static final int METHODID_DELETE_DISTRIBUTION_LIST = 14;
  private static final int METHODID_GET_DISTRIBUTION_LIST = 15;
  private static final int METHODID_CREATE_OR_UPDATE_ACCOUNT_METADATA = 16;
  private static final int METHODID_GET_ACCOUNT_METADATA = 17;
  private static final int METHODID_DELETE_APP_REGISTRATION = 18;
  private static final int METHODID_GET_OAUTH_URL = 19;
  private static final int METHODID_GET_APP_REGISTRATION_STATUS = 20;
  private static final int METHODID_UPDATE_APP_AUTHORIZATION_CODE = 21;
  private static final int METHODID_CREATE_SMTP_EMAIL_CONFIG = 22;
  private static final int METHODID_GET_SMTP_EMAIL_CONFIG = 23;
  private static final int METHODID_DELETE_SMTP_EMAIL_CONFIG = 24;
  private static final int METHODID_DELETE_RESOURCE_SUBSCRIPTIONS_FOR_RESOURCE = 25;
  private static final int METHODID_DELETE_DISTRIBUTION_LISTS_FOR_RESOURCE = 26;
  private static final int METHODID_DELETE_ALL_SUBSCRIPTIONS_FOR_RESOURCE = 27;
  private static final int METHODID_GET_PUBLISHED_EVENT_STATUS = 28;

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
        case METHODID_LIST_NOTIFICATION_MESSAGES:
          serviceImpl.listNotificationMessages((com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse>) responseObserver);
          break;
        case METHODID_LIST_BROADCAST_NOTIFICATIONS:
          serviceImpl.listBroadcastNotifications((com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse>) responseObserver);
          break;
        case METHODID_LIST_RESOURCE_NOTIFICATIONS:
          serviceImpl.listResourceNotifications((com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse>) responseObserver);
          break;
        case METHODID_LIST_BROADCAST_EVENT_CATALOG:
          serviceImpl.listBroadcastEventCatalog((com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse>) responseObserver);
          break;
        case METHODID_LIST_RESOURCE_EVENT_CATALOG:
          serviceImpl.listResourceEventCatalog((com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse>) responseObserver);
          break;
        case METHODID_MARK_RESOURCE_NOTIFICATIONS_AS_READ:
          serviceImpl.markResourceNotificationsAsRead((com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse>) responseObserver);
          break;
        case METHODID_CREATE_OR_UPDATE_RESOURCE_SUBSCRIPTION:
          serviceImpl.createOrUpdateResourceSubscription((com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse>) responseObserver);
          break;
        case METHODID_CREATE_OR_UPDATE_BROADCAST_SUBSCRIPTION:
          serviceImpl.createOrUpdateBroadcastSubscription((com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse>) responseObserver);
          break;
        case METHODID_GET_RESOURCE_SUBSCRIPTION:
          serviceImpl.getResourceSubscription((com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse>) responseObserver);
          break;
        case METHODID_GET_BROADCAST_SUBSCRIPTION:
          serviceImpl.getBroadcastSubscription((com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse>) responseObserver);
          break;
        case METHODID_DELETE_RESOURCE_SUBSCRIPTION:
          serviceImpl.deleteResourceSubscription((com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse>) responseObserver);
          break;
        case METHODID_LIST_RESOURCE_SUBSCRIPTIONS:
          serviceImpl.listResourceSubscriptions((com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse>) responseObserver);
          break;
        case METHODID_CREATE_OR_UPDATE_DISTRIBUTION_LIST:
          serviceImpl.createOrUpdateDistributionList((com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse>) responseObserver);
          break;
        case METHODID_LIST_DISTRIBUTION_LISTS:
          serviceImpl.listDistributionLists((com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse>) responseObserver);
          break;
        case METHODID_DELETE_DISTRIBUTION_LIST:
          serviceImpl.deleteDistributionList((com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse>) responseObserver);
          break;
        case METHODID_GET_DISTRIBUTION_LIST:
          serviceImpl.getDistributionList((com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse>) responseObserver);
          break;
        case METHODID_CREATE_OR_UPDATE_ACCOUNT_METADATA:
          serviceImpl.createOrUpdateAccountMetadata((com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse>) responseObserver);
          break;
        case METHODID_GET_ACCOUNT_METADATA:
          serviceImpl.getAccountMetadata((com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse>) responseObserver);
          break;
        case METHODID_DELETE_APP_REGISTRATION:
          serviceImpl.deleteAppRegistration((com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse>) responseObserver);
          break;
        case METHODID_GET_OAUTH_URL:
          serviceImpl.getOAuthUrl((com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse>) responseObserver);
          break;
        case METHODID_GET_APP_REGISTRATION_STATUS:
          serviceImpl.getAppRegistrationStatus((com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse>) responseObserver);
          break;
        case METHODID_UPDATE_APP_AUTHORIZATION_CODE:
          serviceImpl.updateAppAuthorizationCode((com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse>) responseObserver);
          break;
        case METHODID_CREATE_SMTP_EMAIL_CONFIG:
          serviceImpl.createSmtpEmailConfig((com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse>) responseObserver);
          break;
        case METHODID_GET_SMTP_EMAIL_CONFIG:
          serviceImpl.getSmtpEmailConfig((com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse>) responseObserver);
          break;
        case METHODID_DELETE_SMTP_EMAIL_CONFIG:
          serviceImpl.deleteSmtpEmailConfig((com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse>) responseObserver);
          break;
        case METHODID_DELETE_RESOURCE_SUBSCRIPTIONS_FOR_RESOURCE:
          serviceImpl.deleteResourceSubscriptionsForResource((com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse>) responseObserver);
          break;
        case METHODID_DELETE_DISTRIBUTION_LISTS_FOR_RESOURCE:
          serviceImpl.deleteDistributionListsForResource((com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse>) responseObserver);
          break;
        case METHODID_DELETE_ALL_SUBSCRIPTIONS_FOR_RESOURCE:
          serviceImpl.deleteAllSubscriptionsForResource((com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse>) responseObserver);
          break;
        case METHODID_GET_PUBLISHED_EVENT_STATUS:
          serviceImpl.getPublishedEventStatus((com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse>) responseObserver);
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
          getListNotificationMessagesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListNotificationMessagesResponse>(
                service, METHODID_LIST_NOTIFICATION_MESSAGES)))
        .addMethod(
          getListBroadcastNotificationsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastNotificationsResponse>(
                service, METHODID_LIST_BROADCAST_NOTIFICATIONS)))
        .addMethod(
          getListResourceNotificationsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceNotificationsResponse>(
                service, METHODID_LIST_RESOURCE_NOTIFICATIONS)))
        .addMethod(
          getListBroadcastEventCatalogMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListBroadcastEventCatalogResponse>(
                service, METHODID_LIST_BROADCAST_EVENT_CATALOG)))
        .addMethod(
          getListResourceEventCatalogMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceEventCatalogResponse>(
                service, METHODID_LIST_RESOURCE_EVENT_CATALOG)))
        .addMethod(
          getMarkResourceNotificationsAsReadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.MarkResourceNotificationsAsReadResponse>(
                service, METHODID_MARK_RESOURCE_NOTIFICATIONS_AS_READ)))
        .addMethod(
          getCreateOrUpdateResourceSubscriptionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateResourceSubscriptionResponse>(
                service, METHODID_CREATE_OR_UPDATE_RESOURCE_SUBSCRIPTION)))
        .addMethod(
          getCreateOrUpdateBroadcastSubscriptionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateBroadcastSubscriptionResponse>(
                service, METHODID_CREATE_OR_UPDATE_BROADCAST_SUBSCRIPTION)))
        .addMethod(
          getGetResourceSubscriptionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetResourceSubscriptionResponse>(
                service, METHODID_GET_RESOURCE_SUBSCRIPTION)))
        .addMethod(
          getGetBroadcastSubscriptionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetBroadcastSubscriptionResponse>(
                service, METHODID_GET_BROADCAST_SUBSCRIPTION)))
        .addMethod(
          getDeleteResourceSubscriptionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionResponse>(
                service, METHODID_DELETE_RESOURCE_SUBSCRIPTION)))
        .addMethod(
          getListResourceSubscriptionsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListResourceSubscriptionsResponse>(
                service, METHODID_LIST_RESOURCE_SUBSCRIPTIONS)))
        .addMethod(
          getCreateOrUpdateDistributionListMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateDistributionListResponse>(
                service, METHODID_CREATE_OR_UPDATE_DISTRIBUTION_LIST)))
        .addMethod(
          getListDistributionListsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.ListDistributionListsResponse>(
                service, METHODID_LIST_DISTRIBUTION_LISTS)))
        .addMethod(
          getDeleteDistributionListMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListResponse>(
                service, METHODID_DELETE_DISTRIBUTION_LIST)))
        .addMethod(
          getGetDistributionListMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetDistributionListResponse>(
                service, METHODID_GET_DISTRIBUTION_LIST)))
        .addMethod(
          getCreateOrUpdateAccountMetadataMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateOrUpdateAccountMetadataResponse>(
                service, METHODID_CREATE_OR_UPDATE_ACCOUNT_METADATA)))
        .addMethod(
          getGetAccountMetadataMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAccountMetadataResponse>(
                service, METHODID_GET_ACCOUNT_METADATA)))
        .addMethod(
          getDeleteAppRegistrationMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAppRegistrationResponse>(
                service, METHODID_DELETE_APP_REGISTRATION)))
        .addMethod(
          getGetOAuthUrlMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetOAuthUrlResponse>(
                service, METHODID_GET_OAUTH_URL)))
        .addMethod(
          getGetAppRegistrationStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetAppRegistrationStatusResponse>(
                service, METHODID_GET_APP_REGISTRATION_STATUS)))
        .addMethod(
          getUpdateAppAuthorizationCodeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.UpdateAppAuthorizationCodeResponse>(
                service, METHODID_UPDATE_APP_AUTHORIZATION_CODE)))
        .addMethod(
          getCreateSmtpEmailConfigMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.CreateSmtpEmailConfigResponse>(
                service, METHODID_CREATE_SMTP_EMAIL_CONFIG)))
        .addMethod(
          getGetSmtpEmailConfigMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetSmtpEmailConfigResponse>(
                service, METHODID_GET_SMTP_EMAIL_CONFIG)))
        .addMethod(
          getDeleteSmtpEmailConfigMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteSmtpEmailConfigResponse>(
                service, METHODID_DELETE_SMTP_EMAIL_CONFIG)))
        .addMethod(
          getDeleteResourceSubscriptionsForResourceMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteResourceSubscriptionsForResourceResponse>(
                service, METHODID_DELETE_RESOURCE_SUBSCRIPTIONS_FOR_RESOURCE)))
        .addMethod(
          getDeleteDistributionListsForResourceMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteDistributionListsForResourceResponse>(
                service, METHODID_DELETE_DISTRIBUTION_LISTS_FOR_RESOURCE)))
        .addMethod(
          getDeleteAllSubscriptionsForResourceMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.DeleteAllSubscriptionsForResourceResponse>(
                service, METHODID_DELETE_ALL_SUBSCRIPTIONS_FOR_RESOURCE)))
        .addMethod(
          getGetPublishedEventStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusRequest,
              com.cloudera.thunderhead.service.notification.NotificationApiProto.GetPublishedEventStatusResponse>(
                service, METHODID_GET_PUBLISHED_EVENT_STATUS)))
        .build();
  }

  private static abstract class NotificationApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    NotificationApiBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.notification.NotificationApiProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("NotificationApi");
    }
  }

  private static final class NotificationApiFileDescriptorSupplier
      extends NotificationApiBaseDescriptorSupplier {
    NotificationApiFileDescriptorSupplier() {}
  }

  private static final class NotificationApiMethodDescriptorSupplier
      extends NotificationApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    NotificationApiMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (NotificationApiGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new NotificationApiFileDescriptorSupplier())
              .addMethod(getListNotificationMessagesMethod())
              .addMethod(getListBroadcastNotificationsMethod())
              .addMethod(getListResourceNotificationsMethod())
              .addMethod(getListBroadcastEventCatalogMethod())
              .addMethod(getListResourceEventCatalogMethod())
              .addMethod(getMarkResourceNotificationsAsReadMethod())
              .addMethod(getCreateOrUpdateResourceSubscriptionMethod())
              .addMethod(getCreateOrUpdateBroadcastSubscriptionMethod())
              .addMethod(getGetResourceSubscriptionMethod())
              .addMethod(getGetBroadcastSubscriptionMethod())
              .addMethod(getDeleteResourceSubscriptionMethod())
              .addMethod(getListResourceSubscriptionsMethod())
              .addMethod(getCreateOrUpdateDistributionListMethod())
              .addMethod(getListDistributionListsMethod())
              .addMethod(getDeleteDistributionListMethod())
              .addMethod(getGetDistributionListMethod())
              .addMethod(getCreateOrUpdateAccountMetadataMethod())
              .addMethod(getGetAccountMetadataMethod())
              .addMethod(getDeleteAppRegistrationMethod())
              .addMethod(getGetOAuthUrlMethod())
              .addMethod(getGetAppRegistrationStatusMethod())
              .addMethod(getUpdateAppAuthorizationCodeMethod())
              .addMethod(getCreateSmtpEmailConfigMethod())
              .addMethod(getGetSmtpEmailConfigMethod())
              .addMethod(getDeleteSmtpEmailConfigMethod())
              .addMethod(getDeleteResourceSubscriptionsForResourceMethod())
              .addMethod(getDeleteDistributionListsForResourceMethod())
              .addMethod(getDeleteAllSubscriptionsForResourceMethod())
              .addMethod(getGetPublishedEventStatusMethod())
              .build();
        }
      }
    }
    return result;
  }
}
