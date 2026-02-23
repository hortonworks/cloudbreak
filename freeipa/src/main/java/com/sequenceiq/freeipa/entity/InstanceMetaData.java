package com.sequenceiq.freeipa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestrationNode;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceLifeCycle;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.util.InstanceLifeCycleConverter;
import com.sequenceiq.freeipa.entity.util.InstanceMetadataTypeConverter;
import com.sequenceiq.freeipa.entity.util.InstanceStatusConverter;

@Entity
public class InstanceMetaData implements OrchestrationNode {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "instancemetadata_generator")
    @SequenceGenerator(name = "instancemetadata_generator", sequenceName = "instancemetadata_id_seq", allocationSize = 1)
    private Long id;

    private Long privateId;

    private String privateIp;

    private String publicIp;

    private Integer sshPort;

    private String instanceId;

    private String discoveryFQDN;

    @Column(columnDefinition = "TEXT")
    private String serverCert;

    @Convert(converter = InstanceStatusConverter.class)
    private InstanceStatus instanceStatus;

    @Convert(converter = InstanceMetadataTypeConverter.class)
    private InstanceMetadataType instanceMetadataType;

    private String localityIndicator;

    private Long startDate;

    private Long terminationDate;

    private String subnetId;

    private String availabilityZone;

    private String instanceName;

    @ManyToOne
    @JsonBackReference
    private InstanceGroup instanceGroup;

    @Convert(converter = InstanceLifeCycleConverter.class)
    private InstanceLifeCycle lifeCycle;

    private String variant;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json image;

    @Column(name = "userdatasecretresource_id")
    private Long userdataSecretResourceId;

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

    public boolean isAvailable() {
        return instanceStatus.isAvailable();
    }

    public boolean isNotAvailable() {
        return instanceStatus.isNotAvailable();
    }

    public boolean isTerminated() {
        return InstanceStatus.TERMINATED.equals(instanceStatus);
    }

    public boolean isDeletedOnProvider() {
        return InstanceStatus.DELETED_ON_PROVIDER_SIDE.equals(instanceStatus) || InstanceStatus.DELETED_BY_PROVIDER.equals(instanceStatus);
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

    public InstanceGroup getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(InstanceGroup instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public InstanceLifeCycle getLifeCycle() {
        return lifeCycle;
    }

    public void setLifeCycle(InstanceLifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public Json getImage() {
        return image;
    }

    public void setImage(Json image) {
        this.image = image;
    }

    public Long getUserdataSecretResourceId() {
        return userdataSecretResourceId;
    }

    public void setUserdataSecretResourceId(Long userdataSecretResourceId) {
        this.userdataSecretResourceId = userdataSecretResourceId;
    }

    @Override
    public String toString() {
        return "InstanceMetaData{" +
                "id=" + id +
                ", privateId=" + privateId +
                ", privateIp='" + privateIp + '\'' +
                ", publicIp='" + publicIp + '\'' +
                ", sshPort=" + sshPort +
                ", instanceId='" + instanceId + '\'' +
                ", discoveryFQDN='" + discoveryFQDN + '\'' +
                ", instanceStatus=" + instanceStatus +
                ", instanceMetadataType=" + instanceMetadataType +
                ", localityIndicator='" + localityIndicator + '\'' +
                ", startDate=" + startDate +
                ", terminationDate=" + terminationDate +
                ", subnetId='" + subnetId + '\'' +
                ", availabilityZone='" + availabilityZone + '\'' +
                ", instanceName='" + instanceName + '\'' +
                ", instanceGroup=" + (instanceGroup == null ? null : instanceGroup.getId()) +
                ", lifeCycle=" + lifeCycle +
                ", variant='" + variant + '\'' +
                ", userdataSecretResource=" + userdataSecretResourceId +
                '}';
    }

    @Override
    public Node getNode() {
        return new Node(getPrivateIp(), getPublicIpWrapper(), getInstanceId(), getInstanceGroup().getTemplate().getInstanceType(),
                getDiscoveryFQDN(), getInstanceGroup().getGroupName());
    }
}
