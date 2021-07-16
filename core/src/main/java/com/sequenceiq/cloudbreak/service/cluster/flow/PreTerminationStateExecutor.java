package com.sequenceiq.cloudbreak.service.cluster.flow;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterKerberosService;

@Component
public class PreTerminationStateExecutor {
    @Inject
    private ClusterKerberosService clusterKerberosService;

    public void runPreTerminationTasks(Stack stack) throws CloudbreakException {
        clusterKerberosService.leaveDomains(stack);
    }
}
