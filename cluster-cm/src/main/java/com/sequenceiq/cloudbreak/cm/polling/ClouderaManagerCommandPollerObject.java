package com.sequenceiq.cloudbreak.cm.polling;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public class ClouderaManagerCommandPollerObject extends ClouderaManagerPollerObject {

    private final BigDecimal id;

    public ClouderaManagerCommandPollerObject(StackDtoDelegate stack, ApiClient apiClient, BigDecimal id) {
        super(stack, apiClient);
        this.id = checkNotNull(id, "Command poller object should be used for command polling, thus command id cannot be null.");
    }

    public BigDecimal getId() {
        return id;
    }
}
