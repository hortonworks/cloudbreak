package com.sequenceiq.freeipa.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Cert {
    private String subject;

    private String cacn;

    @JsonProperty("serial_number")
    private long serialNumber;

    @JsonProperty("valid_not_before")
    private String validNotBefore;

    @JsonProperty("valid_not_after")
    private String validNotAfter;

    private String issuer;

    private boolean revoked;

    private String status;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCacn() {
        return cacn;
    }

    public void setCacn(String cacn) {
        this.cacn = cacn;
    }

    public long getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(long serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getValidNotBefore() {
        return validNotBefore;
    }

    public void setValidNotBefore(String validNotBefore) {
        this.validNotBefore = validNotBefore;
    }

    public String getValidNotAfter() {
        return validNotAfter;
    }

    public void setValidNotAfter(String validNotAfter) {
        this.validNotAfter = validNotAfter;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Cert{"
                + "subject='" + subject + '\''
                + ", cacn='" + cacn + '\''
                + ", serialNumber=" + serialNumber
                + ", validNotBefore='" + validNotBefore + '\''
                + ", validNotAfter='" + validNotAfter + '\''
                + ", issuer='" + issuer + '\''
                + ", revoked=" + revoked
                + ", status='" + status + '\''
                + '}';
    }
}
