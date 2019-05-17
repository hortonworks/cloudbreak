package com.sequenceiq.redbeams.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

public class RedbeamsClient {

    private static final Form EMPTY_FORM = new Form();

    private static final String TOKEN_KEY = "TOKEN";

    private final Logger logger = LoggerFactory.getLogger(RedbeamsClient.class);

    private final Client client;

    private final String redbeamsAddress;

    private WebTarget webTarget;

    private EndpointHolder endpointHolder;

    private RedbeamsClient(String redbeamsAddress, ConfigKey configKey) {
        client = RestClientUtil.get(configKey);
        this.redbeamsAddress = redbeamsAddress;
        logger.info("RedbeamsClient has been created with token. redbeams: {}, configKey: {}", redbeamsAddress, configKey);
    }

    public DatabaseV4Endpoint databaseEndpoint() {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        return newEndpoint(DatabaseV4Endpoint.class, headers);
    }

    private ExpiringMap<String, String> configTokenCache() {
        return ExpiringMap.builder().variableExpiration().expirationPolicy(ExpirationPolicy.CREATED).build();
    }

    private <C> C newEndpoint(Class<C> resourceInterface, MultivaluedMap<String, Object> headers) {
        return WebResourceFactory.newResource(resourceInterface, webTarget, false, headers, Collections.emptyList(), EMPTY_FORM);
    }

    private static class EndpointHolder {
        private final List<?> endpoints;

        EndpointHolder(Object... endpoints) {
            this.endpoints = Arrays.asList(endpoints);
        }
    }

    public static class RedbeamsClientBuilder {

        private final String redbeamsAddress;

        private boolean debug;

        private boolean secure = true;

        private boolean ignorePreValidation;

        public RedbeamsClientBuilder(String redbeamsAddress, String caasProtocol, String caasAddress) {
            this.redbeamsAddress = redbeamsAddress;
        }

        public RedbeamsClientBuilder withDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public RedbeamsClientBuilder withCertificateValidation(boolean secure) {
            this.secure = secure;
            return this;
        }

        public RedbeamsClientBuilder withIgnorePreValidation(boolean ignorePreValidation) {
            this.ignorePreValidation = ignorePreValidation;
            return this;
        }

        public RedbeamsClient build() {
            ConfigKey configKey = new ConfigKey(secure, debug, ignorePreValidation);
            return new RedbeamsClient(redbeamsAddress, configKey);
        }
    }
}
