package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Set;

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

    public ClusterScaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, Set<String> hostNames, boolean singlePrimaryGateway,
            boolean kerberosSecured, boolean singleNodeCluster) {
        super(selector, stackId);
        this.hostGroup = hostGroup;
        this.adjustment = adjustment;
        this.hostNames = hostNames;
        this.singlePrimaryGateway = singlePrimaryGateway;
        this.kerberosSecured = kerberosSecured;
        this.singleNodeCluster = singleNodeCluster;
    }

    public ClusterScaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment) {
        this(selector, stackId, hostGroup, adjustment, Collections.emptySet(), false, false, false);
    }

    public ClusterScaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, Promise<Boolean> accepted) {
        super(selector, stackId, accepted);
        this.hostGroup = hostGroup;
        this.adjustment = adjustment;
        this.hostNames = Collections.emptySet();
        this.singlePrimaryGateway = false;
        this.kerberosSecured = false;
        this.singleNodeCluster = false;
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
}
