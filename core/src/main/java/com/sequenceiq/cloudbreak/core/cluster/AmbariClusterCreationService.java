package com.sequenceiq.cloudbreak.core.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackType;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class AmbariClusterCreationService {
    @Inject
    private StackService stackService;

    @Inject
    private AmbariClusterConnector ambariClusterConnector;

    @Inject
    private DatalakeConfigProvider datalakeConfigProvider;

    public void startAmbari(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithTransaction(stackId);
        ambariClusterConnector.waitForServer(stack);
        ambariClusterConnector.changeOriginalAmbariCredentialsAndCreateCloudbreakUser(stack);
    }

    public void buildAmbariCluster(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        ambariClusterConnector.buildCluster(stack);
        if (StackType.DATALAKE == stack.getType()) {
            datalakeConfigProvider.collectAndStoreDatalakeResources(stack);
        }
    }
}
