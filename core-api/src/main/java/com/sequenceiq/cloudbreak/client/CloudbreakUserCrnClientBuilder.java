package com.sequenceiq.cloudbreak.client;

public class CloudbreakUserCrnClientBuilder {

    private final String cloudbreakAddress;

    private boolean debug;

    private boolean secure = true;

    private boolean ignorePreValidation;

    public CloudbreakUserCrnClientBuilder(String cloudbreakAddress) {
        this.cloudbreakAddress = cloudbreakAddress;
    }

    public CloudbreakUserCrnClientBuilder withDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public CloudbreakUserCrnClientBuilder withCertificateValidation(boolean secure) {
        this.secure = secure;
        return this;
    }

    public CloudbreakUserCrnClientBuilder withIgnorePreValidation(boolean ignorePreValidation) {
        this.ignorePreValidation = ignorePreValidation;
        return this;
    }

    public CloudbreakUserCrnClient build() {
        ConfigKey configKey = new ConfigKey(secure, debug, ignorePreValidation);
        return new CloudbreakUserCrnClient(cloudbreakAddress, configKey);
    }
}
