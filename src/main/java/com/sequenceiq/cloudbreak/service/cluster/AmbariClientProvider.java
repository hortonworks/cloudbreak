package com.sequenceiq.cloudbreak.service.cluster;

import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;

@Service
public class AmbariClientProvider {

    private static final String PORT = "8080";

    public AmbariClient getAmbariClient(String ambariServerIP, String ambariUserName, String ambariPassword) {
        return new AmbariClient(ambariServerIP, PORT, ambariUserName, ambariPassword);
    }

    public AmbariClient getDefaultAmbariClient(String ambariAddress) {
        return new AmbariClient(ambariAddress);
    }
}
