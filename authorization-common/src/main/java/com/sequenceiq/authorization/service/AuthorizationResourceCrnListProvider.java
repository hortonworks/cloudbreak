package com.sequenceiq.authorization.service;

import java.util.List;

/**
 * Authorization framework interface for getting resource CRN list by resource name list to allow
 * authz framework to execute permission check based on name list
 */
public interface AuthorizationResourceCrnListProvider extends ResourcePropertyProvider {

    List<String> getResourceCrnListByResourceNameList(List<String> resourceNames);
}
