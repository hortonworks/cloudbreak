package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Fingerprint;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionFingersOnMasterResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionKeysOnMasterResponse;

public class MinionAcceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinionAcceptor.class);

    private final Collection<SaltConnector> saltConnectors;

    private final List<Minion> minions;

    private final MinionFingerprintMatcher fingerprintMatcher;

    private final FingerprintCollector fingerprintCollector;

    public MinionAcceptor(Collection<SaltConnector> saltConnectors, List<Minion> minions, MinionFingerprintMatcher fingerprintMatcher,
            FingerprintCollector fingerprintCollector) {
        this.saltConnectors = saltConnectors;
        this.minions = minions;
        this.fingerprintMatcher = fingerprintMatcher;
        this.fingerprintCollector = fingerprintCollector;
    }

    public void acceptMinions() throws CloudbreakOrchestratorFailedException {
        for (SaltConnector sc : saltConnectors) {
            LOGGER.info("Running for master: [{}]", sc.getHostname());
            List<String> unacceptedMinions = fetchUnacceptedMinionsFromMaster(sc, minions);
            if (!unacceptedMinions.isEmpty()) {
                proceedWithAcceptingMinions(sc, unacceptedMinions);
            } else {
                LOGGER.info("No unaccepted minions found on master");
            }
        }
    }

    private void proceedWithAcceptingMinions(SaltConnector sc, List<String> unacceptedMinions) throws CloudbreakOrchestratorFailedException {
        LOGGER.info("There are unaccepted minions on master: {}", unacceptedMinions);
        Map<String, String> fingerprintsFromMaster = fetchFingerprintsFromMaster(sc, unacceptedMinions);
        List<Minion> minionsToAccept = minions.stream().filter(minion -> unacceptedMinions.contains(minion.getId())).collect(Collectors.toList());
        FingerprintsResponse fingerprintsResponse = fingerprintCollector.collectFingerprintFromMinions(sc, minionsToAccept);
        acceptMatchingFingerprints(sc, fingerprintsFromMaster, fingerprintsResponse, minionsToAccept);
    }

    private void acceptMatchingFingerprints(SaltConnector sc, Map<String, String> fingerprintsFromMaster, FingerprintsResponse fingerprintsResponse,
            List<Minion> minions) {
        Map<String, String> fingerprintByMinion = mapFingerprintByMinion(fingerprintsResponse, minions);
        List<String> minionsToAccept = fingerprintMatcher.collectMinionsWithMatchingFp(fingerprintsFromMaster, fingerprintByMinion);
        LOGGER.info("Following minions will be accepted: {}", minionsToAccept);
        sc.wheel("key.accept", minionsToAccept, Object.class);
    }

    private Map<String, String> mapFingerprintByMinion(FingerprintsResponse fingerprintsResponse, List<Minion> minions) {
        Map<String, String> minionIdByAddress = minions.stream().collect(Collectors.toMap(Minion::getAddress, Minion::getId));
        List<Fingerprint> fingerprints = fingerprintsResponse.getFingerprints();
        return fingerprints.stream()
                .collect(Collectors.toMap(fp -> minionIdByAddress.get(fp.getAddress()), Fingerprint::getFingerprint));
    }

    private List<String> fetchUnacceptedMinionsFromMaster(SaltConnector sc, List<Minion> minions) throws CloudbreakOrchestratorFailedException {
        Set<String> minionId = minions.stream().map(Minion::getId).collect(Collectors.toSet());
        LOGGER.debug("Minions should join: {}", minionId);
        MinionKeysOnMasterResponse response = sc.wheel("key.list_all", null, MinionKeysOnMasterResponse.class);
        LOGGER.debug("Minion keys on master response: {}", response);
        if (!response.getAllMinions().containsAll(minionId)) {
            throw new CloudbreakOrchestratorFailedException("There are missing minions from salt response");
        }
        return response.getUnacceptedMinions();
    }

    private Map<String, String> fetchFingerprintsFromMaster(SaltConnector sc, List<String> minions) throws CloudbreakOrchestratorFailedException {
        Map<String, String> unacceptedMinions;
        try {
            MinionFingersOnMasterResponse response = sc.wheel("key.finger", minions, MinionFingersOnMasterResponse.class);
            LOGGER.debug("MinionFingersOnMasterResponse: {}", response);
            unacceptedMinions = response.getUnacceptedMinions();
        } catch (Exception e) {
            LOGGER.error("Error during fetching fingerprints from master for minions: {}", minions, e);
            throw new CloudbreakOrchestratorFailedException("Error during fetching fingerprints from master for minions", e);
        }
        validateMasterFingerprintResponse(minions, unacceptedMinions);
        return unacceptedMinions;
    }

    private void validateMasterFingerprintResponse(List<String> minions, Map<String, String> unacceptedMinions) throws CloudbreakOrchestratorFailedException {
        if (!unacceptedMinions.keySet().containsAll(minions)) {
            LOGGER.error("Fingerprints from master {} doesn't contain all requested minions {}", unacceptedMinions.keySet(), minions);
            throw new CloudbreakOrchestratorFailedException("Fingerprints from master doesn't contain all requested minions");
        }
    }
}
