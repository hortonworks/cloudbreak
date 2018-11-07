package com.sequenceiq.cloudbreak.service.cluster.ambari;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@Service
public class AmbariSecurityConfigProvider {

    private static final String DEFAULT_AMBARI_SECURITY_MASTER_KEY = "bigdata";

    @Inject
    private SecretService secretService;

    public String getAmbariUserName(Cluster cluster) {
        if (Strings.isNullOrEmpty(cluster.getCloudbreakAmbariUser())) {
            return secretService.get(cluster.getUserName());
        }
        return secretService.get(cluster.getCloudbreakAmbariUser());
    }

    public String getAmbariPassword(Cluster cluster) {
        if (Strings.isNullOrEmpty(cluster.getCloudbreakAmbariPassword())) {
            return secretService.get(cluster.getPassword());
        }
        return secretService.get(cluster.getCloudbreakAmbariPassword());
    }

    public String getAmbariUserProvidedPassword(Cluster cluster) {
        return cluster.getPassword() == null ? null : secretService.get(cluster.getPassword());
    }

    public String getAmbariSecurityMasterKey(Cluster cluster) {
        String securityMasterKey = cluster.getAmbariSecurityMasterKey();
        return Strings.isNullOrEmpty(securityMasterKey) ? DEFAULT_AMBARI_SECURITY_MASTER_KEY : secretService.get(securityMasterKey);
    }
}
