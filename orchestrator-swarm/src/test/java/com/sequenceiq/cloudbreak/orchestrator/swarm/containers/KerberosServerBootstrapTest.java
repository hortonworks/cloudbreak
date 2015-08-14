package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration;

public class KerberosServerBootstrapTest extends AbstractContainerBootstrapTest {

    @Override
    public ContainerBootstrap getTestInstance() {
        return new KerberosServerBootstrap(getMockedDockerClient(), DUMMY_IMAGE, DUMMY_NODE, DUMMY_LOG_VOLUME,
                new KerberosConfiguration("masterkey", "admin", "password"));
    }
}
