package com.sequenceiq.cloudbreak.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class CloudbreakUsage implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cloudbreakusage_generator")
    @SequenceGenerator(name = "cloudbreakusage_generator", sequenceName = "cloudbreakusage_id_seq", allocationSize = 1)
    private Long id;

    private String owner;

    private String account;

    private Long stackId;

    private String stackName;

    private String provider;

    private String region;

    private String availabilityZone;

    private Date day;

    private Long instanceHours;

    private Double costs;

    private String instanceType;

    private String instanceGroup;

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

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
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
        sb.append(", availabilityZone='").append(availabilityZone).append('\'');
        sb.append(", instanceHours='").append(instanceHours).append('\'');
        sb.append(", stackId='").append(stackId).append('\'');
        sb.append(", stackName='").append(stackName).append('\'');
        sb.append(", instanceType='").append(instanceType).append('\'');
        sb.append(", instanceGroup='").append(instanceGroup).append('\'');
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
