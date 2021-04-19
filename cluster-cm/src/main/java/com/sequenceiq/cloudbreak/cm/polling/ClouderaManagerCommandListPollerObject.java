package com.sequenceiq.cloudbreak.cm.polling;

import java.util.List;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ClouderaManagerCommandListPollerObject extends ClouderaManagerPollerObject {
    private final List<Integer> idList;

    public ClouderaManagerCommandListPollerObject(Stack stack, ApiClient apiClient, List<Integer> idList) {
        super(stack, apiClient);
        this.idList = idList;
    }

    public List<Integer> getIdList() {
        return idList;
    }
}
