package com.cloudera.thunderhead.service.workloadconnectivity;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * The admin service catering to managing connectivity between Cloudera's control plane and customer owned environments.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class WorkloadConnectivityAdminApiGrpc {

  private WorkloadConnectivityAdminApiGrpc() {}

  public static final java.lang.String SERVICE_NAME = "workloadconnectivityadminapi.WorkloadConnectivityAdminApi";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest,
      com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse> getListTunnelStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListTunnelStatus",
      requestType = com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest.class,
      responseType = com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest,
      com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse> getListTunnelStatusMethod() {
    io.grpc.MethodDescriptor<com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest, com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse> getListTunnelStatusMethod;
    if ((getListTunnelStatusMethod = WorkloadConnectivityAdminApiGrpc.getListTunnelStatusMethod) == null) {
      synchronized (WorkloadConnectivityAdminApiGrpc.class) {
        if ((getListTunnelStatusMethod = WorkloadConnectivityAdminApiGrpc.getListTunnelStatusMethod) == null) {
          WorkloadConnectivityAdminApiGrpc.getListTunnelStatusMethod = getListTunnelStatusMethod =
              io.grpc.MethodDescriptor.<com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest, com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListTunnelStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WorkloadConnectivityAdminApiMethodDescriptorSupplier("ListTunnelStatus"))
              .build();
        }
      }
    }
    return getListTunnelStatusMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static WorkloadConnectivityAdminApiStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WorkloadConnectivityAdminApiStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WorkloadConnectivityAdminApiStub>() {
        @java.lang.Override
        public WorkloadConnectivityAdminApiStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WorkloadConnectivityAdminApiStub(channel, callOptions);
        }
      };
    return WorkloadConnectivityAdminApiStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static WorkloadConnectivityAdminApiBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WorkloadConnectivityAdminApiBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WorkloadConnectivityAdminApiBlockingV2Stub>() {
        @java.lang.Override
        public WorkloadConnectivityAdminApiBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WorkloadConnectivityAdminApiBlockingV2Stub(channel, callOptions);
        }
      };
    return WorkloadConnectivityAdminApiBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static WorkloadConnectivityAdminApiBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WorkloadConnectivityAdminApiBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WorkloadConnectivityAdminApiBlockingStub>() {
        @java.lang.Override
        public WorkloadConnectivityAdminApiBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WorkloadConnectivityAdminApiBlockingStub(channel, callOptions);
        }
      };
    return WorkloadConnectivityAdminApiBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static WorkloadConnectivityAdminApiFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WorkloadConnectivityAdminApiFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WorkloadConnectivityAdminApiFutureStub>() {
        @java.lang.Override
        public WorkloadConnectivityAdminApiFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WorkloadConnectivityAdminApiFutureStub(channel, callOptions);
        }
      };
    return WorkloadConnectivityAdminApiFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * The admin service catering to managing connectivity between Cloudera's control plane and customer owned environments.
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * List the status of tunnels between jumpgate server and agents :: Returns a list of tunnels (with their status) which enable a specified environment's link with the control plane.
     * </pre>
     */
    default void listTunnelStatus(com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListTunnelStatusMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service WorkloadConnectivityAdminApi.
   * <pre>
   * The admin service catering to managing connectivity between Cloudera's control plane and customer owned environments.
   * </pre>
   */
  public static abstract class WorkloadConnectivityAdminApiImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return WorkloadConnectivityAdminApiGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service WorkloadConnectivityAdminApi.
   * <pre>
   * The admin service catering to managing connectivity between Cloudera's control plane and customer owned environments.
   * </pre>
   */
  public static final class WorkloadConnectivityAdminApiStub
      extends io.grpc.stub.AbstractAsyncStub<WorkloadConnectivityAdminApiStub> {
    private WorkloadConnectivityAdminApiStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WorkloadConnectivityAdminApiStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WorkloadConnectivityAdminApiStub(channel, callOptions);
    }

    /**
     * <pre>
     * List the status of tunnels between jumpgate server and agents :: Returns a list of tunnels (with their status) which enable a specified environment's link with the control plane.
     * </pre>
     */
    public void listTunnelStatus(com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest request,
        io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListTunnelStatusMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service WorkloadConnectivityAdminApi.
   * <pre>
   * The admin service catering to managing connectivity between Cloudera's control plane and customer owned environments.
   * </pre>
   */
  public static final class WorkloadConnectivityAdminApiBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<WorkloadConnectivityAdminApiBlockingV2Stub> {
    private WorkloadConnectivityAdminApiBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WorkloadConnectivityAdminApiBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WorkloadConnectivityAdminApiBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * List the status of tunnels between jumpgate server and agents :: Returns a list of tunnels (with their status) which enable a specified environment's link with the control plane.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse listTunnelStatus(com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListTunnelStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service WorkloadConnectivityAdminApi.
   * <pre>
   * The admin service catering to managing connectivity between Cloudera's control plane and customer owned environments.
   * </pre>
   */
  public static final class WorkloadConnectivityAdminApiBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<WorkloadConnectivityAdminApiBlockingStub> {
    private WorkloadConnectivityAdminApiBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WorkloadConnectivityAdminApiBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WorkloadConnectivityAdminApiBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * List the status of tunnels between jumpgate server and agents :: Returns a list of tunnels (with their status) which enable a specified environment's link with the control plane.
     * </pre>
     */
    public com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse listTunnelStatus(com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListTunnelStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service WorkloadConnectivityAdminApi.
   * <pre>
   * The admin service catering to managing connectivity between Cloudera's control plane and customer owned environments.
   * </pre>
   */
  public static final class WorkloadConnectivityAdminApiFutureStub
      extends io.grpc.stub.AbstractFutureStub<WorkloadConnectivityAdminApiFutureStub> {
    private WorkloadConnectivityAdminApiFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WorkloadConnectivityAdminApiFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WorkloadConnectivityAdminApiFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * List the status of tunnels between jumpgate server and agents :: Returns a list of tunnels (with their status) which enable a specified environment's link with the control plane.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse> listTunnelStatus(
        com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListTunnelStatusMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_TUNNEL_STATUS = 0;

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
        case METHODID_LIST_TUNNEL_STATUS:
          serviceImpl.listTunnelStatus((com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse>) responseObserver);
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
          getListTunnelStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest,
              com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse>(
                service, METHODID_LIST_TUNNEL_STATUS)))
        .build();
  }

  private static abstract class WorkloadConnectivityAdminApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    WorkloadConnectivityAdminApiBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("WorkloadConnectivityAdminApi");
    }
  }

  private static final class WorkloadConnectivityAdminApiFileDescriptorSupplier
      extends WorkloadConnectivityAdminApiBaseDescriptorSupplier {
    WorkloadConnectivityAdminApiFileDescriptorSupplier() {}
  }

  private static final class WorkloadConnectivityAdminApiMethodDescriptorSupplier
      extends WorkloadConnectivityAdminApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    WorkloadConnectivityAdminApiMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (WorkloadConnectivityAdminApiGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new WorkloadConnectivityAdminApiFileDescriptorSupplier())
              .addMethod(getListTunnelStatusMethod())
              .build();
        }
      }
    }
    return result;
  }
}
