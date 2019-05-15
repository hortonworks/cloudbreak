package com.sequenceiq.environment.client;

import java.util.Collections;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.restclient.RestClientUtil;
import com.sequenceiq.environment.api.credential.endpoint.CredentialV1Endpoint;
import com.sequenceiq.environment.api.environment.endpoint.EnvironmentV1Endpoint;
import com.sequenceiq.environment.api.proxy.endpoint.ProxyV1Endpoint;

class EnvironmentUserCrnClient implements EnvironmentClient {
    protected static final Form EMPTY_FORM = new Form();

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUserCrnClient.class);

    private WebTarget webTarget;

    private WebToken crn;

    EnvironmentUserCrnClient(String environmentAddress, UserCrnConfigKey userCrnConfigKey) {
        Client client = RestClientUtil.get(userCrnConfigKey);
        webTarget = client.target(environmentAddress);
        crn = userCrnConfigKey.getToken();
        LOGGER.info("EnvironmentUserCrnClient has been created with token. environment service: {}, userCrnConfigKey: {}", environmentAddress,
                userCrnConfigKey);
    }

    public CredentialV1Endpoint credentialV1Endpoint() {
        return getEndpoint(CredentialV1Endpoint.class);
    }

    public ProxyV1Endpoint proxyV1Endpoint() {
        return getEndpoint(ProxyV1Endpoint.class);
    }

    public EnvironmentV1Endpoint environmentV1Endpoint() {
        return getEndpoint(EnvironmentV1Endpoint.class);
    }

    protected <E> E getEndpoint(Class<E> clazz) {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(crn.getHeader(), crn.getToken());
        return newEndpoint(clazz, headers);
    }

    private <C> C newEndpoint(Class<C> resourceInterface, MultivaluedMap<String, Object> headers) {
        return WebResourceFactory.newResource(resourceInterface, webTarget, false, headers, Collections.emptyList(), EMPTY_FORM);
    }
}

