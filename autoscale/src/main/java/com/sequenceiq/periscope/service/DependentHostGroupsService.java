package com.sequenceiq.periscope.service;

import javax.inject.Inject;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.DependentHostGroupsV4Response;
import com.sequenceiq.periscope.cache.DependentHostGroupsCache;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;

@Service
public class DependentHostGroupsService {

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Cacheable(cacheNames = DependentHostGroupsCache.DEPENDENT_HOST_GROUPS_CACHE, key = "{#stackCrn,#hostGroup}")
    public DependentHostGroupsV4Response getDependentHostGroupsForPolicyHostGroup(String stackCrn, String hostGroup) {
        return cloudbreakCommunicator.getDependentHostGroups(stackCrn, hostGroup);
    }
}
