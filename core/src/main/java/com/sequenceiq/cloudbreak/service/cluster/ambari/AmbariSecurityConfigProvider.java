package com.sequenceiq.cloudbreak.service.cluster.ambari;

import org.springframework.stereotype.Service;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service
public class AmbariSecurityConfigProvider {

    private static final String DEFAULT_AMBARI_SECURITY_MASTER_KEY = "bigdata";

    public String getAmbariUserName(Cluster cluster) {
        if (Strings.isNullOrEmpty(cluster.getCloudbreakAmbariUser())) {
            return cluster.getUserName();
        }
        return cluster.getCloudbreakAmbariUser();
    }

    public String getAmbariPassword(Cluster cluster) {
        if (Strings.isNullOrEmpty(cluster.getCloudbreakAmbariPassword())) {
            return cluster.getPassword();
        }
        return cluster.getCloudbreakAmbariPassword();
    }

    public String getAmbariSecurityMasterKey(Cluster cluster) {
        String securityMasterKey = cluster.getAmbariSecurityMasterKey();
        if (Strings.isNullOrEmpty(securityMasterKey)) {
            securityMasterKey = DEFAULT_AMBARI_SECURITY_MASTER_KEY;
        }
        return securityMasterKey;
    }
}
