package com.sequenceiq.it.cloudbreak.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TrustProperties {

    @Value("${integrationtest.trust.remoteEnvironmentCrn}")
    private String remoteEnvironmentCrn;

    @Value("${integrationtest.trust.hdfsPath}")
    private String hdfsPath;

    @Value("${integrationtest.trust.activedirectory.fqdn}")
    private String activeDirectoryFqdn;

    @Value("${integrationtest.trust.activedirectory.ip}")
    private String activeDirectoryIp;

    @Value("${integrationtest.trust.activedirectory.user}")
    private String activeDirectoryUser;

    @Value("${integrationtest.trust.activedirectory.realm}")
    private String activeDirectoryRealm;

    public String getRemoteEnvironmentCrn(String accountId) {
        return remoteEnvironmentCrn.replaceAll("ACCOUNT_ID", accountId);
    }

    public String getHdfsPath() {
        return hdfsPath;
    }

    public String getActiveDirectoryFqdn() {
        return activeDirectoryFqdn;
    }

    public String getActiveDirectoryIp() {
        return activeDirectoryIp;
    }

    public String getActiveDirectoryUser() {
        return activeDirectoryUser;
    }

    public String getActiveDirectoryRealm() {
        return activeDirectoryRealm;
    }
}
