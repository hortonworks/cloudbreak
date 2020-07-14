package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;

public abstract class SaltFileUpload implements OrchestratorBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaltFileUpload.class);

    private final SaltConnector sc;

    private final Set<String> originalTargets;

    private Set<String> targets;

    public SaltFileUpload(SaltConnector sc, Set<String> targets) {
        this.sc = sc;
        originalTargets = targets;
        this.targets = targets;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.debug("Uploading files to: {}", targets);
        if (!targets.isEmpty()) {
            LOGGER.debug("Current targets for upload: {}", targets);

            GenericResponses responses = upload();

            Set<String> failedTargets = new HashSet<>();

            LOGGER.debug("Salt file upload responses: {}", responses);
            for (GenericResponse genericResponse : responses.getResponses()) {
                if (genericResponse.getStatusCode() != HttpStatus.CREATED.value()) {
                    LOGGER.info("Failed upload attempt to: {}, error: {}", genericResponse.getAddress(), genericResponse.getErrorText());
                    String address = genericResponse.getAddress().split(":")[0];
                    failedTargets.addAll(originalTargets.stream().filter(a -> a.equals(address)).collect(Collectors.toList()));
                }
            }
            targets = failedTargets;

            if (!targets.isEmpty()) {
                LOGGER.info("Missing nodes for file upload: {}", targets);
                throw new CloudbreakOrchestratorFailedException("There are missing nodes for file upload: " + targets);
            }
        }

        LOGGER.debug("File upload has been completed on nodes: {}", originalTargets);
        return true;
    }

    public SaltConnector getSaltConnector() {
        return sc;
    }

    public Set<String> getOriginalTargets() {
        return originalTargets;
    }

    public Set<String> getTargets() {
        return targets;
    }

    abstract GenericResponses upload() throws IOException;
}
