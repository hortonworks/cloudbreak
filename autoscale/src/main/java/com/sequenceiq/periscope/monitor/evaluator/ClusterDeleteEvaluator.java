package com.sequenceiq.periscope.monitor.evaluator;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.event.ClusterDeleteEvent;

@Component("ClusterDeleteEvaluator")
@Scope("prototype")
public class ClusterDeleteEvaluator extends EvaluatorExecutor {

    private static final String EVALUATOR_NAME = ClusterDeleteEvaluator.class.getName();

    private long clusterId;

    @Inject
    private EventPublisher eventPublisher;

    @Nonnull
    @Override
    public EvaluatorContext getContext() {
        return new ClusterIdEvaluatorContext(clusterId);
    }

    @Override
    public String getName() {
        return EVALUATOR_NAME;
    }

    @Override
    public void setContext(EvaluatorContext context) {
        this.clusterId = (long) context.getData();
    }

    @Override
    protected void execute() {
        eventPublisher.publishEvent(new ClusterDeleteEvent(clusterId));
    }
}
