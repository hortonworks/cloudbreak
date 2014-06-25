package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;

@Entity
@Table(name = "AwsTemplate", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "awsTemplate_awsTemplateOwner", "name" })
})
public class AwsTemplate extends Template implements ProvisionEntity {

    @Column(nullable = false)
    private String name;
    private String description;
    @Enumerated(EnumType.STRING)
    private Regions region;
    private String keyName;
    private String amiId;
    @Enumerated(EnumType.STRING)
    private InstanceType instanceType;
    private String sshLocation;

    @ManyToOne
    @JoinColumn(name = "awsTemplate_awsTemplateOwner")
    private User awsTemplateOwner;

    public AwsTemplate() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getAmiId() {
        return amiId;
    }

    public void setAmiId(String amiId) {
        this.amiId = amiId;
    }

    public InstanceType getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(InstanceType instanceType) {
        this.instanceType = instanceType;
    }

    public String getSshLocation() {
        return sshLocation;
    }

    public void setSshLocation(String sshLocation) {
        this.sshLocation = sshLocation;
    }

    public User getAwsTemplateOwner() {
        return awsTemplateOwner;
    }

    public void setAwsTemplateOwner(User awsTemplateOwner) {
        this.awsTemplateOwner = awsTemplateOwner;
    }

    @Override
    public void setUser(User user) {
        this.awsTemplateOwner = user;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public User getOwner() {
        return awsTemplateOwner;
    }
}
