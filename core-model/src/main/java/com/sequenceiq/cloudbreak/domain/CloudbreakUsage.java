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

import com.sequenceiq.cloudbreak.api.model.UsageStatus;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "CloudbreakUsage.findOpensForStack",
                query = "SELECT u FROM CloudbreakUsage u "
                        + "WHERE u.stackId = :stackId "
                        + "AND u.status = 'OPEN'"),
        @NamedQuery(
                name = "CloudbreakUsage.findStoppedForStack",
                query = "SELECT u FROM CloudbreakUsage u "
                        + "WHERE u.stackId = :stackId "
                        + "AND u.status = 'STOPPED'"),
        @NamedQuery(
                name = "CloudbreakUsage.getOpenUsageByStackAndGroupName",
                query = "SELECT u FROM CloudbreakUsage u "
                        + "WHERE u.stackId = :stackId "
                        + "AND u.instanceGroup = :instanceGroupName "
                        + "AND u.status = 'OPEN'"),
        @NamedQuery(
                name = "CloudbreakUsage.findAllOpenAndStopped",
                query = "SELECT u FROM CloudbreakUsage u "
                        + "WHERE (u.status = 'STOPPED' "
                        + "OR u.status = 'OPEN')"
                        + "AND u.day < :today")

})
public class CloudbreakUsage implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cloudbreakusage_generator")
    @SequenceGenerator(name = "cloudbreakusage_generator", sequenceName = "cloudbreakusage_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private Long stackId;

    @Column(nullable = false)
    private String stackName;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String region;

    private String availabilityZone;

    @Column(nullable = false)
    private Date day;

    @Column(nullable = false)
    private Long instanceHours;

    @Column(nullable = false)
    private Double costs;

    @Column(nullable = false)
    private String instanceType;

    @Column(nullable = false)
    private String instanceGroup;

    @Column
    private Date periodStarted;

    @Column
    private String duration;

    @Enumerated(EnumType.STRING)
    private UsageStatus status;

    @Column
    private Integer instanceNum;

    private Long blueprintId;

    private String blueprintName;

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

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public Date getPeriodStarted() {
        return periodStarted;
    }

    public void setPeriodStarted(Date periodStarted) {
        this.periodStarted = periodStarted;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public UsageStatus getStatus() {
        return status;
    }

    public void setStatus(UsageStatus status) {
        this.status = status;
    }

    public Integer getInstanceNum() {
        return instanceNum;
    }

    public void setInstanceNum(Integer instanceNum) {
        this.instanceNum = instanceNum;
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
        sb.append(", blueprintId='").append(blueprintId).append('\'');
        sb.append(", blueprintName='").append(blueprintName).append('\'');
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
