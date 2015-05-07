package com.sequenceiq.cloudbreak.orcestrator.containers;

public interface ContainerBootstrap {

    Boolean call() throws Exception;
}
