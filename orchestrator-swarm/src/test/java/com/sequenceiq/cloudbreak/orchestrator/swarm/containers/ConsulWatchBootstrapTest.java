package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class ConsulWatchBootstrapTest extends AbstractContainerBootstrapTest {

    @Override
    public ContainerBootstrap getTestInstance() {
        Node node = mock(Node.class);
        when(node.getHostname()).thenReturn(DUMMY_GENERATED_ID);
        return new ConsulWatchBootstrap(getMockedDockerClient(), DUMMY_IMAGE, node, DUMMY_NODE, getMockedDockerClientUtil());
    }
}
