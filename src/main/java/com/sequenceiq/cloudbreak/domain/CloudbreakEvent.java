package com.sequenceiq.cloudbreak.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

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
                        + "ORDER BY cbe.eventTimestamp ASC")
})
@Table(name = "cloudbreakevent")
public class CloudbreakEvent implements ProvisionEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String eventType;
    private Date eventTimestamp;
    private String eventMessage;
    private String owner;
    private String account;
    private String cloud;
    private String region;
    private String vmType;
    private String blueprintName;
    private long blueprintId;
    private Long stackId;
    private String stackName;

    @Enumerated(EnumType.STRING)
    private Status stackStatus;

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

    public String getVmType() {
        return vmType;
    }

    public void setVmType(String vmType) {
        this.vmType = vmType;
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
        sb.append(", vmType='").append(vmType).append('\'');
        sb.append(", blueprintName='").append(blueprintName).append('\'');
        sb.append(", blueprintId=").append(blueprintId);
        sb.append(", stackId=").append(stackId);
        sb.append('}');
        return sb.toString();
    }
}
