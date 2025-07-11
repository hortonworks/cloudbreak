package com.sequenceiq.cloudbreak.domain.view;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.converter.CertExpirationStateConverter;
import com.sequenceiq.cloudbreak.domain.converter.StatusConverter;
import com.sequenceiq.common.api.type.CertExpirationState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
@Table(name = "Cluster")
@Deprecated
public class ClusterApiView extends CompactView {
    @OneToOne(fetch = FetchType.LAZY)
    private StackApiView stack;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_id")
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
                ", hostGroups=" + hostGroups +
                ", blueprint=" + blueprint +
                ", clusterManagerIp='" + clusterManagerIp + '\'' +
                ", status=" + status +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", certExpirationState=" + certExpirationState +
                "} " + super.toString();
    }
}
