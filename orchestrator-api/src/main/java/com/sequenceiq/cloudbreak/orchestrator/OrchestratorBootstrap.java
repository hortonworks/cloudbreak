package com.sequenceiq.cloudbreak.orchestrator;

import java.util.Collection;
import java.util.Optional;

public interface OrchestratorBootstrap {

    Optional<Collection<String>> call() throws Exception;
}
