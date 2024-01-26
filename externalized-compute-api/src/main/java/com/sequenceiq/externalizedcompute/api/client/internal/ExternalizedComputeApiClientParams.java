package com.sequenceiq.externalizedcompute.api.client.internal;

public class ExternalizedComputeApiClientParams {

    private boolean restDebug;

    private boolean certificateValidation;

    private boolean ignorePreValidation;

    private String externalizedComputeServerUrl;

    public ExternalizedComputeApiClientParams(boolean restDebug, boolean certificateValidation,
            boolean ignorePreValidation, String externalizedComputeServerUrl) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
        this.externalizedComputeServerUrl = externalizedComputeServerUrl;
    }

    public String getServiceUrl() {
        return externalizedComputeServerUrl;
    }

    public boolean isCertificateValidation() {
        return certificateValidation;
    }

    public boolean isIgnorePreValidation() {
        return ignorePreValidation;
    }

    public boolean isRestDebug() {
        return restDebug;
    }
}
