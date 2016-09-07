package com.sequenceiq.cloudbreak.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.model.Status;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "CloudbreakEvent.cloudbreakEvents",
                query = "SELECT cbe FROM CloudbreakEvent cbe "
                        + "WHERE cbe.owner= :owner ORDER BY cbe.eventTimestamp ASC"),
        @NamedQuery(
                name = "CloudbreakEvent.cloudbreakEventsSince",
                query = "SELECT cbe FROM CloudbreakEvent cbe "
                        + "WHERE cbe.owner= :owner AND cbe.eventTimestamp > :since "
                        + "ORDER BY cbe.eventTimestamp ASC"),
        @NamedQuery(
                name = "CloudbreakEvent.findCloudbreakEventsForStack",
                query = "SELECT cbe FROM CloudbreakEvent cbe "
                        + "WHERE cbe.stackId= :stackId")
})
@Table(name = "cloudbreakevent")
public class CloudbreakEvent implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cloudbreakevent_generator")
    @SequenceGenerator(name = "cloudbreakevent_generator", sequenceName = "cloudbreakevent_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Date eventTimestamp;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String eventMessage;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private String cloud;

    @Column(nullable = false)
    private String region;

    private String availabilityZone;

    private String blueprintName;

    private long blueprintId;

    @Column(nullable = false)
    private Long stackId;

    @Column(nullable = false)
    private String stackName;

    private Long clusterId;

    private String clusterName;

    private String instanceGroup;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status stackStatus;

    @Column(nullable = false)
    private Integer nodeCount;

    @Enumerated(EnumType.STRING)
    private Status clusterStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Date getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Date eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getEventMessage() {
        return eventMessage;
    }

    public void setEventMessage(String eventMessage) {
        this.eventMessage = eventMessage;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public Status getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(Status stackStatus) {
        this.stackStatus = stackStatus;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public Status getClusterStatus() {
        return clusterStatus;
    }

    public void setClusterStatus(Status clusterStatus) {
        this.clusterStatus = clusterStatus;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudbreakEvent{");
        sb.append("id=").append(id);
        sb.append(", eventType='").append(eventType).append('\'');
        sb.append(", eventTimestamp=").append(eventTimestamp);
        sb.append(", eventMessage='").append(eventMessage).append('\'');
        sb.append(", owner='").append(owner).append('\'');
        sb.append(", account='").append(account).append('\'');
        sb.append(", cloud='").append(cloud).append('\'');
        sb.append(", region='").append(region).append('\'');
        sb.append(", blueprintName='").append(blueprintName).append('\'');
        sb.append(", instanceGroup='").append(instanceGroup).append('\'');
        sb.append(", blueprintId=").append(blueprintId).append('\'');
        sb.append(", stackId=").append(stackId).append('\'');
        sb.append(", clusterId=").append(clusterId).append('\'');
        sb.append(", clusterName=").append(clusterName);
        sb.append('}');
        return sb.toString();
    }
}
