package com.sequenceiq.cloudbreak.ambari;

import static com.sequenceiq.cloudbreak.ambari.AmbariRepositoryVersionService.AMBARI_VERSION_2_6_0_0;
import static com.sequenceiq.cloudbreak.ambari.AmbariRepositoryVersionService.AMBARI_VERSION_2_7_0_0;
import static com.sequenceiq.cloudbreak.cluster.api.ClusterApi.AMBARI;
import static com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService.BEAN_POST_TAG;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cluster.api.ClusterPreCreationApi;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service(AMBARI + BEAN_POST_TAG)
public class AmbariPreCreationService implements ClusterPreCreationApi {

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Inject
    private AmbariSecurityConfigProvider ambariSecurityConfigProvider;

    @Inject
    private AmbariBlueprintPortConfigCollector ambariBlueprintPortConfigCollector;

    @Override
    public boolean isVdfReady(AmbariRepo ambariRepo) {
        return ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(ambariRepo::getVersion, AMBARI_VERSION_2_6_0_0);
    }

    @Override
    public boolean isLdapAndSSOReady(AmbariRepo ambariRepo) {
        return ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(ambariRepo::getVersion, AMBARI_VERSION_2_7_0_0);
    }

    @Override
    public String getCloudbreakClusterUserName(Cluster cluster) {
        return ambariSecurityConfigProvider.getCloudbreakClusterUserName(cluster);
    }

    @Override
    public String getCloudbreakClusterPassword(Cluster cluster) {
        return ambariSecurityConfigProvider.getCloudbreakClusterPassword(cluster);
    }

    @Override
    public String getDataplaneClusterUserName(Cluster cluster) {
        return ambariSecurityConfigProvider.getDataplaneClusterUserName(cluster);
    }

    @Override
    public String getDataplaneClusterPassword(Cluster cluster) {
        return ambariSecurityConfigProvider.getDataplaneClusterPassword(cluster);
    }

    @Override
    public String getClusterUserProvidedPassword(Cluster cluster) {
        return ambariSecurityConfigProvider.getClusterUserProvidedPassword(cluster);
    }

    @Override
    public String getCertPath() {
        return ambariSecurityConfigProvider.getCertPath();
    }

    @Override
    public String getKeystorePath() {
        return ambariSecurityConfigProvider.getKeystorePath();
    }

    @Override
    public String getKeystorePassword() {
        return ambariSecurityConfigProvider.getKeystorePassword();
    }

    @Override
    public String getMasterKey(Cluster cluster) {
        return ambariSecurityConfigProvider.getMasterKey(cluster);
    }

    @Override
    public Map<String, Integer> getServicePorts(Blueprint blueprint) {
        return ambariBlueprintPortConfigCollector.getServicePorts(blueprint);
    }
}
