package com.sequenceiq.cloudbreak.orchestrator.model;

public class OrchestrationCredential {

    private String publicApiAddress;
    private String privateApiAddress;
    private String tlsCertificateDir;

    public OrchestrationCredential(String publicApiAddress, String privateApiAddress, String tlsCertificateDir) {
        this.publicApiAddress = publicApiAddress;
        this.privateApiAddress = privateApiAddress;
        this.tlsCertificateDir = tlsCertificateDir;
    }

    public String getPublicApiAddress() {
        return publicApiAddress;
    }

    public String getPrivateApiAddress() {
        return privateApiAddress;
    }

    public String getTlsCertificateDir() {
        return tlsCertificateDir;
    }
}
