package com.sequenceiq.periscope.client;

import com.sequenceiq.cloudbreak.client.ConfigKey;

public class AutoscaleUserCrnClientBuilder {
    private final String autoscaleAddress;

    private boolean debug;

    private boolean secure = true;

    private boolean ignorePreValidation;

    public AutoscaleUserCrnClientBuilder(String autoscaleAddress) {
        this.autoscaleAddress = autoscaleAddress;
    }

    public AutoscaleUserCrnClientBuilder withDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public AutoscaleUserCrnClientBuilder withCertificateValidation(boolean secure) {
        this.secure = secure;
        return this;
    }

    public AutoscaleUserCrnClientBuilder withIgnorePreValidation(boolean ignorePreValidation) {
        this.ignorePreValidation = ignorePreValidation;
        return this;
    }

    public AutoscaleUserCrnClient build() {
        ConfigKey configKey = new ConfigKey(secure, debug, ignorePreValidation);
        return new AutoscaleUserCrnClient(autoscaleAddress, configKey);
    }
}
