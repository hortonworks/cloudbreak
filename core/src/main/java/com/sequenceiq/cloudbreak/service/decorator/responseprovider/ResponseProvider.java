package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackProxy;

public interface ResponseProvider {

    StackV4Response providerEntriesToStackResponse(StackProxy stack, StackV4Response stackResponse);

    String type();
}
