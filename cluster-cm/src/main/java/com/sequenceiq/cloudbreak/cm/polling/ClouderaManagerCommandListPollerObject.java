package com.sequenceiq.cloudbreak.cm.polling;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.List;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ClouderaManagerCommandListPollerObject extends ClouderaManagerPollerObject {
    private final List<BigDecimal> idList;

    public ClouderaManagerCommandListPollerObject(Stack stack, ApiClient apiClient, List<BigDecimal> idList) {
        super(stack, apiClient);
        this.idList = checkNotNull(idList, "Command list poller object should be used for commands polling, thus command id list cannot be null.");
    }

    public List<BigDecimal> getIdList() {
        return idList;
    }
}
