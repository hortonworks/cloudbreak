package com.sequenceiq.consumption.client.internal;

public class ConsumptionApiClientParams {

    private final boolean restDebug;

    private final boolean certificateValidation;

    private final boolean ignorePreValidation;

    private final String consumptionServerUrl;

    public ConsumptionApiClientParams(boolean restDebug, boolean certificateValidation, boolean ignorePreValidation, String consumptionServerUrl) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
        this.consumptionServerUrl = consumptionServerUrl;
    }

    public String getServiceUrl() {
        return consumptionServerUrl;
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
