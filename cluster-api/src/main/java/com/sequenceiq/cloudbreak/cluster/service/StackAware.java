package com.sequenceiq.cloudbreak.cluster.service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;

public interface StackAware {

    Stack getStack();
}
