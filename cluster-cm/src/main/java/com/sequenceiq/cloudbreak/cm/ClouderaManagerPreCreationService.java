package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cluster.api.ClusterApi.CLOUDERA_MANAGER;
import static com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService.BEAN_POST_TAG;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.api.ClusterPreCreationApi;
import com.sequenceiq.cloudbreak.cluster.model.ServiceLocationMap;
import com.sequenceiq.cloudbreak.cm.config.CmMgmtVolumePathBuilder;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service(CLOUDERA_MANAGER + BEAN_POST_TAG)
public class ClouderaManagerPreCreationService implements ClusterPreCreationApi {

    @Inject
    private ClouderaManagerSecurityConfigProvider securityConfigProvider;

    @Inject
    private ClouderaManagerBlueprintPortConfigCollector clouderaManagerBlueprintPortConfigCollector;

    @Inject
    private CmMgmtVolumePathBuilder volumePathBuilder;

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
    public Map<String, Integer> getServicePorts(Blueprint blueprint, boolean tls) {
        return clouderaManagerBlueprintPortConfigCollector.getServicePorts(blueprint, tls);
    }

    @Override
    public Map<String, String> getServiceProtocols(Blueprint blueprint, boolean tls) {
        return clouderaManagerBlueprintPortConfigCollector.getServiceProtocols(blueprint, tls);
    }

    @Override
    public ServiceLocationMap getServiceLocations() {
        return volumePathBuilder.buildServiceLocationMap();
    }
}
