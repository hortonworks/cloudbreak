package com.sequenceiq.cloudbreak.service.cluster.ambari;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.VaultService;

@Service
public class AmbariSecurityConfigProvider {

    private static final String DEFAULT_AMBARI_SECURITY_MASTER_KEY = "bigdata";

    @Inject
    private VaultService vaultService;

    public String getAmbariUserName(Cluster cluster) {
        if (Strings.isNullOrEmpty(cluster.getCloudbreakAmbariUser())) {
            return cluster.getUserName();
        }
        return vaultService.resolveSingleValue(cluster.getCloudbreakAmbariUser());
    }

    public String getAmbariPassword(Cluster cluster) {
        if (Strings.isNullOrEmpty(cluster.getCloudbreakAmbariPassword())) {
            return cluster.getPassword();
        }
        return vaultService.resolveSingleValue(cluster.getCloudbreakAmbariPassword());
    }

    public String getAmbariUserProvidedPassword(Cluster cluster) {
        return cluster.getPassword();
    }

    public String getAmbariSecurityMasterKey(Cluster cluster) {
        String securityMasterKey = cluster.getAmbariSecurityMasterKey();
        if (Strings.isNullOrEmpty(securityMasterKey)) {
            securityMasterKey = DEFAULT_AMBARI_SECURITY_MASTER_KEY;
        }
        return vaultService.resolveSingleValue(securityMasterKey);
    }
}
