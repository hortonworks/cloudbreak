package com.sequenceiq.periscope.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

@Service
public class AmbariClientProvider {

    @Autowired
    private TlsSecurityService tlsSecurityService;

    public AmbariClient createAmbariClient(Cluster cluster) {
        if (cluster.getStackId() != null) {
            TlsConfiguration tlsConfig = tlsSecurityService.getConfiguration(cluster);
            return new AmbariClient(cluster.getHost(),
                    cluster.getPort(),
                    cluster.getAmbariUser(),
                    cluster.getAmbariPass(),
                    tlsConfig.getClientCertPath(),
                    tlsConfig.getClientKeyPath(),
                    tlsConfig.getServerCertPath());
        } else {
            return new AmbariClient(cluster.getHost(), cluster.getPort(), cluster.getAmbariUser(), cluster.getAmbariPass());
        }
    }
}
