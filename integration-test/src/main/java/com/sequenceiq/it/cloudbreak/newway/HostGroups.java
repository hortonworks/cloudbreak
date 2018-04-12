package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.it.IntegrationTestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class HostGroups extends Entity {
    public static final String HOSTGROUPS_REQUEST = "HOSTGROUPS_REQUEST";

    private List<InstanceGroupV2Request> request;

    HostGroups(String newId) {
        super(newId);
        this.request = new ArrayList<>();
    }

    HostGroups() {
        this(HOSTGROUPS_REQUEST);
    }

    public List<InstanceGroupV2Request> getRequest() {
        return request;
    }

    public void setRequest(List<InstanceGroupV2Request> request) {
        this.request = request;
    }

    public HostGroups addHostGroup(InstanceGroupV2Request hostGroup) {
        request.add(hostGroup);
        return this;
    }

    public static Function<IntegrationTestContext, HostGroups> getTestContextHostGroups(String key) {
        return (testContext) -> testContext.getContextParam(key, HostGroups.class);
    }

    public static Function<IntegrationTestContext, HostGroups> getTestContextHostGroups() {
        return getTestContextHostGroups(HOSTGROUPS_REQUEST);
    }

    public static Function<IntegrationTestContext, HostGroups> getNewHostGroups() {
        return (testContext) -> new HostGroups();
    }

    public static HostGroups request(String key) {
        return new HostGroups(key);
    }

    public static HostGroups request() {
        return new HostGroups();
    }
}

