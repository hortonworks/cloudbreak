package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cluster.api.ClusterApi.CLOUDERA_MANAGER;
import static com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService.BEAN_POST_TAG;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cluster.api.ClusterPreCreationApi;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service(CLOUDERA_MANAGER + BEAN_POST_TAG)
public class ClouderaManagerPreCreationService implements ClusterPreCreationApi {

    @Inject
    private ClouderaManagerSecurityConfigProvider securityConfigProvider;

    @Override
    public boolean isVdfReady(AmbariRepo ambariRepo) {
        return false;
    }

    @Override
    public boolean isLdapAndSSOReady(AmbariRepo ambariRepo) {
        return false;
    }

    @Override
    public String getCloudbreakClusterUserName(Cluster cluster) {
        return securityConfigProvider.getCloudbreakClusterUserName(cluster);
    }

    @Override
    public String getCloudbreakClusterPassword(Cluster cluster) {
        return securityConfigProvider.getCloudbreakClusterPassword(cluster);
    }

    @Override
    public String getDataplaneClusterUserName(Cluster cluster) {
        return securityConfigProvider.getDataplaneClusterUserName(cluster);
    }

    @Override
    public String getDataplaneClusterPassword(Cluster cluster) {
        return securityConfigProvider.getDataplaneClusterPassword(cluster);
    }

    @Override
    public String getClusterUserProvidedPassword(Cluster cluster) {
        return securityConfigProvider.getClusterUserProvidedPassword(cluster);
    }

    @Override
    public String getCertPath() {
        return securityConfigProvider.getCertPath();
    }

    @Override
    public String getKeystorePath() {
        return securityConfigProvider.getKeystorePath();
    }

    @Override
    public String getKeystorePassword() {
        return securityConfigProvider.getKeystorePassword();
    }

    @Override
    public String getMasterKey(Cluster cluster) {
        return securityConfigProvider.getMasterKey(cluster);
    }

    @Override
    public Map<String, Integer> getServicePorts(Blueprint blueprint) {
        return Collections.emptyMap();
    }
}
