package com.sequenceiq.cloudbreak.cloud;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public interface ProvisionSetup {

    Map<String, Object> setup(AuthenticatedContext authenticatedContext, CloudStack stack) throws Exception;

    String preCheck(AuthenticatedContext authenticatedContext, CloudStack stack);
}
