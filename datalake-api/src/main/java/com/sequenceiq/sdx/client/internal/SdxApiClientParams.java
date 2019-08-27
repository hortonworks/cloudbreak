package com.sequenceiq.sdx.client.internal;

public class SdxApiClientParams {
    private boolean restDebug;

    private boolean certificateValidation;

    private boolean ignorePreValidation;

    private String sdxServerUrl;

    public SdxApiClientParams(boolean restDebug, boolean certificateValidation, boolean ignorePreValidation, String sdxServerUrl) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
        this.sdxServerUrl = sdxServerUrl;
    }

    public String getServiceUrl() {
        return sdxServerUrl;
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
