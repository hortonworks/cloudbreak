package com.sequenceiq.periscope.monitor.context;

import com.sequenceiq.periscope.model.ScalingActivities;

public class ScalingActivitiesEvaluatorContext implements EvaluatorContext {

    private final ScalingActivities scalingActivities;

    public ScalingActivitiesEvaluatorContext(ScalingActivities scalingActivities) {
        this.scalingActivities = scalingActivities;
    }

    @Override
    public Object getData() {
        return scalingActivities;
    }

    @Override
    public long getItemId() {
        return scalingActivities.getId();
    }
}
