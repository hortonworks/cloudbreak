package com.sequenceiq.environment.credential.attributes.openstack;

public class DomainKeystoneV3Attributes extends KeystoneV3Base {

    private String domainName;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
}
