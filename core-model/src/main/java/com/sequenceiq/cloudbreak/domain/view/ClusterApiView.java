package com.sequenceiq.cloudbreak.domain.view;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
@Table(name = "Cluster")
public class ClusterApiView extends CompactView {
    @OneToOne(fetch = FetchType.LAZY)
    private StackApiView stack;

    @ManyToOne
    private KerberosConfig kerberosConfig;

    @OneToMany(mappedBy = "cluster")
    private Set<HostGroupView> hostGroups = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "blueprint_id")
    private ClusterDefinitionView clusterDefinition;

    private String ambariIp;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne
    private EnvironmentView environment;

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.STACK;
    }

    public EnvironmentView getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentView environment) {
        this.environment = environment;
    }

    public StackApiView getStack() {
        return stack;
    }

    public void setStack(StackApiView stack) {
        this.stack = stack;
    }

    public KerberosConfig getKerberosConfig() {
        return kerberosConfig;
    }

    public void setKerberosConfig(KerberosConfig kerberosConfig) {
        this.kerberosConfig = kerberosConfig;
    }

    public Set<HostGroupView> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroupView> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public ClusterDefinitionView getClusterDefinition() {
        return clusterDefinition;
    }

    public void setClusterDefinition(ClusterDefinitionView clusterDefinition) {
        this.clusterDefinition = clusterDefinition;
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
