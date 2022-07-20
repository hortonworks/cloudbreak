package com.sequenceiq.cloudbreak.cluster.service;

import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public interface StackAware {

    StackDtoDelegate getStack();
}
