package com.sequenceiq.periscope.monitor.evaluator;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;

public abstract class EvaluatorExecutor implements Runnable {
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
}
