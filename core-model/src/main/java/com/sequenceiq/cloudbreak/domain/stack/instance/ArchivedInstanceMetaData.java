package com.sequenceiq.cloudbreak.domain.stack.instance;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.InstanceStatusConverter;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.InstanceLifeCycleConverter;
import com.sequenceiq.cloudbreak.domain.converter.InstanceMetadataTypeConverter;
import com.sequenceiq.cloudbreak.util.DatabaseUtil;

@Entity
public class ArchivedInstanceMetaData implements ProvisionEntity {
    @Id
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

    @Convert(converter = InstanceStatusConverter.class)
    private InstanceStatus instanceStatus;

    @Convert(converter = InstanceMetadataTypeConverter.class)
    private InstanceMetadataType instanceMetadataType;

    private String localityIndicator;

    @ManyToOne(fetch = FetchType.LAZY)
    private InstanceGroup instanceGroup;

    private Long startDate;

    private Long terminationDate;

    /**
     * ID of the subnet (in a cloud platform specific format) the cloud instance is deployed in.
     */
    private String subnetId;

    /**
     * Name of the availability zone the cloud instance is deployed in. May be {@code null} if the cloud platform does not support this construct.
     */
    private String availabilityZone;

    /**
     * ID of the virtual network rack the cloud instance is deployed in. Interpretation and syntax are as per the
     * <a href="https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/RackAwareness.html">Hadoop Rack Awareness</a> page.
     */
    @Column(columnDefinition = "TEXT")
    private String rackId;

    private String instanceName;

    private String statusReason;

    @Convert(converter = InstanceLifeCycleConverter.class)
    private InstanceLifeCycle lifeCycle;

    private String variant;

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

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Json getImage() {
        return image;
    }

    public void setImage(Json image) {
        this.image = image;
    }

    public Boolean getAmbariServer() {
        return ambariServer;
    }

    public void setAmbariServer(Boolean ambariServer) {
        this.ambariServer = ambariServer;
    }

    public Boolean getClusterManagerServer() {
        return clusterManagerServer;
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

    public String getServerCert() {
        return serverCert;
    }

    public void setServerCert(String serverCert) {
        this.serverCert = serverCert;
    }

    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public InstanceMetadataType getInstanceMetadataType() {
        return instanceMetadataType;
    }

    public void setInstanceMetadataType(InstanceMetadataType instanceMetadataType) {
        this.instanceMetadataType = instanceMetadataType;
    }

    public String getLocalityIndicator() {
        return localityIndicator;
    }

    public void setLocalityIndicator(String localityIndicator) {
        this.localityIndicator = localityIndicator;
    }

    public InstanceGroup getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(InstanceGroup instanceGroup) {
        this.instanceGroup = instanceGroup;
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

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getRackId() {
        return rackId;
    }

    public void setRackId(String rackId) {
        this.rackId = rackId;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public InstanceLifeCycle getLifeCycle() {
        return lifeCycle;
    }

    public void setLifeCycle(InstanceLifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    @Override
    public String toString() {
        return "ArchivedInstanceMetaData{" +
                "id=" + id +
                ", privateId=" + privateId +
                ", privateIp='" + privateIp + '\'' +
                ", publicIp='" + publicIp + '\'' +
                ", sshPort=" + sshPort +
                ", instanceId='" + instanceId + '\'' +
                ", image=" + image +
                ", ambariServer=" + ambariServer +
                ", clusterManagerServer=" + clusterManagerServer +
                ", discoveryFQDN='" + discoveryFQDN + '\'' +
                ", serverCert='" + serverCert + '\'' +
                ", instanceStatus=" + instanceStatus +
                ", instanceMetadataType=" + instanceMetadataType +
                ", localityIndicator='" + localityIndicator + '\'' +
                ", instanceGroup=" + DatabaseUtil.lazyLoadSafeToString(instanceGroup) +
                ", startDate=" + startDate +
                ", terminationDate=" + terminationDate +
                ", subnetId='" + subnetId + '\'' +
                ", availabilityZone='" + availabilityZone + '\'' +
                ", rackId='" + rackId + '\'' +
                ", instanceName='" + instanceName + '\'' +
                ", statusReason='" + statusReason + '\'' +
                ", lifeCycle=" + lifeCycle +
                ", variant='" + variant + '\'' +
                '}';
    }
}
