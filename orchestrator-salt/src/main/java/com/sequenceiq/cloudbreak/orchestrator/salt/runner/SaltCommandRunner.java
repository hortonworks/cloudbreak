package com.sequenceiq.cloudbreak.orchestrator.salt.runner;

import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltCommandTracker;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@Component
public class SaltCommandRunner {

    @Inject
    private SaltRunner saltRunner;

    public void runSaltCommand(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel, ExitCriteria exitCriteria)
            throws Exception {
        OrchestratorBootstrap saltCommandTracker = new SaltCommandTracker(sc, baseSaltJobRunner);
        Callable<Boolean> saltCommandRunBootstrapRunner = saltRunner.runner(saltCommandTracker, exitCriteria, exitCriteriaModel);
        saltCommandRunBootstrapRunner.call();
    }

    public void runSaltCommand(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel, int retry,
            ExitCriteria exitCriteria) throws Exception {
        OrchestratorBootstrap saltCommandTracker = new SaltCommandTracker(sc, baseSaltJobRunner);
        Callable<Boolean> saltCommandRunBootstrapRunner = saltRunner.runner(saltCommandTracker, exitCriteria, exitCriteriaModel, retry, false);
        saltCommandRunBootstrapRunner.call();
    }
}
