package com.sequenceiq.cloudbreak.cm;

import jakarta.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterCommissionService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDiagnosticsService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterHealthService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterKraftMigrationStatusService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Service(ClusterApi.CLOUDERA_MANAGER)
@Scope("prototype")
public class ClouderaManagerConnector implements ClusterApi {

    @Inject
    private ApplicationContext applicationContext;

    private final StackDtoDelegate stack;

    private final HttpClientConfig clientConfig;

    public ClouderaManagerConnector(StackDtoDelegate stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @Override
    public ClusterSetupService clusterSetupService() {
        return applicationContext.getBean(ClouderaManagerSetupService.class, stack, clientConfig);
    }

    @Override
    public ClusterModificationService clusterModificationService() {
        return applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig);
    }

    @Override
    public ClusterSecurityService clusterSecurityService() {
        return applicationContext.getBean(ClouderaManagerSecurityService.class, stack, clientConfig);
    }

    @Override
    public ClusterStatusService clusterStatusService() {
        return applicationContext.getBean(ClouderaManagerClusterStatusService.class, stack, clientConfig);
    }

    @Override
    public ClusterKraftMigrationStatusService clusterKraftMigrationStatusService() {
        return applicationContext.getBean(ClouderaManagerClusterKraftMigrationStatusService.class, stack, clientConfig);
    }

    @Override
    public ClusterDecomissionService clusterDecomissionService() {
        return applicationContext.getBean(ClouderaManagerClusterDecommissionService.class, stack, clientConfig);
    }

    @Override
    public ClusterCommissionService clusterCommissionService() {
        return applicationContext.getBean(ClouderaManagerClusterCommissionService.class, stack, clientConfig);
    }

    @Override
    public ClusterDiagnosticsService clusterDiagnosticsService() {
        return applicationContext.getBean(ClouderaManagerDiagnosticsService.class, stack, clientConfig);
    }

    @Override
    public ClusterHealthService clusterHealthService() {
        return applicationContext.getBean(ClouderaManagerClusterHealthService.class, stack, clientConfig);
    }
}
