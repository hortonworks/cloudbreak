package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class BaywatchClientBootstrapTest extends AbstractContainerBootstrapTest {

    public static final String DUMMY_PRIVATE_IP = "privateIp";
    public static final String DUMMY_PUBLIC_IP = "publicIp";
    public static final String CONSUL_DOMAIN = ".node.dc1.consul";

    @Override
    public ContainerBootstrap getTestInstance() {
        return new BaywatchClientBootstrap(getMockedDockerClient(), DUMMY_GETAWAY_ADDRESS, DUMMY_IMAGE,
                DUMMY_GENERATED_ID, new Node(DUMMY_PRIVATE_IP, DUMMY_PUBLIC_IP), DUMMY_VOLUMES, CONSUL_DOMAIN,
                null, getMockedDockerClientUtil());
    }

    @Test
    public void testCallWhenExternLocationIsNotEmpty() throws Exception {
        // GIVEN
        BaywatchClientBootstrap underTest = new BaywatchClientBootstrap(getMockedDockerClient(), DUMMY_GETAWAY_ADDRESS, DUMMY_IMAGE,
                DUMMY_GENERATED_ID, new Node(DUMMY_PRIVATE_IP, DUMMY_PUBLIC_IP), DUMMY_VOLUMES, CONSUL_DOMAIN,
                "externLocation", getMockedDockerClientUtil());
        mockAll();
        // WHEN
        boolean result = underTest.call();
        // THEN
        assertTrue(result);
    }
}
