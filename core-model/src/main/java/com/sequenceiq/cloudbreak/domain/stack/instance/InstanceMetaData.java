package com.sequenceiq.cloudbreak.domain.stack.instance;

import java.util.StringJoiner;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedSubgraph;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.InstanceStatusConverter;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.InstanceLifeCycleConverter;
import com.sequenceiq.cloudbreak.domain.converter.InstanceMetadataTypeConverter;
import com.sequenceiq.common.api.type.InstanceGroupType;

@NamedEntityGraphs({
        @NamedEntityGraph(name = "InstanceMetaData.instanceGroup",
                attributeNodes = {
                        @NamedAttributeNode(value = "instanceGroup", subgraph = "instanceGroup")
                },
                subgraphs = {
                    @NamedSubgraph(name = "instanceGroup", attributeNodes = @NamedAttributeNode("template"))
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

    public void setServer(Boolean server) {
        ambariServer = server;
        clusterManagerServer = server;
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
                && !InstanceStatus.ORCHESTRATION_FAILED.equals(instanceStatus)
                && !InstanceStatus.FAILED.equals(instanceStatus)
                && !InstanceStatus.STOPPED.equals(instanceStatus);
    }

    public boolean isDeletedOnProvider() {
        return InstanceStatus.DELETED_ON_PROVIDER_SIDE.equals(instanceStatus) || InstanceStatus.DELETED_BY_PROVIDER.equals(instanceStatus);
    }

    public boolean isHealthy() {
        return InstanceStatus.SERVICES_HEALTHY.equals(instanceStatus) || InstanceStatus.SERVICES_RUNNING.equals(instanceStatus);
    }

    public boolean isServicesUnhealthy() {
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

    public boolean isGateway() {
        return InstanceMetadataType.GATEWAY == instanceMetadataType || InstanceMetadataType.GATEWAY_PRIMARY == instanceMetadataType
                || (instanceMetadataType == null && instanceGroup != null && InstanceGroupType.GATEWAY == instanceGroup.getInstanceGroupType());
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

    public String getIpWrapper(boolean preferPrivateIp) {
        return preferPrivateIp ? privateIp : getPublicIpWrapper();
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
        return new StringJoiner(", ", InstanceMetaData.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("privateId=" + privateId)
                .add("privateIp='" + privateIp + "'")
                .add("publicIp='" + publicIp + "'")
                .add("instanceId='" + instanceId + "'")
                .add("discoveryFQDN='" + discoveryFQDN + "'")
                .add("instanceStatus=" + instanceStatus)
                .add("instanceMetadataType=" + instanceMetadataType)
                .add("instanceName='" + instanceName + "'")
                .add("statusReason='" + statusReason + "'")
                .add("variant='" + variant + "'")
                .toString();
    }
}
