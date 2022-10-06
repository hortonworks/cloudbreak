package com.sequenceiq.it.cloudbreak.action;

import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;

@FunctionalInterface
public interface RetryableAction<T extends CloudbreakTestDto, U extends MicroserviceClient> extends Action<T, U> {
}
