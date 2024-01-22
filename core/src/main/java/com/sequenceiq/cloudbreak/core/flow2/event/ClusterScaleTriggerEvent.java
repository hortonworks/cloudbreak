package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterScaleTriggerEvent extends StackEvent {

    private final Map<String, Set<Long>> hostGroupsWithPrivateIds;

    private final Map<String, Integer> hostGroupsWithAdjustment;

    private final Map<String, Set<String>> hostGroupsWithHostNames;

    private final boolean singlePrimaryGateway;

    private final boolean kerberosSecured;

    private final boolean singleNodeCluster;

    private final boolean restartServices;

    private final ClusterManagerType clusterManagerType;

    private final boolean rollingRestartEnabled;

    @JsonCreator
    public ClusterScaleTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("hostGroupsWithAdjustment") Map<String, Integer> hostGroupsWithAdjustment,
            @JsonProperty("hostGroupsWithPrivateIds") Map<String, Set<Long>> hostGroupsWithPrivateIds,
            @JsonProperty("hostGroupsWithHostNames") Map<String, Set<String>> hostGroupsWithHostNames,
            @JsonProperty("singlePrimaryGateway") boolean singlePrimaryGateway,
            @JsonProperty("kerberosSecured") boolean kerberosSecured,
            @JsonProperty("singleNodeCluster") boolean singleNodeCluster,
            @JsonProperty("restartServices") boolean restartServices,
            @JsonProperty("clusterManagerType") ClusterManagerType clusterManagerType,
            @JsonProperty("rollingRestartEnabled") boolean rollingRestartEnabled) {
        super(selector, stackId);
        this.hostGroupsWithAdjustment = hostGroupsWithAdjustment == null ? Collections.emptyMap() : hostGroupsWithAdjustment;
        this.hostGroupsWithPrivateIds = hostGroupsWithPrivateIds;
        this.hostGroupsWithHostNames = hostGroupsWithHostNames == null ? Collections.emptyMap() : hostGroupsWithHostNames;
        this.singlePrimaryGateway = singlePrimaryGateway;
        this.kerberosSecured = kerberosSecured;
        this.singleNodeCluster = singleNodeCluster;
        this.restartServices = restartServices;
        this.clusterManagerType = clusterManagerType;
        this.rollingRestartEnabled = rollingRestartEnabled;
    }

    public ClusterScaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupsWithAdjustment,
        Map<String, Set<Long>> hostGroupsWithPrivateIds, Map<String, Set<String>> hostGroupsWithHostNames) {
        this(selector, stackId, hostGroupsWithAdjustment, hostGroupsWithPrivateIds, hostGroupsWithHostNames, false, false, false, false,
                ClusterManagerType.CLOUDERA_MANAGER, false);
    }

    public ClusterScaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupsWithAdjustment,
        Map<String, Set<Long>> hostGroupsWithPrivateIds, Map<String, Set<String>> hostGroupsWithHostNames, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.hostGroupsWithAdjustment = hostGroupsWithAdjustment == null ? Collections.emptyMap() : hostGroupsWithAdjustment;
        this.hostGroupsWithPrivateIds = hostGroupsWithPrivateIds == null ? Collections.emptyMap() : hostGroupsWithPrivateIds;
        this.hostGroupsWithHostNames = hostGroupsWithHostNames == null ? Collections.emptyMap() : hostGroupsWithHostNames;
        singlePrimaryGateway = false;
        kerberosSecured = false;
        singleNodeCluster = false;
        restartServices = false;
        clusterManagerType = ClusterManagerType.CLOUDERA_MANAGER;
        this.rollingRestartEnabled = false;
    }

    public Map<String, Set<Long>> getHostGroupsWithPrivateIds() {
        return hostGroupsWithPrivateIds;
    }

    public Map<String, Integer> getHostGroupsWithAdjustment() {
        return hostGroupsWithAdjustment;
    }

    public Map<String, Set<String>> getHostGroupsWithHostNames() {
        return hostGroupsWithHostNames;
    }

    @JsonIgnore
    public Set<String> getHostGroups() {
        return getHostGroupsWithAdjustment().keySet();
    }

    public boolean isKerberosSecured() {
        return kerberosSecured;
    }

    public boolean isSinglePrimaryGateway() {
        return singlePrimaryGateway;
    }

    public boolean isSingleNodeCluster() {
        return singleNodeCluster;
    }

    public boolean isRestartServices() {
        return restartServices;
    }

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }

    public boolean isRollingRestartEnabled() {
        return rollingRestartEnabled;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterScaleTriggerEvent.class.getSimpleName() + "[", "]")
                .add("hostGroupsWithPrivateIds=" + hostGroupsWithPrivateIds)
                .add("hostGroupsWithAdjustment=" + hostGroupsWithAdjustment)
                .add("hostGroupsWithHostNames=" + hostGroupsWithHostNames)
                .add("singlePrimaryGateway=" + singlePrimaryGateway)
                .add("kerberosSecured=" + kerberosSecured)
                .add("singleNodeCluster=" + singleNodeCluster)
                .add("restartServices=" + restartServices)
                .add("clusterManagerType=" + clusterManagerType)
                .add("rollingRestartEnabled=" + rollingRestartEnabled)
                .add(super.toString())
                .toString();
    }
}
