package com.sequenceiq.cloudbreak.cluster.service;

/**
 * Gives access to the user facing load balancer FQDN of a stack for the cluster manager connectors, which cannot depend on the core module
 * where the load balancers are persisted.
 */
public interface LoadBalancerUserFacingFqdnProvider {

    /**
     * @return the FQDN (or DNS name / IP when no FQDN is registered) of the user facing load balancer of the stack, or null if there is none
     */
    String getLoadBalancerUserFacingFQDN(Long stackId);
}
