package com.sequenceiq.periscope.monitor.evaluator;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.utils.ClusterUtils;
import com.sequenceiq.periscope.utils.TimeUtil;

public abstract class EvaluatorExecutor implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluatorExecutor.class);

    @Inject
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Nonnull
    public abstract EvaluatorContext getContext();

    public abstract String getName();

    public abstract void setContext(EvaluatorContext context);

    @Override
    public final void run() {
        long itemId = getContext().getItemId();
        try {
            execute();
        } finally {
            executorServiceWithRegistry.finished(this, itemId);
        }
    }

    protected abstract void execute();

    protected boolean isCoolDownTimeElapsed(String clusterCrn, String coolDownAction, long expectedCoolDownMillis, long lastClusterScalingActivity) {
        long remainingTime = ClusterUtils.getRemainingCooldownTime(
                expectedCoolDownMillis, lastClusterScalingActivity);

        if (remainingTime <= 0) {
            return true;
        } else {
            LOGGER.debug("Cluster {} cannot be {} for {} min(s)", clusterCrn, coolDownAction,
                    ClusterUtils.TIME_FORMAT.format((double) remainingTime / TimeUtil.MIN_IN_MS));
        }
        return false;
    }
}
