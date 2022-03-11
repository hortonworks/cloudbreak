package com.sequenceiq.cloudbreak.domain.view;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.converter.CertExpirationStateConverter;
import com.sequenceiq.cloudbreak.domain.converter.StatusConverter;
import com.sequenceiq.common.api.type.CertExpirationState;

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

    @Convert(converter = StatusConverter.class)
    private Status status;

    private String environmentCrn;

    @Convert(converter = CertExpirationStateConverter.class)
    private CertExpirationState certExpirationState;

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

    public CertExpirationState getCertExpirationState() {
        return certExpirationState;
    }

    public void setCertExpirationState(CertExpirationState certExpirationState) {
        this.certExpirationState = certExpirationState;
    }

    @Override
    public String toString() {
        return "ClusterApiView{" +
                ", clusterManagerIp='" + clusterManagerIp + '\'' +
                ", status=" + status +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", certExpirationState=" + certExpirationState +
                '}';
    }
}
