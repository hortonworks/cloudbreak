package com.sequenceiq.thunderhead.config;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.thunderhead.grpc.GrpcServer;
import com.sequenceiq.thunderhead.grpc.service.audit.MockAuditLogService;
import com.sequenceiq.thunderhead.grpc.service.auth.MockAuthorizationService;
import com.sequenceiq.thunderhead.grpc.service.auth.MockPersonalResourceViewService;
import com.sequenceiq.thunderhead.grpc.service.auth.MockUserManagementService;

@Configuration
public class GrpcServerConfig {

    @Inject
    private MockUserManagementService mockUserManagementService;

    @Inject
    private MockAuthorizationService mockAuthorizationService;

    @Inject
    private MockPersonalResourceViewService mockPersonalResourceViewService;

    @Inject
    private MockAuditLogService mockAuditLogService;

    @Value("${grpc.server.port:8982}")
    private int grpcServerPort;

    @Bean
    public GrpcServer grpcServer() {
        return new GrpcServer(
                grpcServerPort,
                mockUserManagementService.bindService(),
                mockAuthorizationService.bindService(),
                mockPersonalResourceViewService.bindService(),
                mockAuditLogService.bindService());
    }
}
