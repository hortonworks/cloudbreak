package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.domain.stack.StackStatus;

public interface StackTtlView {

    Long getId();

    String getName();

    String getOwner();

    String getAccount();

    StackStatus getStatus();

    Long getCreationFinished();
}
