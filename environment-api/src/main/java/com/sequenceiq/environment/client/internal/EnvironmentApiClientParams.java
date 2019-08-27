package com.sequenceiq.environment.client.internal;

public class EnvironmentApiClientParams {

    private final boolean restDebug;

    private final boolean certificateValidation;

    private final boolean ignorePreValidation;

    private final String environmentServerUrl;

    public EnvironmentApiClientParams(boolean restDebug, boolean certificateValidation, boolean ignorePreValidation, String environmentServerUrl) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
        this.environmentServerUrl = environmentServerUrl;
    }

    public String getServiceUrl() {
        return environmentServerUrl;
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
