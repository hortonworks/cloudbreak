package com.sequenceiq.cloudbreak.service.cluster.ambari;

import org.springframework.stereotype.Service;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service
public class AmbariSecurityConfigProvider {

    private static final String DEFAULT_AMBARI_SECURITY_MASTER_KEY = "bigdata";

    public String getAmbariUserName(Cluster cluster) {
        if (Strings.isNullOrEmpty(cluster.getCloudbreakAmbariUser().getRaw())) {
            return cluster.getUserName().getRaw();
        }
        return cluster.getCloudbreakAmbariUser().getRaw();
    }

    public String getAmbariPassword(Cluster cluster) {
        if (Strings.isNullOrEmpty(cluster.getCloudbreakAmbariPassword().getRaw())) {
            return cluster.getPassword().getRaw();
        }
        return cluster.getCloudbreakAmbariPassword().getRaw();
    }

    public String getAmbariUserProvidedPassword(Cluster cluster) {
        return cluster.getPassword() == null ? null : cluster.getPassword().getRaw();
    }

    public String getAmbariSecurityMasterKey(Cluster cluster) {
        String securityMasterKey = cluster.getAmbariSecurityMasterKey().getRaw();
        return Strings.isNullOrEmpty(securityMasterKey) ? DEFAULT_AMBARI_SECURITY_MASTER_KEY : securityMasterKey;
    }
}
