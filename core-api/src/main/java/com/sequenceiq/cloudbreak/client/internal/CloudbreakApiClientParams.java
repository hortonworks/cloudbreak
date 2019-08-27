package com.sequenceiq.cloudbreak.client.internal;

public class CloudbreakApiClientParams {

    private final boolean restDebug;

    private final boolean certificateValidation;

    private final boolean ignorePreValidation;

    private final String cloudbreakServerUrl;

    public CloudbreakApiClientParams(boolean restDebug, boolean certificateValidation, boolean ignorePreValidation, String cloudbreakServerUrl) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
        this.cloudbreakServerUrl = cloudbreakServerUrl;
    }

    public String getServiceUrl() {
        return cloudbreakServerUrl;
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
