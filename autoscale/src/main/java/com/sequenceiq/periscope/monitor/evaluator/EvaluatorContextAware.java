package com.sequenceiq.periscope.monitor.evaluator;

import com.sequenceiq.periscope.monitor.context.EvaluatorContext;

public interface EvaluatorContextAware {

    void setContext(EvaluatorContext context);
}
