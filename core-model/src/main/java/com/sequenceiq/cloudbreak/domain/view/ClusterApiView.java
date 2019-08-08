package com.sequenceiq.cloudbreak.domain.view;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
@Table(name = "Cluster")
public class ClusterApiView extends CompactView {
    @OneToOne(fetch = FetchType.LAZY)
    private StackApiView stack;

    @OneToMany(mappedBy = "cluster")
    private Set<HostGroupView> hostGroups = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    private BlueprintView blueprint;

    private String clusterManagerIp;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String environmentCrn;

    @Override
    public AuthorizationResource getResource() {
        return AuthorizationResource.DATAHUB;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public StackApiView getStack() {
        return stack;
    }

    public void setStack(StackApiView stack) {
        this.stack = stack;
    }

    public Set<HostGroupView> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroupView> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public BlueprintView getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintView blueprint) {
        this.blueprint = blueprint;
    }

    public String getClusterManagerIp() {
        return clusterManagerIp;
    }

    public void setClusterManagerIp(String clusterManagerIp) {
        this.clusterManagerIp = clusterManagerIp;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
