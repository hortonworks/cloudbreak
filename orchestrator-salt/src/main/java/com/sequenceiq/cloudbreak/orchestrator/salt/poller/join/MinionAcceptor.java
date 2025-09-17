package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMultimap;
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
            MinionKeysOnMasterResponse minionKeysOnMaster = fetchMinionsFromMaster(sc, minions);
            List<String> unacceptedMinions = minionKeysOnMaster.getUnacceptedMinions();
            List<String> deniedMinions = minionKeysOnMaster.getDeniedMinions();
            List<String> removedUnacceptedMinions = cleanupMinionIds(sc, deniedMinions, unacceptedMinions);
            unacceptedMinions = unacceptedMinions.stream().filter(not(removedUnacceptedMinions::contains)).collect(Collectors.toList());
            if (!unacceptedMinions.isEmpty()) {
                proceedWithAcceptingMinions(sc, unacceptedMinions);
            } else {
                LOGGER.info("No unaccepted minions found on master: [{}]", sc.getHostname());
            }
        }
    }

    private List<String> cleanupMinionIds(SaltConnector sc, List<String> deniedMinions, List<String> unacceptedMinions)
            throws CloudbreakOrchestratorFailedException {
        ArrayList<String> removedUnacceptedMinions = new ArrayList<String>();
        removedUnacceptedMinions.addAll(removeMinionIdsInBothDeniedAndUnacceptedState(sc, deniedMinions, unacceptedMinions));
        removeMinionIdsOnlyInDeniedState(sc, deniedMinions, unacceptedMinions);
        removedUnacceptedMinions.addAll(removeMinionIdsThatAreNotExpected(sc, unacceptedMinions));
        return removedUnacceptedMinions;
    }

    private List<String> removeMinionIdsInBothDeniedAndUnacceptedState(SaltConnector sc, List<String> deniedMinions, List<String> unacceptedMinions)
            throws CloudbreakOrchestratorFailedException {
        LOGGER.info("Unaccepted minions: {}", unacceptedMinions);
        LOGGER.info("Denied minions: {}", deniedMinions);
        List<String> minionIdsInBothDeniedAndUnacceptedState = deniedMinions.stream().distinct()
                .filter(unacceptedMinions::contains).collect(Collectors.toList());
        if (!minionIdsInBothDeniedAndUnacceptedState.isEmpty()) {
            LOGGER.info("There are minions in denied and unaccepted state at the same time, lets remove them: " +
                    minionIdsInBothDeniedAndUnacceptedState);
            sc.wheel("key.delete", minionIdsInBothDeniedAndUnacceptedState, Object.class);
            throw new CloudbreakOrchestratorFailedException("There were minions in denied and unaccepted state at the same time: " +
                    minionIdsInBothDeniedAndUnacceptedState,
                    ImmutableMultimap.of(sc.getHostname(), "There were minions in denied and unaccepted state at the same time: " +
                            minionIdsInBothDeniedAndUnacceptedState));
        }
        return minionIdsInBothDeniedAndUnacceptedState;
    }

    private void removeMinionIdsOnlyInDeniedState(SaltConnector sc, List<String> deniedMinions, List<String> unacceptedMinions) {
        ArrayList<String> minionIdsOnlyDenied = new ArrayList<String>(deniedMinions);
        minionIdsOnlyDenied.removeAll(unacceptedMinions);
        if (!minionIdsOnlyDenied.isEmpty()) {
            LOGGER.info("There are minions in denied state, removing: {}", minionIdsOnlyDenied);
            sc.wheel("key.delete", minionIdsOnlyDenied, Object.class);
        }
    }

    private List<String> removeMinionIdsThatAreNotExpected(SaltConnector sc, List<String> unacceptedMinions) {
        List<String> expectedMinionIds = minions.stream().map(Minion::getId).collect(Collectors.toList());
        List<String> unexpectedMinionIds = unacceptedMinions.stream()
                .filter(not(expectedMinionIds::contains))
                .collect(Collectors.toList());
        if (!unexpectedMinionIds.isEmpty()) {
            LOGGER.info("There are minions that are not expected, removing: {}", unexpectedMinionIds);
            sc.wheel("key.delete", unexpectedMinionIds, Object.class);
        }
        return unexpectedMinionIds;
    }

    private void proceedWithAcceptingMinions(SaltConnector sc, List<String> unacceptedMinions) throws CloudbreakOrchestratorFailedException {
        LOGGER.info("There are unaccepted minions on master: {}", unacceptedMinions);
        Map<String, String> fingerprintsFromMaster = fetchFingerprintsFromMaster(sc, unacceptedMinions);
        List<Minion> minionsToAccept = minions.stream().filter(minion -> unacceptedMinions.contains(minion.getId())).collect(Collectors.toList());
        LOGGER.info("Processing the following minions so they are accepted on the master: {}",
                minionsToAccept.stream().map(Minion::getId).collect(Collectors.toList()));
        if (!minionsToAccept.isEmpty()) {
            FingerprintsResponse fingerprintsResponse = fingerprintCollector.collectFingerprintFromMinions(sc, minionsToAccept);
            acceptMatchingFingerprints(sc, fingerprintsFromMaster, fingerprintsResponse, minionsToAccept);
        }
    }

    private void acceptMatchingFingerprints(SaltConnector sc, Map<String, String> fingerprintsFromMaster, FingerprintsResponse fingerprintsResponse,
            List<Minion> minions) {
        Map<String, String> fingerprintByMinion = mapFingerprintByMinion(fingerprintsResponse, minions);
        List<String> minionsToAccept = fingerprintMatcher.collectMinionsWithMatchingFp(fingerprintsFromMaster, fingerprintByMinion);
        LOGGER.info("Following minions will be accepted: {}", minionsToAccept);
        if (!minionsToAccept.isEmpty()) {
            sc.wheel("key.accept", minionsToAccept, Object.class);
        }
    }

    private Map<String, String> mapFingerprintByMinion(FingerprintsResponse fingerprintsResponse, List<Minion> minions) {
        Map<String, String> minionIdByAddress = minions.stream().collect(Collectors.toMap(Minion::getAddress, Minion::getId));
        List<Fingerprint> fingerprints = fingerprintsResponse.getFingerprints();
        return fingerprints.stream()
                .filter(fp -> fp.getIpFromAddress() != null)
                .collect(Collectors.toMap(fp -> minionIdByAddress.get(fp.getIpFromAddress()), Fingerprint::getFingerprint));
    }

    private MinionKeysOnMasterResponse fetchMinionsFromMaster(SaltConnector sc, List<Minion> minions) throws CloudbreakOrchestratorFailedException {
        Set<String> minionId = minions.stream().map(Minion::getId).collect(Collectors.toSet());
        LOGGER.debug("Minions should join: {}", minionId);
        MinionKeysOnMasterResponse response = sc.wheel("key.list_all", null, MinionKeysOnMasterResponse.class);
        LOGGER.debug("Minion keys on master response: {}", response);
        if (!response.getAllMinions().containsAll(minionId)) {
            throw new CloudbreakOrchestratorFailedException("There are missing minions from salt response",
                    ImmutableMultimap.of(sc.getHostname(), "There are missing minions from salt response"));
        }
        return response;
    }

    private Map<String, String> fetchFingerprintsFromMaster(SaltConnector sc, List<String> minions) throws CloudbreakOrchestratorFailedException {
        Map<String, String> unacceptedMinions;
        try {
            MinionFingersOnMasterResponse response = sc.wheel("key.finger", minions, MinionFingersOnMasterResponse.class);
            LOGGER.debug("MinionFingersOnMasterResponse: {}", response);
            unacceptedMinions = response.getUnacceptedMinions();
        } catch (Exception e) {
            LOGGER.error("Error during fetching fingerprints from master for minions: {}", minions, e);
            throw new CloudbreakOrchestratorFailedException("Error during fetching fingerprints from master for minions", e,
                    ImmutableMultimap.of(sc.getHostname(), "Error during fetching fingerprints from master for minions"));
        }
        validateMasterFingerprintResponse(minions, unacceptedMinions, sc);
        return unacceptedMinions;
    }

    private void validateMasterFingerprintResponse(List<String> minions, Map<String, String> unacceptedMinions, SaltConnector sc)
            throws CloudbreakOrchestratorFailedException {
        if (!unacceptedMinions.keySet().containsAll(minions)) {
            LOGGER.error("Fingerprints from master {} doesn't contain all requested minions {}", unacceptedMinions.keySet(), minions);
            throw new CloudbreakOrchestratorFailedException("Fingerprints from master doesn't contain all requested minions",
                    ImmutableMultimap.of(sc.getHostname(), "Fingerprints from master doesn't contain all requested minions"));
        }
    }
}
