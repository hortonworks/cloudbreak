package com.sequenceiq.remoteenvironment.api.client.internal;

public class RemoteEnvironmentApiClientParams {

    private boolean restDebug;

    private boolean certificateValidation;

    private boolean ignorePreValidation;

    public RemoteEnvironmentApiClientParams(boolean restDebug, boolean certificateValidation,
                                            boolean ignorePreValidation) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
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
