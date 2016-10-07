package com.netflix.dynomitemanager.identity;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;

/**
 * <pre>
 * The InstanceDataDAO service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.0.0)",
    comments = "Source: DAOGrpc.proto")
public class DAOGrpcGrpc {

  private DAOGrpcGrpc() {}

  public static final String SERVICE_NAME = "com.netflix.dynomitemanager.identity.DAOGrpc";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.netflix.dynomitemanager.identity.AppsInstanceFields,
      com.netflix.dynomitemanager.identity.CRUDResponse> METHOD_CREATE_INSTANCE_ENTRY =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "com.netflix.dynomitemanager.identity.DAOGrpc", "CreateInstanceEntry"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.netflix.dynomitemanager.identity.AppsInstanceFields.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.netflix.dynomitemanager.identity.CRUDResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DAOGrpcStub newStub(io.grpc.Channel channel) {
    return new DAOGrpcStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DAOGrpcBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new DAOGrpcBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static DAOGrpcFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new DAOGrpcFutureStub(channel);
  }

  /**
   * <pre>
   * The InstanceDataDAO service definition.
   * </pre>
   */
  public static abstract class DAOGrpcImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Create an instance entry
     * </pre>
     */
    public void createInstanceEntry(com.netflix.dynomitemanager.identity.AppsInstanceFields request,
        io.grpc.stub.StreamObserver<com.netflix.dynomitemanager.identity.CRUDResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_CREATE_INSTANCE_ENTRY, responseObserver);
    }

    @java.lang.Override public io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_CREATE_INSTANCE_ENTRY,
            asyncUnaryCall(
              new MethodHandlers<
                com.netflix.dynomitemanager.identity.AppsInstanceFields,
                com.netflix.dynomitemanager.identity.CRUDResponse>(
                  this, METHODID_CREATE_INSTANCE_ENTRY)))
          .build();
    }
  }

  /**
   * <pre>
   * The InstanceDataDAO service definition.
   * </pre>
   */
  public static final class DAOGrpcStub extends io.grpc.stub.AbstractStub<DAOGrpcStub> {
    private DAOGrpcStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DAOGrpcStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DAOGrpcStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DAOGrpcStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create an instance entry
     * </pre>
     */
    public void createInstanceEntry(com.netflix.dynomitemanager.identity.AppsInstanceFields request,
        io.grpc.stub.StreamObserver<com.netflix.dynomitemanager.identity.CRUDResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_CREATE_INSTANCE_ENTRY, getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * The InstanceDataDAO service definition.
   * </pre>
   */
  public static final class DAOGrpcBlockingStub extends io.grpc.stub.AbstractStub<DAOGrpcBlockingStub> {
    private DAOGrpcBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DAOGrpcBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DAOGrpcBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DAOGrpcBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create an instance entry
     * </pre>
     */
    public com.netflix.dynomitemanager.identity.CRUDResponse createInstanceEntry(com.netflix.dynomitemanager.identity.AppsInstanceFields request) {
      return blockingUnaryCall(
          getChannel(), METHOD_CREATE_INSTANCE_ENTRY, getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * The InstanceDataDAO service definition.
   * </pre>
   */
  public static final class DAOGrpcFutureStub extends io.grpc.stub.AbstractStub<DAOGrpcFutureStub> {
    private DAOGrpcFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DAOGrpcFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DAOGrpcFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DAOGrpcFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create an instance entry
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.netflix.dynomitemanager.identity.CRUDResponse> createInstanceEntry(
        com.netflix.dynomitemanager.identity.AppsInstanceFields request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_CREATE_INSTANCE_ENTRY, getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_INSTANCE_ENTRY = 0;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final DAOGrpcImplBase serviceImpl;
    private final int methodId;

    public MethodHandlers(DAOGrpcImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_INSTANCE_ENTRY:
          serviceImpl.createInstanceEntry((com.netflix.dynomitemanager.identity.AppsInstanceFields) request,
              (io.grpc.stub.StreamObserver<com.netflix.dynomitemanager.identity.CRUDResponse>) responseObserver);
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

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    return new io.grpc.ServiceDescriptor(SERVICE_NAME,
        METHOD_CREATE_INSTANCE_ENTRY);
  }

}
