package com.sequenceiq.consumption.client.internal;

public class ConsumptionApiClientParams {

    private final boolean restDebug;

    private final boolean certificateValidation;

    private final boolean ignorePreValidation;

    private final String billingServerUrl;

    public ConsumptionApiClientParams(boolean restDebug, boolean certificateValidation, boolean ignorePreValidation, String billingServerUrl) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
        this.billingServerUrl = billingServerUrl;
    }

    public String getServiceUrl() {
        return billingServerUrl;
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
