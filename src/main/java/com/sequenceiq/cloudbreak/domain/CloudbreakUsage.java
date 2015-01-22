package com.sequenceiq.cloudbreak.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class CloudbreakUsage implements ProvisionEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String owner;

    private String account;

    private Long stackId;

    private String stackName;

    private String provider;

    private String region;

    private Date day;

    private Long instanceHours;

    private Double costs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Long getInstanceHours() {
        return instanceHours;
    }

    public void setInstanceHours(Long instanceHours) {
        this.instanceHours = instanceHours;
    }

    public Date getDay() {
        return day;
    }

    public void setDay(Date day) {
        this.day = day;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudbreakUsage{");
        sb.append("id=").append(id);
        sb.append(", owner='").append(owner).append('\'');
        sb.append(", account='").append(account).append('\'');
        sb.append(", day=").append(day);
        sb.append(", provider='").append(provider).append('\'');
        sb.append(", region='").append(region).append('\'');
        sb.append(", instanceHours='").append(instanceHours).append('\'');
        sb.append(", stackId='").append(stackId).append('\'');
        sb.append(", stackName='").append(stackName).append('\'');
        sb.append(", costs='").append(costs).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public Double getCosts() {
        return costs;
    }

    public void setCosts(Double costs) {
        this.costs = costs;
    }
}
