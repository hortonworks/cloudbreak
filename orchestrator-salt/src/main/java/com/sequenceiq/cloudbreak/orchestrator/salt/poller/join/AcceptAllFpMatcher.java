package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcceptAllFpMatcher implements MinionFingerprintMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptAllFpMatcher.class);

    @Override
    public List<String> collectMinionsWithMatchingFp(Map<String, String> fingerprintsFromMaster, Map<String, String> fingerprintByMinion) {
        LOGGER.info("Accept all keys: {}", fingerprintsFromMaster.keySet());
        return List.copyOf(fingerprintsFromMaster.keySet());
    }
}
