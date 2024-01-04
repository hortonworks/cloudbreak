package com.sequenceiq.cloudbreak.client;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadLocalUserCrnWebTargetBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadLocalUserCrnWebTargetBuilder.class);

    private static final int TWO_MINUTES_IN_MILLIS = 2 * 60 * 1000;

    private final String serviceAddress;

    private boolean debug;

    private Integer connectionTimeout;

    private Integer readTimeout;

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

    public ThreadLocalUserCrnWebTargetBuilder withClientConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public ThreadLocalUserCrnWebTargetBuilder withClientReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public WebTarget build() {
        ConfigKey configKey = new ConfigKey(secure, debug, ignorePreValidation, TWO_MINUTES_IN_MILLIS);
        Client client = RestClientUtil.get(configKey);
        client.register(clientRequestFilter);
        if (connectionTimeout != null) {
            client.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout);
        } else {
            client.property(ClientProperties.CONNECT_TIMEOUT, TWO_MINUTES_IN_MILLIS);
        }
        if (readTimeout != null) {
            client.property(ClientProperties.READ_TIMEOUT, readTimeout);
        } else {
            client.property(ClientProperties.READ_TIMEOUT, TWO_MINUTES_IN_MILLIS);
        }
        WebTarget webTarget = client.target(serviceAddress).path(apiRoot);
        LOGGER.info("WebTarget has been created with token: service address: {}, configKey: {}", serviceAddress, configKey);
        return webTarget;
    }
}
