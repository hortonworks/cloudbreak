package com.sequenceiq.cloudbreak.client;

import java.util.Collections;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.client.proxy.WebResourceFactory;

public abstract class AbstractKeyBasedServiceEndpoint {

    private static final Form EMPTY_FORM = new Form();

    private WebTarget webTarget;

    private String accessKey;

    private String secretKey;

    protected AbstractKeyBasedServiceEndpoint(WebTarget webTarget, String accessKey, String secretKey) {
        this.webTarget = webTarget;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    protected <E> E getEndpoint(Class<E> clazz) {
        return newEndpoint(clazz, new MultivaluedHashMap<>());
    }

    private <C> C newEndpoint(Class<C> resourceInterface, MultivaluedMap<String, Object> headers) {
        webTarget.register(new ApiKeyRequestFilter(accessKey, secretKey));
        return WebResourceFactory.newResource(resourceInterface, webTarget, false, headers, Collections.emptyList(), EMPTY_FORM);
    }

}
