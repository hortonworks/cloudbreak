package com.sequenceiq.cloudbreak.service.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.Json;

class NetworkLoadBalancerAttributeUtilTest {

    @Test
    void testIsSessionStickyForTargetGroupLBNotPresent() {
        Json networkAttributes = new Json("{}");

        assertFalse(NetworkLoadBalancerAttributeUtil.isSessionStickyForTargetGroup(networkAttributes));
    }

    @Test
    void testIsSessionStickyForTargetGroupLBPresentButStickyNot() {
        Json networkAttributes = new Json("{\"loadBalancer\": {}}");

        assertFalse(NetworkLoadBalancerAttributeUtil.isSessionStickyForTargetGroup(networkAttributes));
    }

    @Test
    void testIsSessionStickyForTargetGroupLBAndStickyPresentWithFalse() {
        Json networkAttributes = new Json("{\"loadBalancer\": {\"stickySessionForLoadBalancerTarget\": false}}");

        assertFalse(NetworkLoadBalancerAttributeUtil.isSessionStickyForTargetGroup(networkAttributes));
    }

    @Test
    void testIsSessionStickyForTargetGroupLBAndStickyPresentWithTrue() {
        Json networkAttributes = new Json("{\"loadBalancer\": {\"stickySessionForLoadBalancerTarget\": true}}");

        assertTrue(NetworkLoadBalancerAttributeUtil.isSessionStickyForTargetGroup(networkAttributes));
    }

}