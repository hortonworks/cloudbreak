package com.sequenceiq.cloudbreak.service.cluster.flow;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterKerberosService;

@Component
public class PreTerminationStateExecutor {
    @Inject
    private ClusterKerberosService clusterKerberosService;

    public void runPreTerminationTasks(StackDto stackDto) throws CloudbreakException {
        clusterKerberosService.leaveDomains(stackDto);
    }
}
