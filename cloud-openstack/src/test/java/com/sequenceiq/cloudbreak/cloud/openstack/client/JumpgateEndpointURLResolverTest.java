package com.sequenceiq.cloudbreak.cloud.openstack.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.model.identity.URLResolverParams;
import org.openstack4j.model.identity.v3.Token;

class JumpgateEndpointURLResolverTest {

    private static final String PROXY_BASE_URL = "http://localhost:10180/cluster-proxy/proxy/crn:cdp:openstack-jumpgate:us-west-1:acc:jumpgate:cred";

    @Test
    void findURLV3ShouldReturnProxyUrlWithServiceName() {
        JumpgateEndpointURLResolver resolver = new JumpgateEndpointURLResolver(PROXY_BASE_URL);

        URLResolverParams params = URLResolverParams.create((Token) null, ServiceType.COMPUTE);
        String result = resolver.findURLV3(params);

        assertEquals(PROXY_BASE_URL + "/nova", result);
    }

    @Test
    void findURLV2ShouldReturnProxyUrlWithServiceName() {
        JumpgateEndpointURLResolver resolver = new JumpgateEndpointURLResolver(PROXY_BASE_URL);

        URLResolverParams params = URLResolverParams.create((Token) null, ServiceType.NETWORK);
        String result = resolver.findURLV2(params);

        assertEquals(PROXY_BASE_URL + "/neutron", result);
    }

    @Test
    void findURLV3ShouldResolveCinderCorrectly() {
        JumpgateEndpointURLResolver resolver = new JumpgateEndpointURLResolver(PROXY_BASE_URL);

        URLResolverParams params = URLResolverParams.create((Token) null, ServiceType.BLOCK_STORAGE);
        String result = resolver.findURLV3(params);

        assertEquals(PROXY_BASE_URL + "/cinder", result);
    }

    @Test
    void findURLV3ShouldResolveImageService() {
        JumpgateEndpointURLResolver resolver = new JumpgateEndpointURLResolver(PROXY_BASE_URL);

        URLResolverParams params = URLResolverParams.create((Token) null, ServiceType.IMAGE);
        String result = resolver.findURLV3(params);

        assertEquals(PROXY_BASE_URL + "/glance", result);
    }

    @Test
    void findURLV3ShouldResolveIdentityService() {
        JumpgateEndpointURLResolver resolver = new JumpgateEndpointURLResolver(PROXY_BASE_URL);

        URLResolverParams params = URLResolverParams.create((Token) null, ServiceType.IDENTITY);
        String result = resolver.findURLV3(params);

        assertEquals(PROXY_BASE_URL + "/keystone", result);
    }
}
