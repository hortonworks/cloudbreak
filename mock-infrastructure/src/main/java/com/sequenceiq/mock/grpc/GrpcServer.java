package com.sequenceiq.mock.grpc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

public class GrpcServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServer.class);

    private final int port;

    private final ImmutableList<ServerServiceDefinition> serviceDefinitions;

    private Server server;

    public GrpcServer(int port, ServerServiceDefinition... serviceDefinitions) {
        this.port = port;
        ImmutableList.Builder<ServerServiceDefinition> builder =
                ImmutableList.builder();
        for (ServerServiceDefinition serviceDefinition : serviceDefinitions) {
            builder.add(ServerInterceptors.intercept(
                    checkNotNull(serviceDefinition),
                    new RequestContextServerInterceptor()));
        }
        this.serviceDefinitions = builder.build();
    }

    @PostConstruct
    public void start() {
        checkState(server == null);
        try {
            ServerBuilder builder = ServerBuilder.forPort(port);
            for (ServerServiceDefinition serviceDefinition : serviceDefinitions) {
                LOGGER.info("Starting {} service on port {}.", serviceDefinition.getServiceDescriptor().getName(), port);
                builder.addService(serviceDefinition);
            }
            server = builder.build().start();
            LOGGER.info("Server started.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (server != null) {
            server.shutdown();
            try {
                server.awaitTermination();
                server = null;
                LOGGER.info("Server stopped.");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
