package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.Polling;
import com.dyngr.exception.PollerException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Fingerprint;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionFingersOnMasterResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionKeysOnMasterResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

public class MinionAcceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinionAcceptor.class);

    private static final Long MINION_DELETION_POLLING_INTERVAL_IN_SECONDS = 5L;

    private static final long MINION_DELETION_POLLING_TIMEOUT_IN_MINUTES = 2L;

    private final Collection<SaltConnector> saltConnectors;

    private final List<Minion> requiredMinions;

    private final List<Minion> knownMinions;

    private final MinionFingerprintMatcher fingerprintMatcher;

    private final FingerprintCollector fingerprintCollector;

    private final SaltStateService saltStateService;

    /**
     * @param requiredMinions the minions that must be present on the master (the bootstrap targets); used only for the must-be-present check.
     * @param knownMinions all minions known to the cluster; used for rogue-detection and acceptance. MUST be a superset of {@code requiredMinions},
     *                      otherwise a legitimate target minion would be treated as unexpected and its key deleted.
     */
    public MinionAcceptor(Collection<SaltConnector> saltConnectors, List<Minion> requiredMinions, List<Minion> knownMinions,
            MinionFingerprintMatcher fingerprintMatcher, FingerprintCollector fingerprintCollector, SaltStateService saltStateService) {
        this.saltConnectors = saltConnectors;
        this.requiredMinions = requiredMinions;
        this.knownMinions = knownMinions;
        this.fingerprintMatcher = fingerprintMatcher;
        this.fingerprintCollector = fingerprintCollector;
        this.saltStateService = saltStateService;
    }

    public void acceptMinions() throws CloudbreakOrchestratorFailedException {
        boolean removedConflictingMinion = false;
        for (SaltConnector sc : saltConnectors) {
            LOGGER.info("Running for master: [{}]", sc.getHostname());
            MinionKeysOnMasterResponse minionKeysOnMaster = fetchMinionsFromMaster(sc, requiredMinions);
            List<String> unacceptedMinions = new ArrayList<>(minionKeysOnMaster.getUnacceptedMinions());
            List<String> deniedMinions = minionKeysOnMaster.getDeniedMinions();
            List<String> conflictingMinions = removeMinionIdsInBothDeniedAndUnacceptedState(sc, deniedMinions, unacceptedMinions);
            List<String> deniedOnlyMinions = removeMinionIdsOnlyInDeniedState(sc, deniedMinions, unacceptedMinions);
            List<String> unexpectedMinions = removeMinionIdsThatAreNotExpected(sc, unacceptedMinions);
            removedConflictingMinion = removedConflictingMinion || !conflictingMinions.isEmpty() || !deniedOnlyMinions.isEmpty();
            unacceptedMinions = unacceptedMinions.stream()
                    .filter(not(conflictingMinions::contains))
                    .filter(not(unexpectedMinions::contains))
                    .collect(Collectors.toList());
            if (!unacceptedMinions.isEmpty()) {
                proceedWithAcceptingMinions(sc, unacceptedMinions);
            } else {
                LOGGER.info("No unaccepted minions found on master: [{}]", sc.getHostname());
            }
        }
        if (removedConflictingMinion) {
            throw new CloudbreakOrchestratorFailedException("Minion(s) were removed, restart bootstrap to ensure all minion present");
        }
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
            deleteSaltKey(sc, minionIdsInBothDeniedAndUnacceptedState);
        }
        return minionIdsInBothDeniedAndUnacceptedState;
    }

    private void deleteSaltKey(SaltConnector sc, List<String> minionIdsToDelete) throws CloudbreakOrchestratorFailedException {
        sc.wheel("key.delete", minionIdsToDelete, Object.class);
        pollUntilMinionDisappearsFromIpAddrResponse(sc, minionIdsToDelete);
    }

    private void pollUntilMinionDisappearsFromIpAddrResponse(SaltConnector sc, List<String> minionIdsToDelete) throws CloudbreakOrchestratorFailedException {
        MinionDeletionPoller minionDeletionPoller = new MinionDeletionPoller(sc, new HashSet<>(minionIdsToDelete), saltStateService);
        try {
            Polling.waitPeriodly(MINION_DELETION_POLLING_INTERVAL_IN_SECONDS, TimeUnit.SECONDS)
                    .stopAfterDelay(getMinionDeletionPollingTimeoutInMinutes(), TimeUnit.MINUTES)
                    .stopIfException(false)
                    .run(minionDeletionPoller);
        } catch (PollerException e) {
            Set<String> remainingMinions = minionDeletionPoller.getRemainingReachableMinions();
            LOGGER.error("Failed while polling deleted minion keys on master [{}], minions: {}, remaining: {}",
                    sc.getHostname(), minionIdsToDelete, remainingMinions, e);
            throw new CloudbreakOrchestratorFailedException("Failed while polling deleted minion keys", e);
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected failure while polling deleted minion keys on master [{}], minions: {}", sc.getHostname(), minionIdsToDelete, e);
            throw new CloudbreakOrchestratorFailedException("Failed while polling deleted minion keys", e);
        }
    }

    long getMinionDeletionPollingTimeoutInMinutes() {
        return MINION_DELETION_POLLING_TIMEOUT_IN_MINUTES;
    }

    private List<String> removeMinionIdsOnlyInDeniedState(SaltConnector sc, List<String> deniedMinions, List<String> unacceptedMinions)
            throws CloudbreakOrchestratorFailedException {
        List<String> minionIdsOnlyDenied = deniedMinions.stream().distinct()
                .filter(not(unacceptedMinions::contains))
                .collect(Collectors.toList());
        if (!minionIdsOnlyDenied.isEmpty()) {
            LOGGER.info("There are minions in denied state, removing: {}", minionIdsOnlyDenied);
            deleteSaltKey(sc, minionIdsOnlyDenied);
        }
        return minionIdsOnlyDenied;
    }

    private List<String> removeMinionIdsThatAreNotExpected(SaltConnector sc, List<String> unacceptedMinions)
            throws CloudbreakOrchestratorFailedException {
        List<String> expectedMinionIds = knownMinions.stream().map(Minion::getId).collect(Collectors.toList());
        List<String> unexpectedMinionIds = unacceptedMinions.stream()
                .filter(not(expectedMinionIds::contains))
                .collect(Collectors.toList());
        if (!unexpectedMinionIds.isEmpty()) {
            LOGGER.info("There are minions that are not expected, removing: {}", unexpectedMinionIds);
            deleteSaltKey(sc, unexpectedMinionIds);
        }
        return unexpectedMinionIds;
    }

    private void proceedWithAcceptingMinions(SaltConnector sc, List<String> unacceptedMinions) throws CloudbreakOrchestratorFailedException {
        LOGGER.info("There are unaccepted minions on master: {}", unacceptedMinions);
        Map<String, String> fingerprintsFromMaster = fetchFingerprintsFromMaster(sc, unacceptedMinions);
        List<Minion> minionsToAccept = knownMinions.stream().filter(minion -> unacceptedMinions.contains(minion.getId())).collect(Collectors.toList());
        LOGGER.info("Processing the following minions so they are accepted on the master: {}",
                minionsToAccept.stream().map(Minion::getId).collect(Collectors.toList()));
        if (!minionsToAccept.isEmpty()) {
            FingerprintsResponse fingerprintsResponse = fingerprintCollector.collectFingerprintFromMinions(sc, minionsToAccept);
            acceptMatchingFingerprints(sc, fingerprintsFromMaster, fingerprintsResponse, minionsToAccept);
        }
    }

    private void acceptMatchingFingerprints(SaltConnector sc, Map<String, String> fingerprintsFromMaster, FingerprintsResponse fingerprintsResponse,
            List<Minion> minions) throws CloudbreakOrchestratorFailedException {
        Map<String, String> fingerprintByMinion = mapFingerprintByMinion(fingerprintsResponse, minions);
        List<String> minionsToAccept = fingerprintMatcher.collectMinionsWithMatchingFp(fingerprintsFromMaster, fingerprintByMinion);
        LOGGER.info("Following minions will be accepted: {}", minionsToAccept);
        if (!minionsToAccept.isEmpty()) {
            sc.wheel("key.accept", minionsToAccept, Object.class);
        }
        List<Minion> minionsNotAccepted = minions.stream().filter(minion -> !minionsToAccept.contains(minion.getId())).toList();
        if (!minionsNotAccepted.isEmpty()) {
            LOGGER.warn("Not all minions can be accepted, as their fingerprint is different: {}", minionsNotAccepted);
            throw new CloudbreakOrchestratorFailedException("Not all minions can be accepted, as their fingerprint is different: " + minionsNotAccepted);
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
            throw new CloudbreakOrchestratorFailedException("There are missing minions from salt response");
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
            throw new CloudbreakOrchestratorFailedException("Error during fetching fingerprints from master for minions", e);
        }
        validateMasterFingerprintResponse(minions, unacceptedMinions, sc);
        return unacceptedMinions;
    }

    private void validateMasterFingerprintResponse(List<String> minions, Map<String, String> unacceptedMinions, SaltConnector sc)
            throws CloudbreakOrchestratorFailedException {
        if (!unacceptedMinions.keySet().containsAll(minions)) {
            LOGGER.error("Fingerprints from master {} doesn't contain all requested minions {}", unacceptedMinions.keySet(), minions);
            throw new CloudbreakOrchestratorFailedException("Fingerprints from master doesn't contain all requested minions");
        }
    }
}
