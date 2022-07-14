package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public interface ResponseProvider {

    StackV4Response providerEntriesToStackResponse(StackDtoDelegate stack, StackV4Response stackResponse);

    String type();
}
