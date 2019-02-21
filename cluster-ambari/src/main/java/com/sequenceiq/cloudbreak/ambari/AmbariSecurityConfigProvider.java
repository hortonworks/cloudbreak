package com.sequenceiq.cloudbreak.ambari;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.service.AbstractClusterSecurityProvider;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service
public class AmbariSecurityConfigProvider extends AbstractClusterSecurityProvider {

    private static final String DEFAULT_AMBARI_SECURITY_MASTER_KEY = "bigdata";

    @Value("${cb.ambari.ldaps.certPath}")
    private String certPath;

    @Value("${cb.ambari.ldaps.keystorePath}")
    private String keystorePath;

    @Value("${cb.ambari.ldaps.keystorePassword}")
    private String keystorePassword;

    public String getCertPath() {
        return certPath;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getMasterKey(Cluster cluster) {
        String securityMasterKey = cluster.getAmbariSecurityMasterKey();
        return StringUtils.isBlank(securityMasterKey) ? DEFAULT_AMBARI_SECURITY_MASTER_KEY : securityMasterKey;
    }
}
