package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface ResponseProvider {

    StackResponse providerEntriesToStackResponse(Stack stack, StackResponse stackResponse);

    String type();
}
