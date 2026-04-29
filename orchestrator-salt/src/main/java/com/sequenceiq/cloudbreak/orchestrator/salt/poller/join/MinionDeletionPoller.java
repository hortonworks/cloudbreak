package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

public class MinionDeletionPoller implements AttemptMaker<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinionDeletionPoller.class);

    private final SaltConnector saltConnector;

    private final Set<String> minionIdsToDelete;

    private final SaltStateService saltStateService;

    private final Set<String> remainingReachableMinions = new HashSet<>();

    private int attempt;

    public MinionDeletionPoller(SaltConnector saltConnector, Set<String> minionIdsToDelete, SaltStateService saltStateService) {
        this.saltConnector = saltConnector;
        this.minionIdsToDelete = minionIdsToDelete;
        this.saltStateService = saltStateService;
    }

    @Override
    public AttemptResult<Void> process() {
        attempt++;
        LOGGER.debug("Checking deleted minion keys on master [{}]. Attempt: [{}], minions: {}", saltConnector.getHostname(), attempt, minionIdsToDelete);
        try {
            MinionIpAddressesResponse minionIpAddressesResponse = saltStateService.collectMinionIpAddresses(saltConnector, Optional.of(minionIdsToDelete));
            if (minionIpAddressesResponse == null || minionIpAddressesResponse.getResult() == null) {
                return AttemptResults.continueFor(new IllegalStateException("Collecting minion IP addresses returned empty response"));
            } else {
                remainingReachableMinions.clear();
                remainingReachableMinions.addAll(collectRemainingReachableMinions(minionIpAddressesResponse));
                if (remainingReachableMinions.isEmpty()) {
                    LOGGER.info("Deleted minion keys are no longer reachable on master [{}]: {}", saltConnector.getHostname(), minionIdsToDelete);
                    return AttemptResults.justFinish();
                } else {
                    LOGGER.debug("Deleted minion keys are still reachable on master [{}] at attempt [{}]: {}", saltConnector.getHostname(), attempt,
                            remainingReachableMinions);
                    return AttemptResults.continueFor(new IllegalStateException("Deleted minion keys are still reachable: "
                                        + remainingReachableMinions));
                }
            }
        } catch (RuntimeException e) {
            LOGGER.debug("Unable to verify minion key deletion for minions: {}", minionIdsToDelete, e);
            return AttemptResults.continueFor(e);
        }
    }

    public Set<String> getRemainingReachableMinions() {
        return Set.copyOf(remainingReachableMinions);
    }

    private Set<String> collectRemainingReachableMinions(MinionIpAddressesResponse minionIpAddressesResponse) {
        Set<String> presentMinions = minionIpAddressesResponse.getReachableNodes();
        Set<String> unreachableMinions = new HashSet<>(minionIpAddressesResponse.getUnreachableNodes());
        return minionIdsToDelete.stream()
                .filter(minionId -> presentMinions.contains(minionId) && !unreachableMinions.contains(minionId))
                .collect(Collectors.toSet());
    }
}




