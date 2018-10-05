package com.sequenceiq.cloudbreak.domain.view;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
@Table(name = "Cluster")
public class ClusterApiView extends CompactViewWithOwner {
    @OneToOne(fetch = FetchType.LAZY)
    private StackApiView stack;

    @ManyToOne
    private KerberosConfig kerberosConfig;

    @Column(nullable = false)
    private Boolean secure;

    @OneToMany(mappedBy = "cluster")
    private Set<HostGroupView> hostGroups = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    private BlueprintView blueprint;

    private String ambariIp;

    private String emailTo;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.STACK;
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

    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
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

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
