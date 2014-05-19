package com.sequenceiq.provisioning.domain;

public class CertificateRequest {

    private String subscriptionId;
    private String jksPassword;

    public CertificateRequest(CertificateRequest certificateRequest) {
        this.jksPassword = certificateRequest.jksPassword;
        this.subscriptionId = certificateRequest.subscriptionId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getJksPassword() {
        return jksPassword;
    }

    public void setJksPassword(String jksPassword) {
        this.jksPassword = jksPassword;
    }
}
