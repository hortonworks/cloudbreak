package com.sequenceiq.cloudbreak.service.cluster.api;

import java.util.Collection;

import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterModificationService {

    void upscaleCluster(Stack stack, HostGroup hostGroup, Collection<HostMetadata> hostMetadata) throws CloudbreakException;

    void stopCluster(Stack stack) throws CloudbreakException;

    int startCluster(Stack stack) throws CloudbreakException;
}
