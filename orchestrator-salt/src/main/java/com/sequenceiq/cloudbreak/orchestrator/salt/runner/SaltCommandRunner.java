package com.sequenceiq.cloudbreak.orchestrator.salt.runner;

import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltCommandTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ModifyGrainBase;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@Component
public class SaltCommandRunner {

    @Value("${cb.max.salt.modifygrain.maxerrorretry}")
    private int modifyGrainMaxErrorRetry;

    @Value("${cb.max.salt.modifygrain.maxretry}")
    private int modifyGrainMaxRetry;

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

    public void runModifyGrainCommand(SaltConnector sc, ModifyGrainBase modifyGrainRunner, ExitCriteriaModel exitCriteriaModel,
            ExitCriteria exitCriteria) throws Exception {
        OrchestratorBootstrap saltCommandTracker = new SaltCommandTracker(sc, modifyGrainRunner);
        Callable<Boolean> saltCommandRunBootstrapRunner = saltRunner.runner(saltCommandTracker, exitCriteria,
                exitCriteriaModel, modifyGrainMaxRetry, modifyGrainMaxErrorRetry);
        saltCommandRunBootstrapRunner.call();
    }
}
