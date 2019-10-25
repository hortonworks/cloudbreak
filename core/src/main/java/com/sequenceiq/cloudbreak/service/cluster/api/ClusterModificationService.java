package com.sequenceiq.cloudbreak.service.cluster.api;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterModificationService {

    void upscaleCluster(Stack stack, String hostGroupName) throws CloudbreakException;

    void stopCluster(Stack stack) throws CloudbreakException;

    int startCluster(Stack stack) throws CloudbreakException;
}
