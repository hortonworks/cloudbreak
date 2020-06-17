package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;

public class FingerprintsResponse {

    private List<Fingerprint> fingerprints;

    private String errorText;

    private int statusCode;

    public List<Fingerprint> getFingerprints() {
        return fingerprints;
    }

    public void setFingerprints(List<Fingerprint> fingerprints) {
        this.fingerprints = fingerprints;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        return "FingerprintsResponse{" +
                "fingerprints=" + fingerprints +
                ", errorText='" + errorText + '\'' +
                ", statusCode=" + statusCode +
                '}';
    }
}
