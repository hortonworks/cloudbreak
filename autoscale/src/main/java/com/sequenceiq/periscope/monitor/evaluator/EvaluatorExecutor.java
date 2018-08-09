package com.sequenceiq.periscope.monitor.evaluator;

import javax.annotation.Nonnull;

import com.sequenceiq.periscope.monitor.context.EvaluatorContext;

public interface EvaluatorExecutor extends Runnable {

    @Nonnull
    EvaluatorContext getContext();

    String getName();

    void setContext(EvaluatorContext context);
}
