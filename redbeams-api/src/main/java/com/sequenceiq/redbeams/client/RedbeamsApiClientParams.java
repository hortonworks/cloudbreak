package com.sequenceiq.redbeams.client;

public class RedbeamsApiClientParams {

    private boolean restDebug;

    private boolean certificateValidation;

    private boolean ignorePreValidation;

    private String redbeamsServerUrl;

    public RedbeamsApiClientParams(boolean restDebug, boolean certificateValidation, boolean ignorePreValidation, String redbeamsServerUrl) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
        this.redbeamsServerUrl = redbeamsServerUrl;
    }

    public String getServiceUrl() {
        return redbeamsServerUrl;
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
