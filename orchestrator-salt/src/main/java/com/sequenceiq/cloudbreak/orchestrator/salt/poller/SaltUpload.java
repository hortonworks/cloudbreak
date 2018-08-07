package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

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

public class SaltUpload implements OrchestratorBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltUpload.class);

    private final SaltConnector sc;

    private final Set<String> originalTargets;

    private final String path;

    private final String fileName;

    private final byte[] content;

    private Set<String> targets;

    public SaltUpload(SaltConnector sc, Set<String> targets, String path, String fileName, byte[] content) {
        this.sc = sc;
        originalTargets = targets;
        this.targets = targets;
        this.path = path;
        this.fileName = fileName;
        this.content = content;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Uploading files to: {}", targets);
        if (!targets.isEmpty()) {
            LOGGER.info("Current targets for upload: {}", targets);

            GenericResponses responses = sc.upload(targets, path, fileName, content);

            Set<String> failedTargets = new HashSet<>();

            LOGGER.info("Salt file upload responses: {}", responses);
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

        LOGGER.info("File upload has been completed on nodes: {}", originalTargets);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SaltUpload{");
        sb.append("sc=").append(sc);
        sb.append(", originalTargets=").append(originalTargets);
        sb.append(", path='").append(path).append('\'');
        sb.append(", fileName='").append(fileName).append('\'');
        sb.append(", targets=").append(targets);
        sb.append('}');
        return sb.toString();
    }
}
