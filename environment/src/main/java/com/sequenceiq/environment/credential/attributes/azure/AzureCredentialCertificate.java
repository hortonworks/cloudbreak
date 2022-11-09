package com.sequenceiq.environment.credential.attributes.azure;

import com.sequenceiq.common.api.credential.AppCertificateStatus;

public class AzureCredentialCertificate {

    private AppCertificateStatus status;

    private Long expiration;

    private String certificate;

    private String sha512;

    private String privateKey;

    public AppCertificateStatus getStatus() {
        return status;
    }

    public void setStatus(AppCertificateStatus status) {
        this.status = status;
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getSha512() {
        return sha512;
    }

    public void setSha512(String sha512) {
        this.sha512 = sha512;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public String toString() {
        return "AzureCredentialCertificate{" +
                ", status='" + status + '\'' +
                ", expiration=" + expiration +
                ", certificate='" + certificate + '\'' +
                ", sha512='" + sha512 + '\'' +
                ", privateKey='***'" +
                '}';
    }
}
