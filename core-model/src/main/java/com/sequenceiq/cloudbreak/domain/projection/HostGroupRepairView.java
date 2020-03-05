package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;

public interface HostGroupRepairView {

    String getName();

    RecoveryMode getRecoveryMode();
}
