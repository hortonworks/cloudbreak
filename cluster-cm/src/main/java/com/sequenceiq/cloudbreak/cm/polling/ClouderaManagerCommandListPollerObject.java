package com.sequenceiq.cloudbreak.cm.polling;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public class ClouderaManagerCommandListPollerObject extends ClouderaManagerPollerObject {
    private final List<Long> idList;

    public ClouderaManagerCommandListPollerObject(StackDtoDelegate stack, ApiClient apiClient, List<Long> idList) {
        super(stack, apiClient);
        this.idList = checkNotNull(idList, "Command list poller object should be used for commands polling, thus command id list cannot be null.");
    }

    public List<Long> getIdList() {
        return idList;
    }
}
