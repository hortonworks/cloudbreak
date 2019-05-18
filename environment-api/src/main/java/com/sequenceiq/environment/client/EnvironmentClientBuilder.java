package com.sequenceiq.environment.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentClientBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentClientBuilder.class);

    private final String environmentAddress;

    private boolean debug;

    private boolean secure = true;

    private boolean ignorePreValidation;

    private WebToken token;

    public EnvironmentClientBuilder(String environmentAddress) {
        this.environmentAddress = environmentAddress;
    }

    public EnvironmentClientBuilder withDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public EnvironmentClientBuilder withCertificateValidation(boolean secure) {
        this.secure = secure;
        return this;
    }

    public EnvironmentClientBuilder withIgnorePreValidation(boolean ignorePreValidation) {
        this.ignorePreValidation = ignorePreValidation;
        return this;
    }

    public  EnvironmentClientBuilder withCrnToken(WebToken token) {
        this.token = token;
        return this;
    }

    public EnvironmentClient build() {
        UserCrnConfigKey userCrnConfigKey = new UserCrnConfigKey(secure, debug, ignorePreValidation, token);
        return new EnvironmentUserCrnClient(environmentAddress, userCrnConfigKey);
    }
}
