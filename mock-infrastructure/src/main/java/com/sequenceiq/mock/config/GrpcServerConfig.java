package com.sequenceiq.mock.config;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.mock.grpc.GrpcServer;
import com.sequenceiq.mock.grpc.service.LiftieService;

@Configuration
public class GrpcServerConfig {

    @Value("${grpc.server.port:8987}")
    private int grpcServerPort;

    @Inject
    private LiftieService liftieService;

    @Bean
    public GrpcServer grpcServer() {
        return new GrpcServer(
                grpcServerPort,
                liftieService.bindService());
    }

}
