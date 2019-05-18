package com.sequenceiq.cloudbreak.client;

public abstract class AbstractUserCrnServiceClientBuilder {
    private final String serviceAddress;

    private boolean debug;

    private boolean secure = true;

    private boolean ignorePreValidation;

    public AbstractUserCrnServiceClientBuilder(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public AbstractUserCrnServiceClientBuilder withDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public AbstractUserCrnServiceClientBuilder withCertificateValidation(boolean secure) {
        this.secure = secure;
        return this;
    }

    public AbstractUserCrnServiceClientBuilder withIgnorePreValidation(boolean ignorePreValidation) {
        this.ignorePreValidation = ignorePreValidation;
        return this;
    }

    public <T extends AbstractUserCrnServiceClient> T build() {
        return createUserCrnClient(serviceAddress, new ConfigKey(secure, debug, ignorePreValidation));
    }

    protected abstract <T extends AbstractUserCrnServiceClient> T createUserCrnClient(String serviceAddress, ConfigKey configKey);
}
