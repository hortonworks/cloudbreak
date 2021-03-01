package com.sequenceiq.environment.parameters.dao.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.sequenceiq.environment.parameter.dto.ResourceGroupCreation;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;
import com.sequenceiq.environment.parameters.dao.converter.ResourceGroupCreationConverter;
import com.sequenceiq.environment.parameters.dao.converter.ResourceGroupUsagePatternConverter;

@Entity
@DiscriminatorValue("AZURE")
public class AzureParameters extends BaseParameters {

    @Column(name = "resource_group_name")
    private String resourceGroupName;

    @Column(name = "resource_group_creation")
    @Convert(converter = ResourceGroupCreationConverter.class)
    private ResourceGroupCreation resourceGroupCreation;

    @Column(name = "resource_group_single")
    @Convert(converter = ResourceGroupUsagePatternConverter.class)
    private ResourceGroupUsagePattern resourceGroupUsagePattern;

    @Column(name = "encryption_parameter_keyUrl")
    private String resourceEncryptionParameterKeyUrl;

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

    public String getResourceEncryptionParameterKeyUrl() {
        return resourceEncryptionParameterKeyUrl;
    }

    public void setResourceEncryptionParameterKeyUrl(String resourceEncryptionParameterKeyUrl) {
        this.resourceEncryptionParameterKeyUrl = resourceEncryptionParameterKeyUrl;
    }
}
