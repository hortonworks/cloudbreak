package com.sequenceiq.cloudbreak.cm;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;

@FunctionalInterface
public interface ClouderaManagerOperationPollerCommand {
    ExtendedPollingResult apply(StackDtoDelegate stack, ApiClient apiClient, Long commandId);
}
