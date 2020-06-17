package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Fingerprint;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintRequest;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;

public class FingerprintCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(FingerprintCollector.class);

    public FingerprintsResponse collectFingerprintFromMinions(SaltConnector sc, List<Minion> minionsToAccept) throws CloudbreakOrchestratorFailedException {
        FingerprintsResponse fingerprintsResponse;
        try {
            fingerprintsResponse = sc.collectFingerPrints(new FingerprintRequest(minionsToAccept));
        } catch (Exception e) {
            LOGGER.error("Couldn't collect fingerprints for minions: {}", minionsToAccept, e);
            throw new CloudbreakOrchestratorFailedException("Couldn't collect fingerprints for minions", e);
        }
        validateFingerprintResponse(minionsToAccept, fingerprintsResponse);
        return fingerprintsResponse;
    }

    private void validateFingerprintResponse(List<Minion> minionsToAccept, FingerprintsResponse response) throws CloudbreakOrchestratorFailedException {
        validateHttpStatus(response, minionsToAccept);
        validateAllMinionsCollected(minionsToAccept, response);
    }

    private void validateAllMinionsCollected(List<Minion> minionsToAccept, FingerprintsResponse response) throws CloudbreakOrchestratorFailedException {
        List<String> collectedMinionAddress = response.getFingerprints().stream().map(Fingerprint::getAddress).collect(Collectors.toList());
        boolean allFingerprintCollected = minionsToAccept.stream().allMatch(minion -> collectedMinionAddress.contains(minion.getAddress()));
        if (!allFingerprintCollected) {
            LOGGER.error("Not all minions' fingerprint has been collected. Minions to collect: [{}] Response: [{}]", minionsToAccept, response);
            throw new CloudbreakOrchestratorFailedException("Not all minions' fingerprint has been collected.");
        }
    }

    private void validateHttpStatus(FingerprintsResponse response, List<Minion> minionsToAccept) throws CloudbreakOrchestratorFailedException {
        if (HttpStatus.OK.value() != response.getStatusCode()) {
            LOGGER.error("There was an error during collection of fingerprints from minions. Response: [{}] Minions: [{}]", response, minionsToAccept);
            throw new CloudbreakOrchestratorFailedException(
                    String.format("Couldn't collect fingerprints for minions. Statuscode: [%s] Status reason: [%s]",
                            response.getStatusCode(), response.getErrorText()));
        }
    }
}
