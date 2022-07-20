package com.sequenceiq.cloudbreak.cm;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.service.AbstractClusterSecurityProvider;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
public class ClouderaManagerSecurityConfigProvider extends AbstractClusterSecurityProvider {
    @Override
    public String getCertPath() {
        return null;
    }

    @Override
    public String getKeystorePath() {
        return null;
    }

    @Override
    public String getKeystorePassword() {
        return null;
    }

    @Override
    public String getMasterKey(ClusterView cluster) {
        return "cmmasterpw";
    }
}
