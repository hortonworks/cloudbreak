package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import java.util.List;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;

public interface FingerprintCollector {

    FingerprintsResponse collectFingerprintFromMinions(SaltConnector sc, List<Minion> minionsToAccept) throws CloudbreakOrchestratorFailedException;

}
