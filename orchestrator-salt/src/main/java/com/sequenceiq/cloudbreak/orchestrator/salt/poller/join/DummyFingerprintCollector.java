package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;

public class DummyFingerprintCollector implements FingerprintCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyFingerprintCollector.class);

    @Override
    public FingerprintsResponse collectFingerprintFromMinions(SaltConnector sc, List<Minion> minionsToAccept) {
        LOGGER.info("Return empty 'FingerprintsResponse'");
        return new FingerprintsResponse();
    }
}
