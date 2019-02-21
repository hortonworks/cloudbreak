package com.sequenceiq.cloudbreak.cm;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service(ClusterApi.CLOUDERA_MANAGER)
@Scope("prototype")
public class ClouderaManagerConnector implements ClusterApi {

    @Inject
    private ApplicationContext applicationContext;

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    public ClouderaManagerConnector(Stack stack, HttpClientConfig clientConfig) {
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
    public ClusterDecomissionService clusterDecomissionService() {
        return applicationContext.getBean(ClouderaManagerClusterDecomissionService.class, stack, clientConfig);
    }
}
