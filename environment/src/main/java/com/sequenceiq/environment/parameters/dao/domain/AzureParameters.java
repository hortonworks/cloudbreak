package com.sequenceiq.environment.parameters.dao.domain;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.environment.parameter.dto.ResourceGroupCreation;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;
import com.sequenceiq.environment.parameters.dao.converter.ResourceGroupCreationConverter;
import com.sequenceiq.environment.parameters.dao.converter.ResourceGroupUsagePatternConverter;

@Entity
@DiscriminatorValue("AZURE")
public class AzureParameters extends BaseParameters implements AccountIdAwareResource {

    @Column(name = "resource_group_name")
    private String resourceGroupName;

    @Column(name = "resource_group_creation")
    @Convert(converter = ResourceGroupCreationConverter.class)
    private ResourceGroupCreation resourceGroupCreation;

    @Column(name = "resource_group_single")
    @Convert(converter = ResourceGroupUsagePatternConverter.class)
    private ResourceGroupUsagePattern resourceGroupUsagePattern;

    @Column(name = "encryption_key_url")
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret encryptionKeyUrl = Secret.EMPTY;

    @Column(name = "disk_encryption_set_id")
    private String diskEncryptionSetId;

    @Column(name = "encryption_key_resource_group_name")
    private String encryptionKeyResourceGroupName;

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

    public String getEncryptionKeyUrl() {
        return getIfNotNull(encryptionKeyUrl, Secret::getRaw);
    }

    public String getEncryptionKeyUrlSecret() {
        return getIfNotNull(encryptionKeyUrl, Secret::getSecret);
    }

    public void setEncryptionKeyUrl(String encryptionKeyUrl) {
        this.encryptionKeyUrl = new Secret(encryptionKeyUrl);
    }

    public String getDiskEncryptionSetId() {
        return diskEncryptionSetId;
    }

    public void setDiskEncryptionSetId(String diskEncryptionSetId) {
        this.diskEncryptionSetId = diskEncryptionSetId;
    }

    public String getEncryptionKeyResourceGroupName() {
        return encryptionKeyResourceGroupName;
    }

    public void setEncryptionKeyResourceGroupName(String encryptionKeyResourceGroupName) {
        this.encryptionKeyResourceGroupName = encryptionKeyResourceGroupName;
    }
}