package com.sequenceiq.cloudbreak.service.cluster;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Cluster;

@Service
public class AmbariAuthenticationProvider {

    public String getAmbariUserName(Cluster cluster) {
        return cluster.getUserName();
    }

    public String getAmbariPassword(Cluster cluster) {
        return cluster.getPassword();
    }
}