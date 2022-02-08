package com.sequenceiq.cloudbreak.usage.strategy;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.usage.model.UsageContext;

public interface UsageProcessingStrategy {

    /**
     * Process usage event by using an implemented strategy class with this method.
     * @param event full usage event object
     * @param context object that holds common usage related data
     */
    void processUsage(UsageProto.Event event, UsageContext context);
}
