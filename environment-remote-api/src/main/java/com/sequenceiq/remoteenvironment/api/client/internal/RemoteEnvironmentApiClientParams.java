package com.sequenceiq.remoteenvironment.api.client.internal;

public class RemoteEnvironmentApiClientParams {

    private boolean restDebug;

    private boolean certificateValidation;

    private boolean ignorePreValidation;

    private String serviceUrl;

    public RemoteEnvironmentApiClientParams(boolean restDebug, boolean certificateValidation,
                                            boolean ignorePreValidation, String remoteEnvironmentUrl) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
        this.serviceUrl = remoteEnvironmentUrl;
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

    public String getServiceUrl() {
        return serviceUrl;
    }
}
