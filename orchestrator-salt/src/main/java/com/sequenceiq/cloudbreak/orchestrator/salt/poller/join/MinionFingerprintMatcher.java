package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import java.util.List;
import java.util.Map;

public interface MinionFingerprintMatcher {
    List<String> collectMinionsWithMatchingFp(Map<String, String> fingerprintsFromMaster, Map<String, String> fingerprintByMinion);
}
