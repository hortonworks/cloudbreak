package com.sequenceiq.cloudbreak.domain.view;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.InstanceStatusConverter;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.InstanceLifeCycleConverter;
import com.sequenceiq.cloudbreak.domain.converter.InstanceMetadataTypeConverter;

@Entity
@Table(name = "InstanceMetaData")
@Deprecated
public class InstanceMetaDataView implements ProvisionEntity {

    @Id
    private Long id;

    @ManyToOne
    private InstanceGroupView instanceGroup;

    @Column(nullable = false)
    @Convert(converter = InstanceStatusConverter.class)
    private InstanceStatus instanceStatus;

    @Column
    private String instanceName;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

    private Long privateId;

    private String privateIp;

    private String publicIp;

    private Integer sshPort;

    private String instanceId;

    private Boolean ambariServer;

    private Boolean clusterManagerServer;

    private String discoveryFQDN;

    @Convert(converter = InstanceMetadataTypeConverter.class)
    private InstanceMetadataType instanceMetadataType;

    private String localityIndicator;

    private Long startDate;

    private Long terminationDate;

    private String subnetId;

    private String availabilityZone;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json image;

    /**
     * ID of the virtual network rack the cloud instance is deployed in. Interpretation and syntax are as per the
     * <a href="https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/RackAwareness.html">Hadoop Rack Awareness</a> page.
     */
    @Column(columnDefinition = "TEXT")
    private String rackId;

    @Convert(converter = InstanceLifeCycleConverter.class)
    private InstanceLifeCycle lifeCycle;

    private String variant;

    public boolean isTerminated() {
        return InstanceStatus.TERMINATED.equals(instanceStatus);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public InstanceGroupView getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(InstanceGroupView instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Long getPrivateId() {
        return privateId;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public Boolean getAmbariServer() {
        return ambariServer;
    }

    public Boolean getClusterManagerServer() {
        return clusterManagerServer;
    }

    public String getDiscoveryFQDN() {
        return discoveryFQDN;
    }

    public InstanceMetadataType getInstanceMetadataType() {
        return instanceMetadataType;
    }

    public String getLocalityIndicator() {
        return localityIndicator;
    }

    public Long getStartDate() {
        return startDate;
    }

    public Long getTerminationDate() {
        return terminationDate;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public String getRackId() {
        return rackId;
    }

    public InstanceLifeCycle getLifeCycle() {
        return lifeCycle;
    }

    public String getVariant() {
        return variant;
    }

    public Json getImage() {
        return image;
    }

    @Override
    public String toString() {
        return "InstanceMetaDataView{" +
                "id=" + id +
                ", instanceGroup=" + instanceGroup +
                ", instanceStatus=" + instanceStatus +
                ", instanceName='" + instanceName + '\'' +
                ", statusReason='" + statusReason + '\'' +
                ", privateId=" + privateId +
                ", privateIp='" + privateIp + '\'' +
                ", publicIp='" + publicIp + '\'' +
                ", sshPort=" + sshPort +
                ", instanceId='" + instanceId + '\'' +
                ", ambariServer=" + ambariServer +
                ", clusterManagerServer=" + clusterManagerServer +
                ", discoveryFQDN='" + discoveryFQDN + '\'' +
                ", instanceMetadataType=" + instanceMetadataType +
                ", localityIndicator='" + localityIndicator + '\'' +
                ", startDate=" + startDate +
                ", terminationDate=" + terminationDate +
                ", subnetId='" + subnetId + '\'' +
                ", availabilityZone='" + availabilityZone + '\'' +
                ", image=" + image +
                ", rackId='" + rackId + '\'' +
                ", lifeCycle=" + lifeCycle +
                ", variant='" + variant + '\'' +
                '}';
    }
}
