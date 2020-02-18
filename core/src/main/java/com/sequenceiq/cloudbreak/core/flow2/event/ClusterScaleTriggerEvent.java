package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class ClusterScaleTriggerEvent extends StackEvent implements HostGroupPayload {
    private final String hostGroup;

    private final Integer adjustment;

    private final Set<String> hostNames;

    private final boolean singlePrimaryGateway;

    private final boolean kerberosSecured;

    private final boolean singleNodeCluster;

    private final ClusterManagerType clusterManagerType;

    public ClusterScaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, Set<String> hostNames, boolean singlePrimaryGateway,
            boolean kerberosSecured, boolean singleNodeCluster, ClusterManagerType clusterManagerType) {
        super(selector, stackId);
        this.hostGroup = hostGroup;
        this.adjustment = adjustment;
        this.hostNames = hostNames;
        this.singlePrimaryGateway = singlePrimaryGateway;
        this.kerberosSecured = kerberosSecured;
        this.singleNodeCluster = singleNodeCluster;
        this.clusterManagerType = clusterManagerType;
    }

    public ClusterScaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment) {
        this(selector, stackId, hostGroup, adjustment, Collections.emptySet(), false, false, false, ClusterManagerType.CLOUDERA_MANAGER);
    }

    public ClusterScaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.hostGroup = hostGroup;
        this.adjustment = adjustment;
        hostNames = Collections.emptySet();
        singlePrimaryGateway = false;
        kerberosSecured = false;
        singleNodeCluster = false;
        clusterManagerType = ClusterManagerType.CLOUDERA_MANAGER;
    }

    @Override
    public String getHostGroupName() {
        return hostGroup;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public Set<String> getHostNames() {
        return hostNames;
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

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }
}
