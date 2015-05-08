package com.sequenceiq.cloudbreak.orchestrator.containers;

public interface ContainerBootstrap {

    Boolean call() throws Exception;
}
