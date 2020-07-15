package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EqualMinionFpMatcher implements MinionFingerprintMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EqualMinionFpMatcher.class);

    public List<String> collectMinionsWithMatchingFp(Map<String, String> fingerprintsFromMaster, Map<String, String> fingerprintByMinion) {
        LOGGER.info("Matching fingerprints from master: {} and from minions: {}", fingerprintsFromMaster, fingerprintByMinion);
        return fingerprintsFromMaster.entrySet().stream()
                .filter(entry -> isFingerPrintMatches(fingerprintByMinion, entry))
                .map(Entry::getKey)
                .collect(Collectors.toList());
    }

    private boolean isFingerPrintMatches(Map<String, String> fingerprintByMinion, Map.Entry<String, String> fingerPrintFromMasterByMinion) {
        String minionId = fingerPrintFromMasterByMinion.getKey();
        String fingerprintOnMinion = fingerprintByMinion.get(minionId);
        LOGGER.debug("Minion ID: [{}] Fingerprint on master: [{}] Fingerprint on minion: [{}]",
                minionId, fingerPrintFromMasterByMinion.getValue(), fingerprintOnMinion);
        return fingerPrintFromMasterByMinion.getValue().equals(fingerprintOnMinion);
    }
}
