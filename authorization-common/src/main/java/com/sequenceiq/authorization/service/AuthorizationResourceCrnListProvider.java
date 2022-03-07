package com.sequenceiq.authorization.service;

import java.util.List;

public interface AuthorizationResourceCrnListProvider extends ResourcePropertyProvider {

    List<String> getResourceCrnListByResourceNameList(List<String> resourceNames);
}
