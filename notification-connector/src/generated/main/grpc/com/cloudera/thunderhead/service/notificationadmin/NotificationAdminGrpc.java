package com.cloudera.thunderhead.service.notificationadmin;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Notification Admin Service :: Notification Admin Service.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class NotificationAdminGrpc {

  private NotificationAdminGrpc() {}

  public static final java.lang.String SERVICE_NAME = "notificationadmin.NotificationAdmin";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse> getPublishBroadcastEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PublishBroadcastEvent",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse> getPublishBroadcastEventMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse> getPublishBroadcastEventMethod;
    if ((getPublishBroadcastEventMethod = NotificationAdminGrpc.getPublishBroadcastEventMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getPublishBroadcastEventMethod = NotificationAdminGrpc.getPublishBroadcastEventMethod) == null) {
          NotificationAdminGrpc.getPublishBroadcastEventMethod = getPublishBroadcastEventMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PublishBroadcastEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("PublishBroadcastEvent"))
              .build();
        }
      }
    }
    return getPublishBroadcastEventMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse> getPublishTargetedEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PublishTargetedEvent",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse> getPublishTargetedEventMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse> getPublishTargetedEventMethod;
    if ((getPublishTargetedEventMethod = NotificationAdminGrpc.getPublishTargetedEventMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getPublishTargetedEventMethod = NotificationAdminGrpc.getPublishTargetedEventMethod) == null) {
          NotificationAdminGrpc.getPublishTargetedEventMethod = getPublishTargetedEventMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PublishTargetedEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("PublishTargetedEvent"))
              .build();
        }
      }
    }
    return getPublishTargetedEventMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse> getListBroadcastEventCatalogMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListBroadcastEventCatalog",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse> getListBroadcastEventCatalogMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse> getListBroadcastEventCatalogMethod;
    if ((getListBroadcastEventCatalogMethod = NotificationAdminGrpc.getListBroadcastEventCatalogMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getListBroadcastEventCatalogMethod = NotificationAdminGrpc.getListBroadcastEventCatalogMethod) == null) {
          NotificationAdminGrpc.getListBroadcastEventCatalogMethod = getListBroadcastEventCatalogMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListBroadcastEventCatalog"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("ListBroadcastEventCatalog"))
              .build();
        }
      }
    }
    return getListBroadcastEventCatalogMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse> getListBroadcastNotificationsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListBroadcastNotifications",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse> getListBroadcastNotificationsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse> getListBroadcastNotificationsMethod;
    if ((getListBroadcastNotificationsMethod = NotificationAdminGrpc.getListBroadcastNotificationsMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getListBroadcastNotificationsMethod = NotificationAdminGrpc.getListBroadcastNotificationsMethod) == null) {
          NotificationAdminGrpc.getListBroadcastNotificationsMethod = getListBroadcastNotificationsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListBroadcastNotifications"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("ListBroadcastNotifications"))
              .build();
        }
      }
    }
    return getListBroadcastNotificationsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse> getDeletePendingBroadcastEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeletePendingBroadcastEvent",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse> getDeletePendingBroadcastEventMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse> getDeletePendingBroadcastEventMethod;
    if ((getDeletePendingBroadcastEventMethod = NotificationAdminGrpc.getDeletePendingBroadcastEventMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getDeletePendingBroadcastEventMethod = NotificationAdminGrpc.getDeletePendingBroadcastEventMethod) == null) {
          NotificationAdminGrpc.getDeletePendingBroadcastEventMethod = getDeletePendingBroadcastEventMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeletePendingBroadcastEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("DeletePendingBroadcastEvent"))
              .build();
        }
      }
    }
    return getDeletePendingBroadcastEventMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse> getListPendingBroadcastEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListPendingBroadcastEvents",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse> getListPendingBroadcastEventsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse> getListPendingBroadcastEventsMethod;
    if ((getListPendingBroadcastEventsMethod = NotificationAdminGrpc.getListPendingBroadcastEventsMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getListPendingBroadcastEventsMethod = NotificationAdminGrpc.getListPendingBroadcastEventsMethod) == null) {
          NotificationAdminGrpc.getListPendingBroadcastEventsMethod = getListPendingBroadcastEventsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListPendingBroadcastEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("ListPendingBroadcastEvents"))
              .build();
        }
      }
    }
    return getListPendingBroadcastEventsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse> getCreateOrUpdateAccountMetadataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateOrUpdateAccountMetadata",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse> getCreateOrUpdateAccountMetadataMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse> getCreateOrUpdateAccountMetadataMethod;
    if ((getCreateOrUpdateAccountMetadataMethod = NotificationAdminGrpc.getCreateOrUpdateAccountMetadataMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getCreateOrUpdateAccountMetadataMethod = NotificationAdminGrpc.getCreateOrUpdateAccountMetadataMethod) == null) {
          NotificationAdminGrpc.getCreateOrUpdateAccountMetadataMethod = getCreateOrUpdateAccountMetadataMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateOrUpdateAccountMetadata"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("CreateOrUpdateAccountMetadata"))
              .build();
        }
      }
    }
    return getCreateOrUpdateAccountMetadataMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse> getGetAccountMetadataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAccountMetadata",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse> getGetAccountMetadataMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse> getGetAccountMetadataMethod;
    if ((getGetAccountMetadataMethod = NotificationAdminGrpc.getGetAccountMetadataMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getGetAccountMetadataMethod = NotificationAdminGrpc.getGetAccountMetadataMethod) == null) {
          NotificationAdminGrpc.getGetAccountMetadataMethod = getGetAccountMetadataMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAccountMetadata"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("GetAccountMetadata"))
              .build();
        }
      }
    }
    return getGetAccountMetadataMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse> getCreateOrUpdateDistributionListMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateOrUpdateDistributionList",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse> getCreateOrUpdateDistributionListMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse> getCreateOrUpdateDistributionListMethod;
    if ((getCreateOrUpdateDistributionListMethod = NotificationAdminGrpc.getCreateOrUpdateDistributionListMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getCreateOrUpdateDistributionListMethod = NotificationAdminGrpc.getCreateOrUpdateDistributionListMethod) == null) {
          NotificationAdminGrpc.getCreateOrUpdateDistributionListMethod = getCreateOrUpdateDistributionListMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateOrUpdateDistributionList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("CreateOrUpdateDistributionList"))
              .build();
        }
      }
    }
    return getCreateOrUpdateDistributionListMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse> getListDistributionListsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListDistributionLists",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse> getListDistributionListsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse> getListDistributionListsMethod;
    if ((getListDistributionListsMethod = NotificationAdminGrpc.getListDistributionListsMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getListDistributionListsMethod = NotificationAdminGrpc.getListDistributionListsMethod) == null) {
          NotificationAdminGrpc.getListDistributionListsMethod = getListDistributionListsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListDistributionLists"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("ListDistributionLists"))
              .build();
        }
      }
    }
    return getListDistributionListsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse> getGetDistributionListMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetDistributionList",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse> getGetDistributionListMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse> getGetDistributionListMethod;
    if ((getGetDistributionListMethod = NotificationAdminGrpc.getGetDistributionListMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getGetDistributionListMethod = NotificationAdminGrpc.getGetDistributionListMethod) == null) {
          NotificationAdminGrpc.getGetDistributionListMethod = getGetDistributionListMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetDistributionList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("GetDistributionList"))
              .build();
        }
      }
    }
    return getGetDistributionListMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse> getDeleteDistributionListMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteDistributionList",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse> getDeleteDistributionListMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse> getDeleteDistributionListMethod;
    if ((getDeleteDistributionListMethod = NotificationAdminGrpc.getDeleteDistributionListMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getDeleteDistributionListMethod = NotificationAdminGrpc.getDeleteDistributionListMethod) == null) {
          NotificationAdminGrpc.getDeleteDistributionListMethod = getDeleteDistributionListMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteDistributionList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("DeleteDistributionList"))
              .build();
        }
      }
    }
    return getDeleteDistributionListMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse> getDeleteDistributionListsForResourceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteDistributionListsForResource",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse> getDeleteDistributionListsForResourceMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse> getDeleteDistributionListsForResourceMethod;
    if ((getDeleteDistributionListsForResourceMethod = NotificationAdminGrpc.getDeleteDistributionListsForResourceMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getDeleteDistributionListsForResourceMethod = NotificationAdminGrpc.getDeleteDistributionListsForResourceMethod) == null) {
          NotificationAdminGrpc.getDeleteDistributionListsForResourceMethod = getDeleteDistributionListsForResourceMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteDistributionListsForResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("DeleteDistributionListsForResource"))
              .build();
        }
      }
    }
    return getDeleteDistributionListsForResourceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse> getListResourceSubscriptionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListResourceSubscriptions",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse> getListResourceSubscriptionsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse> getListResourceSubscriptionsMethod;
    if ((getListResourceSubscriptionsMethod = NotificationAdminGrpc.getListResourceSubscriptionsMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getListResourceSubscriptionsMethod = NotificationAdminGrpc.getListResourceSubscriptionsMethod) == null) {
          NotificationAdminGrpc.getListResourceSubscriptionsMethod = getListResourceSubscriptionsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListResourceSubscriptions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("ListResourceSubscriptions"))
              .build();
        }
      }
    }
    return getListResourceSubscriptionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse> getListPublishedBroadcastNotificationsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListPublishedBroadcastNotifications",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse> getListPublishedBroadcastNotificationsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse> getListPublishedBroadcastNotificationsMethod;
    if ((getListPublishedBroadcastNotificationsMethod = NotificationAdminGrpc.getListPublishedBroadcastNotificationsMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getListPublishedBroadcastNotificationsMethod = NotificationAdminGrpc.getListPublishedBroadcastNotificationsMethod) == null) {
          NotificationAdminGrpc.getListPublishedBroadcastNotificationsMethod = getListPublishedBroadcastNotificationsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListPublishedBroadcastNotifications"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("ListPublishedBroadcastNotifications"))
              .build();
        }
      }
    }
    return getListPublishedBroadcastNotificationsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse> getListDraftBroadcastEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListDraftBroadcastEvents",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse> getListDraftBroadcastEventsMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse> getListDraftBroadcastEventsMethod;
    if ((getListDraftBroadcastEventsMethod = NotificationAdminGrpc.getListDraftBroadcastEventsMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getListDraftBroadcastEventsMethod = NotificationAdminGrpc.getListDraftBroadcastEventsMethod) == null) {
          NotificationAdminGrpc.getListDraftBroadcastEventsMethod = getListDraftBroadcastEventsMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListDraftBroadcastEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("ListDraftBroadcastEvents"))
              .build();
        }
      }
    }
    return getListDraftBroadcastEventsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse> getPublishEmailTargetedEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PublishEmailTargetedEvent",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse> getPublishEmailTargetedEventMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse> getPublishEmailTargetedEventMethod;
    if ((getPublishEmailTargetedEventMethod = NotificationAdminGrpc.getPublishEmailTargetedEventMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getPublishEmailTargetedEventMethod = NotificationAdminGrpc.getPublishEmailTargetedEventMethod) == null) {
          NotificationAdminGrpc.getPublishEmailTargetedEventMethod = getPublishEmailTargetedEventMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PublishEmailTargetedEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("PublishEmailTargetedEvent"))
              .build();
        }
      }
    }
    return getPublishEmailTargetedEventMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse> getGetPublishedEventStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetPublishedEventStatus",
      requestType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest.class,
      responseType = com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest,
      com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse> getGetPublishedEventStatusMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse> getGetPublishedEventStatusMethod;
    if ((getGetPublishedEventStatusMethod = NotificationAdminGrpc.getGetPublishedEventStatusMethod) == null) {
      synchronized (NotificationAdminGrpc.class) {
        if ((getGetPublishedEventStatusMethod = NotificationAdminGrpc.getGetPublishedEventStatusMethod) == null) {
          NotificationAdminGrpc.getGetPublishedEventStatusMethod = getGetPublishedEventStatusMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest, com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetPublishedEventStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NotificationAdminMethodDescriptorSupplier("GetPublishedEventStatus"))
              .build();
        }
      }
    }
    return getGetPublishedEventStatusMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static NotificationAdminStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NotificationAdminStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NotificationAdminStub>() {
        @java.lang.Override
        public NotificationAdminStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NotificationAdminStub(channel, callOptions);
        }
      };
    return NotificationAdminStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static NotificationAdminBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NotificationAdminBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NotificationAdminBlockingV2Stub>() {
        @java.lang.Override
        public NotificationAdminBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NotificationAdminBlockingV2Stub(channel, callOptions);
        }
      };
    return NotificationAdminBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static NotificationAdminBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NotificationAdminBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NotificationAdminBlockingStub>() {
        @java.lang.Override
        public NotificationAdminBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NotificationAdminBlockingStub(channel, callOptions);
        }
      };
    return NotificationAdminBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static NotificationAdminFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NotificationAdminFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NotificationAdminFutureStub>() {
        @java.lang.Override
        public NotificationAdminFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NotificationAdminFutureStub(channel, callOptions);
        }
      };
    return NotificationAdminFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Notification Admin Service :: Notification Admin Service.
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * PublishBroadcastEvent :: Publish the Broadcast events to all CDP users (across all tenants).
     * </pre>
     */
    default void publishBroadcastEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPublishBroadcastEventMethod(), responseObserver);
    }

    /**
     * <pre>
     * PublishTargetedEvent :: Publish the targeted event to notification service when event occur.
     * </pre>
     */
    default void publishTargetedEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPublishTargetedEventMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists all the types of broadcast events available in the catalog. :: Lists all the types of broadcast events available in the catalog.
     * </pre>
     */
    default void listBroadcastEventCatalog(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListBroadcastEventCatalogMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists all the broadcast messages. :: Lists all the broadcast messages.
     * </pre>
     */
    default void listBroadcastNotifications(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListBroadcastNotificationsMethod(), responseObserver);
    }

    /**
     * <pre>
     * DeletePendingBroadcastEvent :: Deletes broadcast event that is waiting for delivery.
     * </pre>
     */
    default void deletePendingBroadcastEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeletePendingBroadcastEventMethod(), responseObserver);
    }

    /**
     * <pre>
     * ListPendingBroadcastEvents :: Lists all pending broadcast messages scheduled for publication.
     * </pre>
     */
    default void listPendingBroadcastEvents(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListPendingBroadcastEventsMethod(), responseObserver);
    }

    /**
     * <pre>
     * CreateOrUpdateAccountMetadata :: Creates or Updates Account metadata, such as allowed domains list for the Account.
     * </pre>
     */
    default void createOrUpdateAccountMetadata(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateOrUpdateAccountMetadataMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetAccountMetadata :: Fetches the Account metadata such as allowed email domains.
     * </pre>
     */
    default void getAccountMetadata(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAccountMetadataMethod(), responseObserver);
    }

    /**
     * <pre>
     * CreateOrUpdateDistributionList :: Creates or updates a distribution list with the given preferences.
     * </pre>
     */
    default void createOrUpdateDistributionList(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateOrUpdateDistributionListMethod(), responseObserver);
    }

    /**
     * <pre>
     * ListDistributionLists :: Lists all distribution lists.
     * </pre>
     */
    default void listDistributionLists(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListDistributionListsMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetDistributionList :: Fetches an existing distribution list with events and channel preferences.
     * </pre>
     */
    default void getDistributionList(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetDistributionListMethod(), responseObserver);
    }

    /**
     * <pre>
     * DeleteDistributionList :: Deletes a distribution list.
     * </pre>
     */
    default void deleteDistributionList(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteDistributionListMethod(), responseObserver);
    }

    /**
     * <pre>
     * DeleteDistributionListsForResource :: Deletes distribution lists for the resource CRN.
     * </pre>
     */
    default void deleteDistributionListsForResource(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteDistributionListsForResourceMethod(), responseObserver);
    }

    /**
     * <pre>
     * ListResourceSubscriptions :: Lists all resource subscriptions for a resource or an account ID.
     * </pre>
     */
    default void listResourceSubscriptions(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListResourceSubscriptionsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists all the published broadcast messages. :: Lists all the published broadcast messages.
     * </pre>
     */
    default void listPublishedBroadcastNotifications(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListPublishedBroadcastNotificationsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists all the draft broadcast events. :: Lists all the draft broadcast events.
     * </pre>
     */
    default void listDraftBroadcastEvents(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListDraftBroadcastEventsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Publish targeted email based on email address. :: Publish targeted email based on email address.
     * </pre>
     */
    default void publishEmailTargetedEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPublishEmailTargetedEventMethod(), responseObserver);
    }

    /**
     * <pre>
     * API endpoint to get PublishedEventStatus :: Fetches the published event delivery status.
     * </pre>
     */
    default void getPublishedEventStatus(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetPublishedEventStatusMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service NotificationAdmin.
   * <pre>
   * Notification Admin Service :: Notification Admin Service.
   * </pre>
   */
  public static abstract class NotificationAdminImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return NotificationAdminGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service NotificationAdmin.
   * <pre>
   * Notification Admin Service :: Notification Admin Service.
   * </pre>
   */
  public static final class NotificationAdminStub
      extends io.grpc.stub.AbstractAsyncStub<NotificationAdminStub> {
    private NotificationAdminStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NotificationAdminStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NotificationAdminStub(channel, callOptions);
    }

    /**
     * <pre>
     * PublishBroadcastEvent :: Publish the Broadcast events to all CDP users (across all tenants).
     * </pre>
     */
    public void publishBroadcastEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPublishBroadcastEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * PublishTargetedEvent :: Publish the targeted event to notification service when event occur.
     * </pre>
     */
    public void publishTargetedEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPublishTargetedEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists all the types of broadcast events available in the catalog. :: Lists all the types of broadcast events available in the catalog.
     * </pre>
     */
    public void listBroadcastEventCatalog(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListBroadcastEventCatalogMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists all the broadcast messages. :: Lists all the broadcast messages.
     * </pre>
     */
    public void listBroadcastNotifications(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListBroadcastNotificationsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * DeletePendingBroadcastEvent :: Deletes broadcast event that is waiting for delivery.
     * </pre>
     */
    public void deletePendingBroadcastEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeletePendingBroadcastEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * ListPendingBroadcastEvents :: Lists all pending broadcast messages scheduled for publication.
     * </pre>
     */
    public void listPendingBroadcastEvents(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListPendingBroadcastEventsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * CreateOrUpdateAccountMetadata :: Creates or Updates Account metadata, such as allowed domains list for the Account.
     * </pre>
     */
    public void createOrUpdateAccountMetadata(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateOrUpdateAccountMetadataMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetAccountMetadata :: Fetches the Account metadata such as allowed email domains.
     * </pre>
     */
    public void getAccountMetadata(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAccountMetadataMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * CreateOrUpdateDistributionList :: Creates or updates a distribution list with the given preferences.
     * </pre>
     */
    public void createOrUpdateDistributionList(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateOrUpdateDistributionListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * ListDistributionLists :: Lists all distribution lists.
     * </pre>
     */
    public void listDistributionLists(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListDistributionListsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetDistributionList :: Fetches an existing distribution list with events and channel preferences.
     * </pre>
     */
    public void getDistributionList(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetDistributionListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * DeleteDistributionList :: Deletes a distribution list.
     * </pre>
     */
    public void deleteDistributionList(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteDistributionListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * DeleteDistributionListsForResource :: Deletes distribution lists for the resource CRN.
     * </pre>
     */
    public void deleteDistributionListsForResource(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteDistributionListsForResourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * ListResourceSubscriptions :: Lists all resource subscriptions for a resource or an account ID.
     * </pre>
     */
    public void listResourceSubscriptions(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListResourceSubscriptionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists all the published broadcast messages. :: Lists all the published broadcast messages.
     * </pre>
     */
    public void listPublishedBroadcastNotifications(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListPublishedBroadcastNotificationsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists all the draft broadcast events. :: Lists all the draft broadcast events.
     * </pre>
     */
    public void listDraftBroadcastEvents(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListDraftBroadcastEventsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Publish targeted email based on email address. :: Publish targeted email based on email address.
     * </pre>
     */
    public void publishEmailTargetedEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPublishEmailTargetedEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * API endpoint to get PublishedEventStatus :: Fetches the published event delivery status.
     * </pre>
     */
    public void getPublishedEventStatus(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetPublishedEventStatusMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service NotificationAdmin.
   * <pre>
   * Notification Admin Service :: Notification Admin Service.
   * </pre>
   */
  public static final class NotificationAdminBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<NotificationAdminBlockingV2Stub> {
    private NotificationAdminBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NotificationAdminBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NotificationAdminBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * PublishBroadcastEvent :: Publish the Broadcast events to all CDP users (across all tenants).
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse publishBroadcastEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getPublishBroadcastEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * PublishTargetedEvent :: Publish the targeted event to notification service when event occur.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse publishTargetedEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getPublishTargetedEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the types of broadcast events available in the catalog. :: Lists all the types of broadcast events available in the catalog.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse listBroadcastEventCatalog(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListBroadcastEventCatalogMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the broadcast messages. :: Lists all the broadcast messages.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse listBroadcastNotifications(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListBroadcastNotificationsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeletePendingBroadcastEvent :: Deletes broadcast event that is waiting for delivery.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse deletePendingBroadcastEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeletePendingBroadcastEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * ListPendingBroadcastEvents :: Lists all pending broadcast messages scheduled for publication.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse listPendingBroadcastEvents(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListPendingBroadcastEventsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateOrUpdateAccountMetadata :: Creates or Updates Account metadata, such as allowed domains list for the Account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse createOrUpdateAccountMetadata(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateOrUpdateAccountMetadataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetAccountMetadata :: Fetches the Account metadata such as allowed email domains.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse getAccountMetadata(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetAccountMetadataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateOrUpdateDistributionList :: Creates or updates a distribution list with the given preferences.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse createOrUpdateDistributionList(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateOrUpdateDistributionListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * ListDistributionLists :: Lists all distribution lists.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse listDistributionLists(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListDistributionListsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetDistributionList :: Fetches an existing distribution list with events and channel preferences.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse getDistributionList(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetDistributionListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteDistributionList :: Deletes a distribution list.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse deleteDistributionList(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteDistributionListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteDistributionListsForResource :: Deletes distribution lists for the resource CRN.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse deleteDistributionListsForResource(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteDistributionListsForResourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * ListResourceSubscriptions :: Lists all resource subscriptions for a resource or an account ID.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse listResourceSubscriptions(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListResourceSubscriptionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the published broadcast messages. :: Lists all the published broadcast messages.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse listPublishedBroadcastNotifications(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListPublishedBroadcastNotificationsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the draft broadcast events. :: Lists all the draft broadcast events.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse listDraftBroadcastEvents(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListDraftBroadcastEventsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Publish targeted email based on email address. :: Publish targeted email based on email address.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse publishEmailTargetedEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getPublishEmailTargetedEventMethod(), getCallOptions(), request);
    }
<<<<<<< HEAD
=======

    /**
     * <pre>
     * API endpoint to get PublishedEventStatus :: Fetches the published event delivery status.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse getPublishedEventStatus(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetPublishedEventStatusMethod(), getCallOptions(), request);
    }
>>>>>>> 97727b4b88 (CB-30441 EDH integration about the sent notifications + DTO refactors and updates)
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service NotificationAdmin.
   * <pre>
   * Notification Admin Service :: Notification Admin Service.
   * </pre>
   */
  public static final class NotificationAdminBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<NotificationAdminBlockingStub> {
    private NotificationAdminBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NotificationAdminBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NotificationAdminBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * PublishBroadcastEvent :: Publish the Broadcast events to all CDP users (across all tenants).
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse publishBroadcastEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPublishBroadcastEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * PublishTargetedEvent :: Publish the targeted event to notification service when event occur.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse publishTargetedEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPublishTargetedEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the types of broadcast events available in the catalog. :: Lists all the types of broadcast events available in the catalog.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse listBroadcastEventCatalog(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListBroadcastEventCatalogMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the broadcast messages. :: Lists all the broadcast messages.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse listBroadcastNotifications(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListBroadcastNotificationsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeletePendingBroadcastEvent :: Deletes broadcast event that is waiting for delivery.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse deletePendingBroadcastEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeletePendingBroadcastEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * ListPendingBroadcastEvents :: Lists all pending broadcast messages scheduled for publication.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse listPendingBroadcastEvents(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListPendingBroadcastEventsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateOrUpdateAccountMetadata :: Creates or Updates Account metadata, such as allowed domains list for the Account.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse createOrUpdateAccountMetadata(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateOrUpdateAccountMetadataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetAccountMetadata :: Fetches the Account metadata such as allowed email domains.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse getAccountMetadata(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAccountMetadataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * CreateOrUpdateDistributionList :: Creates or updates a distribution list with the given preferences.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse createOrUpdateDistributionList(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateOrUpdateDistributionListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * ListDistributionLists :: Lists all distribution lists.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse listDistributionLists(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListDistributionListsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetDistributionList :: Fetches an existing distribution list with events and channel preferences.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse getDistributionList(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetDistributionListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteDistributionList :: Deletes a distribution list.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse deleteDistributionList(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteDistributionListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DeleteDistributionListsForResource :: Deletes distribution lists for the resource CRN.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse deleteDistributionListsForResource(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteDistributionListsForResourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * ListResourceSubscriptions :: Lists all resource subscriptions for a resource or an account ID.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse listResourceSubscriptions(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListResourceSubscriptionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the published broadcast messages. :: Lists all the published broadcast messages.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse listPublishedBroadcastNotifications(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListPublishedBroadcastNotificationsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists all the draft broadcast events. :: Lists all the draft broadcast events.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse listDraftBroadcastEvents(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListDraftBroadcastEventsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Publish targeted email based on email address. :: Publish targeted email based on email address.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse publishEmailTargetedEvent(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPublishEmailTargetedEventMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * API endpoint to get PublishedEventStatus :: Fetches the published event delivery status.
     * </pre>
     */
    public com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse getPublishedEventStatus(com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetPublishedEventStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service NotificationAdmin.
   * <pre>
   * Notification Admin Service :: Notification Admin Service.
   * </pre>
   */
  public static final class NotificationAdminFutureStub
      extends io.grpc.stub.AbstractFutureStub<NotificationAdminFutureStub> {
    private NotificationAdminFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NotificationAdminFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NotificationAdminFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * PublishBroadcastEvent :: Publish the Broadcast events to all CDP users (across all tenants).
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse> publishBroadcastEvent(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPublishBroadcastEventMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * PublishTargetedEvent :: Publish the targeted event to notification service when event occur.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse> publishTargetedEvent(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPublishTargetedEventMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists all the types of broadcast events available in the catalog. :: Lists all the types of broadcast events available in the catalog.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse> listBroadcastEventCatalog(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListBroadcastEventCatalogMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists all the broadcast messages. :: Lists all the broadcast messages.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse> listBroadcastNotifications(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListBroadcastNotificationsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * DeletePendingBroadcastEvent :: Deletes broadcast event that is waiting for delivery.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse> deletePendingBroadcastEvent(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeletePendingBroadcastEventMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * ListPendingBroadcastEvents :: Lists all pending broadcast messages scheduled for publication.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse> listPendingBroadcastEvents(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListPendingBroadcastEventsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * CreateOrUpdateAccountMetadata :: Creates or Updates Account metadata, such as allowed domains list for the Account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse> createOrUpdateAccountMetadata(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateOrUpdateAccountMetadataMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetAccountMetadata :: Fetches the Account metadata such as allowed email domains.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse> getAccountMetadata(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAccountMetadataMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * CreateOrUpdateDistributionList :: Creates or updates a distribution list with the given preferences.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse> createOrUpdateDistributionList(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateOrUpdateDistributionListMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * ListDistributionLists :: Lists all distribution lists.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse> listDistributionLists(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListDistributionListsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetDistributionList :: Fetches an existing distribution list with events and channel preferences.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse> getDistributionList(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetDistributionListMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * DeleteDistributionList :: Deletes a distribution list.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse> deleteDistributionList(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteDistributionListMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * DeleteDistributionListsForResource :: Deletes distribution lists for the resource CRN.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse> deleteDistributionListsForResource(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteDistributionListsForResourceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * ListResourceSubscriptions :: Lists all resource subscriptions for a resource or an account ID.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse> listResourceSubscriptions(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListResourceSubscriptionsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists all the published broadcast messages. :: Lists all the published broadcast messages.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse> listPublishedBroadcastNotifications(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListPublishedBroadcastNotificationsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists all the draft broadcast events. :: Lists all the draft broadcast events.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse> listDraftBroadcastEvents(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListDraftBroadcastEventsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Publish targeted email based on email address. :: Publish targeted email based on email address.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse> publishEmailTargetedEvent(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPublishEmailTargetedEventMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * API endpoint to get PublishedEventStatus :: Fetches the published event delivery status.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse> getPublishedEventStatus(
        com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetPublishedEventStatusMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PUBLISH_BROADCAST_EVENT = 0;
  private static final int METHODID_PUBLISH_TARGETED_EVENT = 1;
  private static final int METHODID_LIST_BROADCAST_EVENT_CATALOG = 2;
  private static final int METHODID_LIST_BROADCAST_NOTIFICATIONS = 3;
  private static final int METHODID_DELETE_PENDING_BROADCAST_EVENT = 4;
  private static final int METHODID_LIST_PENDING_BROADCAST_EVENTS = 5;
  private static final int METHODID_CREATE_OR_UPDATE_ACCOUNT_METADATA = 6;
  private static final int METHODID_GET_ACCOUNT_METADATA = 7;
  private static final int METHODID_CREATE_OR_UPDATE_DISTRIBUTION_LIST = 8;
  private static final int METHODID_LIST_DISTRIBUTION_LISTS = 9;
  private static final int METHODID_GET_DISTRIBUTION_LIST = 10;
  private static final int METHODID_DELETE_DISTRIBUTION_LIST = 11;
  private static final int METHODID_DELETE_DISTRIBUTION_LISTS_FOR_RESOURCE = 12;
  private static final int METHODID_LIST_RESOURCE_SUBSCRIPTIONS = 13;
  private static final int METHODID_LIST_PUBLISHED_BROADCAST_NOTIFICATIONS = 14;
  private static final int METHODID_LIST_DRAFT_BROADCAST_EVENTS = 15;
  private static final int METHODID_PUBLISH_EMAIL_TARGETED_EVENT = 16;
  private static final int METHODID_GET_PUBLISHED_EVENT_STATUS = 17;

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
        case METHODID_PUBLISH_BROADCAST_EVENT:
          serviceImpl.publishBroadcastEvent((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse>) responseObserver);
          break;
        case METHODID_PUBLISH_TARGETED_EVENT:
          serviceImpl.publishTargetedEvent((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse>) responseObserver);
          break;
        case METHODID_LIST_BROADCAST_EVENT_CATALOG:
          serviceImpl.listBroadcastEventCatalog((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse>) responseObserver);
          break;
        case METHODID_LIST_BROADCAST_NOTIFICATIONS:
          serviceImpl.listBroadcastNotifications((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse>) responseObserver);
          break;
        case METHODID_DELETE_PENDING_BROADCAST_EVENT:
          serviceImpl.deletePendingBroadcastEvent((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse>) responseObserver);
          break;
        case METHODID_LIST_PENDING_BROADCAST_EVENTS:
          serviceImpl.listPendingBroadcastEvents((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse>) responseObserver);
          break;
        case METHODID_CREATE_OR_UPDATE_ACCOUNT_METADATA:
          serviceImpl.createOrUpdateAccountMetadata((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse>) responseObserver);
          break;
        case METHODID_GET_ACCOUNT_METADATA:
          serviceImpl.getAccountMetadata((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse>) responseObserver);
          break;
        case METHODID_CREATE_OR_UPDATE_DISTRIBUTION_LIST:
          serviceImpl.createOrUpdateDistributionList((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse>) responseObserver);
          break;
        case METHODID_LIST_DISTRIBUTION_LISTS:
          serviceImpl.listDistributionLists((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse>) responseObserver);
          break;
        case METHODID_GET_DISTRIBUTION_LIST:
          serviceImpl.getDistributionList((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse>) responseObserver);
          break;
        case METHODID_DELETE_DISTRIBUTION_LIST:
          serviceImpl.deleteDistributionList((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse>) responseObserver);
          break;
        case METHODID_DELETE_DISTRIBUTION_LISTS_FOR_RESOURCE:
          serviceImpl.deleteDistributionListsForResource((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse>) responseObserver);
          break;
        case METHODID_LIST_RESOURCE_SUBSCRIPTIONS:
          serviceImpl.listResourceSubscriptions((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse>) responseObserver);
          break;
        case METHODID_LIST_PUBLISHED_BROADCAST_NOTIFICATIONS:
          serviceImpl.listPublishedBroadcastNotifications((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse>) responseObserver);
          break;
        case METHODID_LIST_DRAFT_BROADCAST_EVENTS:
          serviceImpl.listDraftBroadcastEvents((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse>) responseObserver);
          break;
        case METHODID_PUBLISH_EMAIL_TARGETED_EVENT:
          serviceImpl.publishEmailTargetedEvent((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse>) responseObserver);
          break;
        case METHODID_GET_PUBLISHED_EVENT_STATUS:
          serviceImpl.getPublishedEventStatus((com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse>) responseObserver);
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
          getPublishBroadcastEventMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishBroadcastEventResponse>(
                service, METHODID_PUBLISH_BROADCAST_EVENT)))
        .addMethod(
          getPublishTargetedEventMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse>(
                service, METHODID_PUBLISH_TARGETED_EVENT)))
        .addMethod(
          getListBroadcastEventCatalogMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastEventCatalogResponse>(
                service, METHODID_LIST_BROADCAST_EVENT_CATALOG)))
        .addMethod(
          getListBroadcastNotificationsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListBroadcastNotificationsResponse>(
                service, METHODID_LIST_BROADCAST_NOTIFICATIONS)))
        .addMethod(
          getDeletePendingBroadcastEventMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeletePendingBroadcastEventResponse>(
                service, METHODID_DELETE_PENDING_BROADCAST_EVENT)))
        .addMethod(
          getListPendingBroadcastEventsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPendingBroadcastEventsResponse>(
                service, METHODID_LIST_PENDING_BROADCAST_EVENTS)))
        .addMethod(
          getCreateOrUpdateAccountMetadataMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse>(
                service, METHODID_CREATE_OR_UPDATE_ACCOUNT_METADATA)))
        .addMethod(
          getGetAccountMetadataMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetAccountMetadataResponse>(
                service, METHODID_GET_ACCOUNT_METADATA)))
        .addMethod(
          getCreateOrUpdateDistributionListMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse>(
                service, METHODID_CREATE_OR_UPDATE_DISTRIBUTION_LIST)))
        .addMethod(
          getListDistributionListsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse>(
                service, METHODID_LIST_DISTRIBUTION_LISTS)))
        .addMethod(
          getGetDistributionListMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetDistributionListResponse>(
                service, METHODID_GET_DISTRIBUTION_LIST)))
        .addMethod(
          getDeleteDistributionListMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse>(
                service, METHODID_DELETE_DISTRIBUTION_LIST)))
        .addMethod(
          getDeleteDistributionListsForResourceMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListsForResourceResponse>(
                service, METHODID_DELETE_DISTRIBUTION_LISTS_FOR_RESOURCE)))
        .addMethod(
          getListResourceSubscriptionsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListResourceSubscriptionsResponse>(
                service, METHODID_LIST_RESOURCE_SUBSCRIPTIONS)))
        .addMethod(
          getListPublishedBroadcastNotificationsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListPublishedBroadcastNotificationsResponse>(
                service, METHODID_LIST_PUBLISHED_BROADCAST_NOTIFICATIONS)))
        .addMethod(
          getListDraftBroadcastEventsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDraftBroadcastEventsResponse>(
                service, METHODID_LIST_DRAFT_BROADCAST_EVENTS)))
        .addMethod(
          getPublishEmailTargetedEventMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishEmailTargetedEventResponse>(
                service, METHODID_PUBLISH_EMAIL_TARGETED_EVENT)))
        .addMethod(
          getGetPublishedEventStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest,
              com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse>(
                service, METHODID_GET_PUBLISHED_EVENT_STATUS)))
        .build();
  }

  private static abstract class NotificationAdminBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    NotificationAdminBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("NotificationAdmin");
    }
  }

  private static final class NotificationAdminFileDescriptorSupplier
      extends NotificationAdminBaseDescriptorSupplier {
    NotificationAdminFileDescriptorSupplier() {}
  }

  private static final class NotificationAdminMethodDescriptorSupplier
      extends NotificationAdminBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    NotificationAdminMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (NotificationAdminGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new NotificationAdminFileDescriptorSupplier())
              .addMethod(getPublishBroadcastEventMethod())
              .addMethod(getPublishTargetedEventMethod())
              .addMethod(getListBroadcastEventCatalogMethod())
              .addMethod(getListBroadcastNotificationsMethod())
              .addMethod(getDeletePendingBroadcastEventMethod())
              .addMethod(getListPendingBroadcastEventsMethod())
              .addMethod(getCreateOrUpdateAccountMetadataMethod())
              .addMethod(getGetAccountMetadataMethod())
              .addMethod(getCreateOrUpdateDistributionListMethod())
              .addMethod(getListDistributionListsMethod())
              .addMethod(getGetDistributionListMethod())
              .addMethod(getDeleteDistributionListMethod())
              .addMethod(getDeleteDistributionListsForResourceMethod())
              .addMethod(getListResourceSubscriptionsMethod())
              .addMethod(getListPublishedBroadcastNotificationsMethod())
              .addMethod(getListDraftBroadcastEventsMethod())
              .addMethod(getPublishEmailTargetedEventMethod())
              .addMethod(getGetPublishedEventStatusMethod())
              .build();
        }
      }
    }
    return result;
  }
}
