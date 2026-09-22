package com.sequenceiq.maintenance.client.internal;

public class MaintenanceApiClientParams {

    private final boolean restDebug;

    private final boolean certificateValidation;

    private final boolean ignorePreValidation;

    private final String maintenanceServerUrl;

    public MaintenanceApiClientParams(
            boolean restDebug,
            boolean certificateValidation,
            boolean ignorePreValidation,
            String maintenanceServerUrl) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
        this.maintenanceServerUrl = maintenanceServerUrl;
    }

    public String getServiceUrl() {
        return maintenanceServerUrl;
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
