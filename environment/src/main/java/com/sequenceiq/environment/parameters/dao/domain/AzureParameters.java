package com.sequenceiq.environment.parameters.dao.domain;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Entity
@DiscriminatorValue("AZURE")
public class AzureParameters extends BaseParameters {

    @Column(name = "resource_group_name")
    private String resourceGroupName;

    @Column(name = "resource_group_creation")
    @Enumerated(EnumType.STRING)
    private ResourceGroupCreation resourceGroupCreation;

    @Column(name = "resource_group_single")
    @Enumerated(EnumType.STRING)
    private ResourceGroupUsagePattern resourceGroupUsagePattern;

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public ResourceGroupCreation getResourceGroupCreation() {
        return resourceGroupCreation;
    }

    public void setResourceGroupCreation(ResourceGroupCreation resourceGroupCreation) {
        this.resourceGroupCreation = resourceGroupCreation;
    }

    public ResourceGroupUsagePattern getResourceGroupUsagePattern() {
        return resourceGroupUsagePattern;
    }

    public void setResourceGroupUsagePattern(ResourceGroupUsagePattern resourceGroupUsagePattern) {
        this.resourceGroupUsagePattern = resourceGroupUsagePattern;
    }
}
