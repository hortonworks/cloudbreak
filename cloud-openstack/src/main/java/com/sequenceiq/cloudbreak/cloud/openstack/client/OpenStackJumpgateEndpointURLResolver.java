package com.sequenceiq.cloudbreak.cloud.openstack.client;

import org.openstack4j.api.identity.EndpointURLResolver;
import org.openstack4j.model.identity.URLResolverParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenStackJumpgateEndpointURLResolver implements EndpointURLResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackJumpgateEndpointURLResolver.class);

    private final String clusterProxyBaseUrl;

    public OpenStackJumpgateEndpointURLResolver(String clusterProxyBaseUrl) {
        this.clusterProxyBaseUrl = clusterProxyBaseUrl;
    }

    @Override
    public String findURLV2(URLResolverParams params) {
        return resolveProxyUrl(params.type.getServiceName());
    }

    @Override
    public String findURLV3(URLResolverParams params) {
        return resolveProxyUrl(params.type.getServiceName());
    }

    private String resolveProxyUrl(String serviceName) {
        String resolvedUrl = clusterProxyBaseUrl + "/" + serviceName;
        LOGGER.debug("Jumpgate URL resolved [{}]: {}", serviceName, resolvedUrl);
        return resolvedUrl;
    }
}
