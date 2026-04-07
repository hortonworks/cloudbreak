package com.sequenceiq.cloudbreak.cm.polling;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public class ClouderaManagerCommandPollerObject extends ClouderaManagerPollerObject {

    public static final long TIMEOUT_UNKNOWN = -1L;

    private final Long id;

    private final long timeoutInSeconds;

    public ClouderaManagerCommandPollerObject(StackDtoDelegate stack, ApiClient apiClient, Long id) {
        this(stack, apiClient, id, TIMEOUT_UNKNOWN);
    }

    public ClouderaManagerCommandPollerObject(StackDtoDelegate stack, ApiClient apiClient, Long id, long timeoutInSeconds) {
        super(stack, apiClient);
        this.id = checkNotNull(id, "Command poller object should be used for command polling, thus command id cannot be null.");
        this.timeoutInSeconds = timeoutInSeconds;
    }

    public Long getId() {
        return id;
    }

    public long getTimeoutInSeconds() {
        return timeoutInSeconds;
    }
}
