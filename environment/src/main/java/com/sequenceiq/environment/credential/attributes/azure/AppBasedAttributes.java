package com.sequenceiq.environment.credential.attributes.azure;

import com.sequenceiq.common.api.credential.AppAuthenticationType;

public class AppBasedAttributes {

    private String accessKey;

    private String secretKey;

    private AppAuthenticationType authenticationType;

    private AzureCredentialCertificate certificate;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public AppAuthenticationType getAuthenticationType() {
        return authenticationType == null ? AppAuthenticationType.SECRET : authenticationType;
    }

    public void setAuthenticationType(AppAuthenticationType authenticationType) {
        this.authenticationType = authenticationType;
    }

    public AzureCredentialCertificate getCertificate() {
        return certificate;
    }

    public void setCertificate(AzureCredentialCertificate certificate) {
        this.certificate = certificate;
    }
}
