package com.sequenceiq.cloudbreak.service.stack;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.flow.ResourceRequestResult;

public interface FailureHandlerService {

    void handleFailure(Stack stack, List<ResourceRequestResult> resourceRequestResults);
}
