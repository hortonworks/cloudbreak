package com.sequenceiq.periscope.client.internal;

public class AutoscaleApiClientParams {
    private boolean restDebug;

    private boolean certificateValidation;

    private boolean ignorePreValidation;

    private String autoscaleServerUrl;

    private Integer connectionTimeout;

    private Integer readTimeout;

    public AutoscaleApiClientParams(boolean restDebug, boolean certificateValidation, boolean ignorePreValidation, String autoscaleServerUrl,
            Integer connectionTimeout, Integer readTimeout) {
        this.restDebug = restDebug;
        this.certificateValidation = certificateValidation;
        this.ignorePreValidation = ignorePreValidation;
        this.autoscaleServerUrl = autoscaleServerUrl;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public String getServiceUrl() {
        return autoscaleServerUrl;
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
