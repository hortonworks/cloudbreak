package com.sequenceiq.cloudbreak.service.cluster.ambari;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service
public class AmbariSecurityConfigProvider {

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

    public String getCloudbreakAmbariUserName(Cluster cluster) {
        return cluster.getCloudbreakAmbariUser();
    }

    public String getCloudbreakAmbariPassword(Cluster cluster) {
        return cluster.getCloudbreakAmbariPassword();
    }

    public String getDataplaneAmbariUserName(Cluster cluster) {
        return cluster.getDpAmbariUser();
    }

    public String getDataplaneAmbariPassword(Cluster cluster) {
        return cluster.getDpAmbariPassword();
    }

    public String getAmbariUserProvidedPassword(Cluster cluster) {
        return cluster.getPassword();
    }

    public String getAmbariSecurityMasterKey(Cluster cluster) {
        String securityMasterKey = cluster.getAmbariSecurityMasterKey();
        return Strings.isNullOrEmpty(securityMasterKey) ? DEFAULT_AMBARI_SECURITY_MASTER_KEY : securityMasterKey;
    }
}
