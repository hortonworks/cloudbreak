package com.sequenceiq.cloudbreak.service.cluster.api;

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterSetupService {

    void waitForServer(Stack stack) throws CloudbreakException;

    void buildCluster(Stack stack);

    void waitForHosts(Stack stack) throws CloudbreakSecuritySetupException;

    void waitForServices(Stack stack, int requestId) throws CloudbreakException;
}
