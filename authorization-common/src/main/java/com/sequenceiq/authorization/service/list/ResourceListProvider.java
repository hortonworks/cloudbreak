package com.sequenceiq.authorization.service.list;

import java.util.List;

public interface ResourceListProvider {
    List<Resource> findResources(String accountId, List<String> resourceCrns);
}
