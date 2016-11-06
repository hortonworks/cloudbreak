package com.sequenceiq.cloudbreak.service.cluster;

import org.springframework.stereotype.Service;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.Cluster;

@Service
public class AmbariAuthenticationProvider {

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
}
