package com.sequenceiq.cloudbreak.ambari;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.ambari.status.AmbariClusterStatusService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service(ClusterApi.AMBARI)
@Scope("prototype")
public class AmbariClusterConnector implements ClusterApi {

    @Inject
    private ApplicationContext applicationContext;

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    public AmbariClusterConnector(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @Override
    public ClusterSetupService clusterSetupService() {
        return applicationContext.getBean(AmbariClusterSetupService.class, stack, clientConfig);
    }

    @Override
    public ClusterModificationService clusterModificationService() {
        return applicationContext.getBean(AmbariClusterModificationService.class, stack, clientConfig);

    }

    @Override
    public ClusterSecurityService clusterSecurityService() {
        return applicationContext.getBean(AmbariClusterSecurityService.class, stack, clientConfig);
    }

    @Override
    public ClusterStatusService clusterStatusService() {
        return applicationContext.getBean(AmbariClusterStatusService.class, stack, clientConfig);
    }

    @Override
    public ClusterDecomissionService clusterDecomissionService() {
        return applicationContext.getBean(AmbariClusterDecomissionService.class, stack, clientConfig);
    }
}
