package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class ClusterScaleTriggerEvent extends StackEvent {

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    private String hostGroup;

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    private Integer adjustment;

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    private Set<String> hostNames;

    private Map<String, Set<Long>> hostGroupsWithPrivateIds;

    private Map<String, Integer> hostGroupsWithAdjustment;

    private Map<String, Set<String>> hostGroupsWithHostNames;

    private final boolean singlePrimaryGateway;

    private final boolean kerberosSecured;

    private final boolean singleNodeCluster;

    private final boolean restartServices;

    private final ClusterManagerType clusterManagerType;

    public ClusterScaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupsWithAdjustment,
            Map<String, Set<Long>> hostGroupsWithPrivateIds, Map<String, Set<String>> hostGroupsWithHostNames, boolean singlePrimaryGateway,
            boolean kerberosSecured, boolean singleNodeCluster, boolean restartServices, ClusterManagerType clusterManagerType) {
        super(selector, stackId);
        this.hostGroupsWithAdjustment = hostGroupsWithAdjustment;
        this.hostGroupsWithPrivateIds = hostGroupsWithPrivateIds;
        this.hostGroupsWithHostNames = hostGroupsWithHostNames;
        this.singlePrimaryGateway = singlePrimaryGateway;
        this.kerberosSecured = kerberosSecured;
        this.singleNodeCluster = singleNodeCluster;
        this.restartServices = restartServices;
        this.clusterManagerType = clusterManagerType;
    }

    public ClusterScaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupsWithAdjustment,
            Map<String, Set<Long>> hostGroupsWithPrivateIds, Map<String, Set<String>> hostGroupsWithHostNames) {
        this(selector, stackId, hostGroupsWithAdjustment, hostGroupsWithPrivateIds, hostGroupsWithHostNames, false, false, false, false,
                ClusterManagerType.CLOUDERA_MANAGER);
    }

    public ClusterScaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupsWithAdjustment,
            Map<String, Set<Long>> hostGroupsWithPrivateIds, Map<String, Set<String>> hostGroupsWithHostNames, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.hostGroupsWithAdjustment = hostGroupsWithAdjustment;
        this.hostGroupsWithPrivateIds = hostGroupsWithPrivateIds;
        this.hostGroupsWithHostNames = hostGroupsWithHostNames;
        singlePrimaryGateway = false;
        kerberosSecured = false;
        singleNodeCluster = false;
        restartServices = false;
        clusterManagerType = ClusterManagerType.CLOUDERA_MANAGER;
    }

    public Map<String, Set<Long>> getHostGroupsWithPrivateIds() {
        if (hostGroupsWithPrivateIds == null) {
            hostGroupsWithPrivateIds = Collections.emptyMap();
        }
        return hostGroupsWithPrivateIds;
    }

    public Map<String, Integer> getHostGroupsWithAdjustment() {
        if (hostGroupsWithAdjustment == null) {
            if (hostGroup != null && adjustment != null) {
                hostGroupsWithAdjustment = Collections.singletonMap(hostGroup, adjustment);
            } else {
                hostGroupsWithAdjustment = Collections.emptyMap();
            }
        }
        return hostGroupsWithAdjustment;
    }

    public Map<String, Set<String>> getHostGroupsWithHostNames() {
        if (hostGroupsWithHostNames == null) {
            if (hostGroup != null && hostNames != null && !hostNames.isEmpty()) {
                hostGroupsWithHostNames = Collections.singletonMap(hostGroup, hostNames);
            } else {
                hostGroupsWithHostNames = Collections.emptyMap();
            }
        }
        return hostGroupsWithHostNames;
    }

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
}
