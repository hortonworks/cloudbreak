package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static java.util.Collections.singletonMap;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;

public class PillarSave implements OrchestratorBootstrap {

    private final SaltConnector sc;
    private final Pillar pillar;

    public PillarSave(SaltConnector sc, String gateway) {
        this.sc = sc;
        this.pillar = new Pillar("/ambari/server.sls", singletonMap("ambari", singletonMap("server", gateway)));
    }

    public PillarSave(SaltConnector sc, SaltPillarProperties pillarProperties) {
        this.sc = sc;
        this.pillar = new Pillar(pillarProperties.getPath(), pillarProperties.getProperties());
    }

    @Override
    public Boolean call() throws Exception {
        GenericResponse resp = sc.pillar(pillar);
        resp.assertError();
        return true;
    }
}
