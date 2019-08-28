package com.sequenceiq.freeipa.api.client.internal;

public class FreeIpaApiClientParams {
    private boolean restDebug;

    private boolean certificateValidation;

    private boolean ignorePreValidation;

    private String freeIpaServerUrl;

    public FreeIpaApiClientParams(boolean restDebug, boolean certificateValidation, boolean ignorePreValidation, String freeIpaServerUrl) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
        this.freeIpaServerUrl = freeIpaServerUrl;
    }

    public String getServiceUrl() {
        return freeIpaServerUrl;
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
