package com.sequenceiq.cloudbreak.service.cluster.ambari;

import com.sequenceiq.cloudbreak.service.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterSetupService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class AmbariClusterConnector implements ClusterApi {

    @Inject
    private AmbariClusterSetupService ambariClusterSetupService;

    @Inject
    private AmbariClusterModificationService ambariClusterModificationService;

    @Inject
    private AmbariClusterSecurityService ambariClusterSecurityService;

    @Override
    public ClusterSetupService clusterSetupService() {
        return ambariClusterSetupService;
    }

    @Override
    public ClusterModificationService clusterModificationService() {
        return ambariClusterModificationService;
    }

    @Override
    public ClusterSecurityService clusterSecurityService() {
        return ambariClusterSecurityService;
    }
}
