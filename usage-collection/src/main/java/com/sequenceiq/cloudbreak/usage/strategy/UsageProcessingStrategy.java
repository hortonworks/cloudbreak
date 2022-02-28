package com.sequenceiq.cloudbreak.usage.strategy;

import com.cloudera.thunderhead.service.common.usage.UsageProto;

public interface UsageProcessingStrategy {

    /**
     * Process usage event by using an implemented strategy class with this method.
     * @param event full usage event object
     */
    void processUsage(UsageProto.Event event);
}
