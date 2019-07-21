package com.sequenceiq.cloudbreak.client;

public abstract class AbstractUserCrnServiceClientBuilder<T extends AbstractUserCrnServiceClient> {
    private final String serviceAddress;

    private boolean debug;

    private boolean secure = true;

    private boolean ignorePreValidation;

    public AbstractUserCrnServiceClientBuilder(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public AbstractUserCrnServiceClientBuilder<T> withDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public AbstractUserCrnServiceClientBuilder<T> withCertificateValidation(boolean secure) {
        this.secure = secure;
        return this;
    }

    public AbstractUserCrnServiceClientBuilder<T> withIgnorePreValidation(boolean ignorePreValidation) {
        this.ignorePreValidation = ignorePreValidation;
        return this;
    }

    public T build() {
        return createUserCrnClient(serviceAddress, new ConfigKey(secure, debug, ignorePreValidation));
    }

    protected abstract T createUserCrnClient(String serviceAddress, ConfigKey configKey);
}
