package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterSetupService;

@Service
public class ClouderaManagerConnector implements ClusterApi {

    @Inject
    private ClouderaManagerSetupService clouderaManagerSetupService;

    @Inject
    private ClouderaManagerModificationService clouderaManagerModificationService;

    @Inject
    private ClouderaManagerSecurityService clouderaManagerSecurityService;

    @Override
    public ClusterSetupService clusterSetupService() {
        return clouderaManagerSetupService;
    }

    @Override
    public ClusterModificationService clusterModificationService() {
        return clouderaManagerModificationService;
    }

    @Override
    public ClusterSecurityService clusterSecurityService() {
        return clouderaManagerSecurityService;
    }

    @Override
    public String clusterVariant() {
        return "CLOUDERA_MANAGER";
    }
}
