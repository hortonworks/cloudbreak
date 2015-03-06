package com.sequenceiq.cloudbreak.core.flow.service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

public interface StackManagementFacade {
    FlowContext stackCreationError(FlowContext context) throws CloudbreakException;
}
