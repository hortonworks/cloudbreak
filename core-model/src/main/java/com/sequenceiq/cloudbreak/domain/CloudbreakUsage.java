package com.sequenceiq.cloudbreak.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.model.UsageStatus;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"stackId", "instanceGroup", "day"}))
public class CloudbreakUsage implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cloudbreakusage_generator")
    @SequenceGenerator(name = "cloudbreakusage_generator", sequenceName = "cloudbreakusage_id_seq", allocationSize = 1)
    private Long id;

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

    @Column
    private Integer peak;

    @Column
    private String flexId;

    @Column
    private String smartSenseId;

    @Column
    private String stackUuid;

    @Column
    private String parentUuid;

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

    public Integer getPeak() {
        return peak;
    }

    public void setPeak(Integer peak) {
        this.peak = peak;
    }

    public String getFlexId() {
        return flexId;
    }

    public void setFlexId(String flexId) {
        this.flexId = flexId;
    }

    public String getStackUuid() {
        return stackUuid;
    }

    public void setStackUuid(String stackUuid) {
        this.stackUuid = stackUuid;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getSmartSenseId() {
        return smartSenseId;
    }

    public void setSmartSenseId(String smartSenseId) {
        this.smartSenseId = smartSenseId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CloudbreakUsage{");
        sb.append("id=").append(id);
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
