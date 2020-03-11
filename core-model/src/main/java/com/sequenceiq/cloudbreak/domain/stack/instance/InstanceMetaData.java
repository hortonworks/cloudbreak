package com.sequenceiq.cloudbreak.domain.stack.instance;

import java.util.StringJoiner;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@NamedEntityGraphs({
        @NamedEntityGraph(name = "InstanceMetaData.instanceGroup",
                attributeNodes = {
                        @NamedAttributeNode(value = "instanceGroup")
                }
        ),
})
@Entity
public class InstanceMetaData implements ProvisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "instancemetadata_generator")
    @SequenceGenerator(name = "instancemetadata_generator", sequenceName = "instancemetadata_id_seq", allocationSize = 50)
    private Long id;

    private Long privateId;

    private String privateIp;

    private String publicIp;

    private Integer sshPort;

    private String instanceId;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json image;

    private Boolean ambariServer;

    private Boolean clusterManagerServer;

    private String discoveryFQDN;

    @Column(columnDefinition = "TEXT")
    private String serverCert;

    @Enumerated(EnumType.STRING)
    private InstanceStatus instanceStatus;

    @Enumerated(EnumType.STRING)
    private InstanceMetadataType instanceMetadataType;

    private String localityIndicator;

    @ManyToOne(fetch = FetchType.LAZY)
    private InstanceGroup instanceGroup;

    private Long startDate;

    private Long terminationDate;

    private String subnetId;

    private String instanceName;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

    public InstanceGroup getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(InstanceGroup instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public String getInstanceGroupName() {
        return instanceGroup.getGroupName();
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPrivateId() {
        return privateId;
    }

    public void setPrivateId(Long privateId) {
        this.privateId = privateId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Boolean getAmbariServer() {
        return getClusterManagerServer() != null ? getClusterManagerServer() : ambariServer;
    }

    public Boolean getClusterManagerServer() {
        return clusterManagerServer;
    }

    public void setAmbariServer(Boolean ambariServer) {
        this.ambariServer = ambariServer;
    }

    public void setServer(Boolean ambariServer) {
        this.ambariServer = ambariServer;
        clusterManagerServer = ambariServer;
    }

    public void setClusterManagerServer(Boolean clusterManagerServer) {
        this.clusterManagerServer = clusterManagerServer;
    }

    public String getDiscoveryFQDN() {
        return discoveryFQDN;
    }

    public void setDiscoveryFQDN(String discoveryFQDN) {
        this.discoveryFQDN = discoveryFQDN;
    }

    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public Long getTerminationDate() {
        return terminationDate;
    }

    public void setTerminationDate(Long terminationDate) {
        this.terminationDate = terminationDate;
    }

    public boolean isCreated() {
        return InstanceStatus.CREATED.equals(instanceStatus);
    }

    public boolean isFailed() {
        return instanceStatus == InstanceStatus.FAILED || instanceStatus == InstanceStatus.ORCHESTRATION_FAILED
                || instanceStatus == InstanceStatus.DECOMMISSION_FAILED;
    }

    public boolean isTerminated() {
        return terminationDate != null || InstanceStatus.TERMINATED.equals(instanceStatus);
    }

    public boolean isReachable() {
        return !isTerminated()
                && !isDeletedOnProvider()
                && !isFailed()
                && !InstanceStatus.STOPPED.equals(instanceStatus);
    }

    public boolean isDeletedOnProvider() {
        return InstanceStatus.DELETED_ON_PROVIDER_SIDE.equals(instanceStatus);
    }

    public boolean isUnhealthy() {
        return InstanceStatus.SERVICES_UNHEALTHY.equals(instanceStatus);
    }

    public boolean isRunning() {
        return !isTerminated() && (InstanceStatus.CREATED.equals(instanceStatus) || InstanceStatus.SERVICES_RUNNING.equals(instanceStatus)
                || InstanceStatus.DECOMMISSIONED.equals(instanceStatus) || InstanceStatus.DECOMMISSION_FAILED.equals(instanceStatus)
                || InstanceStatus.SERVICES_HEALTHY.equals(instanceStatus) || InstanceStatus.SERVICES_UNHEALTHY.equals(instanceStatus));
    }

    public boolean isAttached() {
        return InstanceStatus.SERVICES_HEALTHY.equals(instanceStatus) || InstanceStatus.SERVICES_UNHEALTHY.equals(instanceStatus)
                || InstanceStatus.DECOMMISSION_FAILED.equals(instanceStatus);
    }

    public String getLocalityIndicator() {
        return localityIndicator;
    }

    public void setLocalityIndicator(String localityIndicator) {
        this.localityIndicator = localityIndicator;
    }

    public String getPublicIpWrapper() {
        if (publicIp == null) {
            return privateIp;
        }
        return publicIp;
    }

    public InstanceMetadataType getInstanceMetadataType() {
        return instanceMetadataType;
    }

    public void setInstanceMetadataType(InstanceMetadataType instanceMetadataType) {
        this.instanceMetadataType = instanceMetadataType;
    }

    public String getServerCert() {
        return serverCert;
    }

    public void setServerCert(String serverCert) {
        this.serverCert = serverCert;
    }

    public String getDomain() {
        if (discoveryFQDN == null || discoveryFQDN.isEmpty()) {
            return null;
        }
        return discoveryFQDN.contains(".") ? discoveryFQDN.substring(discoveryFQDN.indexOf('.') + 1) : null;
    }

    public String getShortHostname() {
        if (discoveryFQDN == null || discoveryFQDN.isEmpty()) {
            return null;
        }
        return discoveryFQDN.split("\\.")[0];
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public Json getImage() {
        return image;
    }

    public void setImage(Json image) {
        this.image = image;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InstanceMetaData.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("privateId=" + privateId)
                .add("privateIp='" + privateIp + "'")
                .add("publicIp='" + publicIp + "'")
                .add("instanceId='" + instanceId + "'")
                .add("discoveryFQDN='" + discoveryFQDN + "'")
                .add("instanceName='" + instanceName + "'")
                .add("instanceStatus='" + instanceStatus + "'")
                .add("statusReason='" + statusReason + "'")
                .toString();
    }
}
