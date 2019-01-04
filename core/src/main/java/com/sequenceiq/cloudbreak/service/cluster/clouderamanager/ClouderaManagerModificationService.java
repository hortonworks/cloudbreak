package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import java.util.Collection;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterModificationService;

@Service
public class ClouderaManagerModificationService implements ClusterModificationService {

    @Override
    public void upscaleCluster(Stack stack, HostGroup hostGroup, Collection<HostMetadata> hostMetadata) throws CloudbreakException {

    }

    @Override
    public void stopCluster(Stack stack) throws CloudbreakException {

    }

    @Override
    public int startCluster(Stack stack) throws CloudbreakException {
        return 0;
    }
}
