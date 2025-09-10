package com.sequenceiq.thunderhead.config;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.thunderhead.grpc.GrpcServer;
import com.sequenceiq.thunderhead.grpc.service.audit.MockAuditLogService;
import com.sequenceiq.thunderhead.grpc.service.auth.MockAuthorizationService;
import com.sequenceiq.thunderhead.grpc.service.auth.MockPersonalResourceViewService;
import com.sequenceiq.thunderhead.grpc.service.auth.MockUserManagementService;
import com.sequenceiq.thunderhead.grpc.service.authdistributor.MockAuthDistributorService;
import com.sequenceiq.thunderhead.grpc.service.cdl.MockCdlService;
import com.sequenceiq.thunderhead.grpc.service.classiccluster.MockClassicClusterService;
import com.sequenceiq.thunderhead.grpc.service.datalakedr.MockDatalakeDrService;
import com.sequenceiq.thunderhead.grpc.service.metering.MockMeteringService;
import com.sequenceiq.thunderhead.grpc.service.pem.MockPublicEndpointManagementService;
import com.sequenceiq.thunderhead.grpc.service.remotecluster.MockRemoteClusterService;
import com.sequenceiq.thunderhead.grpc.service.servicediscovery.MockServiceDiscoveryService;

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

    @Inject
    private MockDatalakeDrService datalakeDrService;

    @Inject
    private MockPublicEndpointManagementService mockPublicEndpointManagementService;

    @Inject
    private MockRemoteClusterService mockRemoteClusterService;

    @Inject
    private MockClassicClusterService mockClassicClusterService;

    @Inject
    private MockServiceDiscoveryService mockServiceDiscoveryService;

    @Inject
    private MockAuthDistributorService mockAuthDistributorService;

    @Inject
    private MockMeteringService mockMeteringService;

    @Inject
    private MockCdlService mockCdlService;

    @Value("${grpc.server.port:8982}")
    private int grpcServerPort;

    @Value("${datalakedr.server.port:8981}")
    private int datalakeDrServerPort;

    @Bean
    public GrpcServer grpcServer() {
        return new GrpcServer(
                grpcServerPort,
                mockUserManagementService.bindService(),
                mockAuthorizationService.bindService(),
                mockPersonalResourceViewService.bindService(),
                mockAuditLogService.bindService(),
                mockPublicEndpointManagementService.bindService(),
                mockServiceDiscoveryService.bindService(),
                mockAuthDistributorService.bindService(),
                mockMeteringService.bindService(),
                mockRemoteClusterService.bindService(),
                mockClassicClusterService.bindService(),
                mockCdlService.bindService());
    }

    @Bean
    public GrpcServer datalakedrServer() {
        return new GrpcServer(datalakeDrServerPort, datalakeDrService.bindService());
    }
}
