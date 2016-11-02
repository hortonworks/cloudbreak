package com.sequenceiq.cloudbreak.core.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class AmbariClusterCreationService {
    @Inject
    private StackService stackService;

    @Inject
    private AmbariClusterConnector ambariClusterConnector;

    public void startAmbari(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getById(stackId);
        ambariClusterConnector.waitForAmbariServer(stack);
        ambariClusterConnector.changeOriginalAmbariCredentials(stack);
    }

    public void buildAmbariCluster(Long stackId) {
        Stack stack = stackService.getById(stackId);
        ambariClusterConnector.buildAmbariCluster(stack);
    }
}
