package com.sequenceiq.periscope.monitor;

import org.quartz.Job;

import com.sequenceiq.periscope.monitor.context.EvaluatorContext;

public interface Monitor<M extends Monitored> extends Job {

    String getIdentifier();

    String getTriggerExpression();

    Class<?> getEvaluatorType(M monitored);

    EvaluatorContext getContext(M monitored);
}
