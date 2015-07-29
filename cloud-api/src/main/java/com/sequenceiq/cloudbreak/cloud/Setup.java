package com.sequenceiq.cloudbreak.cloud;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public interface Setup {

    // FIXME do not pass CloudStack just the objects what we need response is too generic
    Map<String, Object> execute(AuthenticatedContext authenticatedContext, CloudStack stack) throws Exception;

    // FIXME do not pass CloudStack just the objects what we need response is too generic
    String preCheck(AuthenticatedContext authenticatedContext, CloudStack stack);
}
