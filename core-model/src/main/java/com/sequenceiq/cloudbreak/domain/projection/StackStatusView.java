package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.domain.stack.StackStatus;

public interface StackStatusView {

    Long getId();

    String getName();

    StackStatus getStatus();
}
