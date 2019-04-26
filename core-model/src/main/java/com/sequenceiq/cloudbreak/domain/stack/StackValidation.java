package com.sequenceiq.cloudbreak.domain.stack;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;

public class StackValidation implements ProvisionEntity {
    private Set<HostGroup> hostGroups;

    private Set<InstanceGroup> instanceGroups;

    private Blueprint blueprint;

    private Network network;

    private Credential credential;

    private EnvironmentView environment;

    public Set<HostGroup> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroup> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public Set<InstanceGroup> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroup> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Blueprint getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(Blueprint blueprint) {
        this.blueprint = blueprint;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public EnvironmentView getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentView environment) {
        this.environment = environment;
    }
}
