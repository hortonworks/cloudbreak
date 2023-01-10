package com.sequenceiq.cloudbreak.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadLocalUserCrnWebTargetBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadLocalUserCrnWebTargetBuilder.class);

    private static final int TWO_MINUTES_IN_MILLIS = 2 * 60 * 1000;

    private final String serviceAddress;

    private boolean debug;

    private boolean secure = true;

    private boolean ignorePreValidation;

    private String apiRoot;

    private ClientRequestFilter clientRequestFilter;

    public ThreadLocalUserCrnWebTargetBuilder(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public ThreadLocalUserCrnWebTargetBuilder withDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public ThreadLocalUserCrnWebTargetBuilder withCertificateValidation(boolean secure) {
        this.secure = secure;
        return this;
    }

    public ThreadLocalUserCrnWebTargetBuilder withIgnorePreValidation(boolean ignorePreValidation) {
        this.ignorePreValidation = ignorePreValidation;
        return this;
    }

    public ThreadLocalUserCrnWebTargetBuilder withApiRoot(String apiRoot) {
        this.apiRoot = apiRoot;
        return this;
    }

    public ThreadLocalUserCrnWebTargetBuilder withClientRequestFilter(ClientRequestFilter clientRequestFilter) {
        this.clientRequestFilter = clientRequestFilter;
        return this;
    }

    public WebTarget build() {
        ConfigKey configKey = new ConfigKey(secure, debug, ignorePreValidation, TWO_MINUTES_IN_MILLIS);
        Client client = RestClientUtil.get(configKey);
        client.register(clientRequestFilter);
        WebTarget webTarget = client.target(serviceAddress).path(apiRoot);
        LOGGER.info("WebTarget has been created with token: service address: {}, configKey: {}", serviceAddress, configKey);
        return webTarget;
    }
}
