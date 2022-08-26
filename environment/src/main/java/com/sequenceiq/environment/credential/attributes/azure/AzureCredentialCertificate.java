package com.sequenceiq.environment.credential.attributes.azure;

public class AzureCredentialCertificate {

    private String id;

    private String status;

    private Long expiration;

    private String certificate;

    private String privateKey;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public String toString() {
        return "AzureCredentialCertificate{" +
                "id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", expiration=" + expiration +
                ", certificate='" + certificate + '\'' +
                ", privateKey='***'" +
                '}';
    }
}
