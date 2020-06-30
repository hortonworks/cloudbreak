package com.sequenceiq.cloudbreak.orchestrator.salt.runner;

import java.util.concurrent.Callable;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@Component
public class SaltRunner {

    private static final int SLEEP_TIME = 10000;
    // TODO Question: Customize this for different operations, or use an expectedDuration parameter to auto-tune.
    //  Maybe expose this parameter in each function here, so that new API invocations are forced to think about
    //  what the value here should be.

    // By setting this to a lower value for specific calls (bootStrap, mount-disks, etc) - can end up saving 10-15 seconds
    //  in node startup time.

    // The downside is a potentially less scalable CB instance; horizontal scaling should take care of this if a faster poll
    //  becomes a bottleneck.

    @Value("${cb.max.salt.new.service.retry.onerror}")
    private int maxRetryOnError;

    @Value("${cb.max.salt.new.service.retry}")
    private int maxRetry;

    public Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel, int maxRetry,
            boolean usingErrorCount) {
        return new OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap(), maxRetry, SLEEP_TIME,
                usingErrorCount ? maxRetryOnError : maxRetry);
    }

    public Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel) {
        return runner(bootstrap, exitCriteria, exitCriteriaModel, maxRetry, false);
    }
}
