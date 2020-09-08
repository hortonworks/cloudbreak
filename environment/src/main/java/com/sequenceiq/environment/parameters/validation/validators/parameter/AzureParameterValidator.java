package com.sequenceiq.environment.parameters.validation.validators.parameter;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static com.sequenceiq.environment.parameters.dao.domain.ResourceGroupCreation.USE_EXISTING;
import static com.sequenceiq.environment.parameters.dao.domain.ResourceGroupUsagePattern.USE_MULTIPLE;

import java.util.Objects;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.parameters.dto.AzureParametersDto;
import com.sequenceiq.environment.parameters.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;

@Component
public class AzureParameterValidator implements ParameterValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureParameterValidator.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public ValidationResult validate(EnvironmentDto environmentDto, ParametersDto parametersDto, ValidationResultBuilder validationResultBuilder) {

        LOGGER.debug("ParametersDto: {}", parametersDto);
        AzureParametersDto azureParametersDto = parametersDto.azureParametersDto();
        if (Objects.isNull(azureParametersDto)) {
            return validationResultBuilder.build();
        }

        AzureResourceGroupDto azureResourceGroupDto = azureParametersDto.getAzureResourceGroupDto();
        if (Objects.isNull(azureResourceGroupDto)) {
            return validationResultBuilder.build();
        }

        ValidationResult validationResult = validateEntitlement(validationResultBuilder, azureResourceGroupDto, environmentDto.getAccountId());
        if (validationResult.hasError()) {
            return validationResult;
        }

        if (USE_MULTIPLE.equals(azureResourceGroupDto.getResourceGroupUsagePattern())) {
            return validateResourceGroupUsageMultiple(validationResultBuilder, azureResourceGroupDto);
        }
        if (USE_EXISTING.equals(azureResourceGroupDto.getResourceGroupCreation()) && StringUtils.isBlank(azureResourceGroupDto.getName())) {
            return validationResultBuilder.error("If you use a single resource group for your resources then please " +
                                    "provide the name of that resource group.").build();
        }

        LOGGER.debug("Using single, existing resource group {}", azureResourceGroupDto.getName());
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environmentDto.getCredential());
        AzureClient azureClient = azureClientService.getClient(cloudCredential);
        if (!azureClient.resourceGroupExists(azureResourceGroupDto.getName())) {
            validationResultBuilder.error(
                    String.format("Resource group '%s' does not exist or insufficient permission to access it.", azureResourceGroupDto.getName()));
        }
        return validationResultBuilder.build();
    }

    private ValidationResult validateResourceGroupUsageMultiple(ValidationResultBuilder validationResultBuilder, AzureResourceGroupDto azureResourceGroupDto) {
        if (StringUtils.isNotBlank(azureResourceGroupDto.getName())) {
            return validationResultBuilder.error(
                    String.format("You specified to use multiple resource groups for your resources, " +
                                    "but then the single resource group name '%s' cannot not be specified.",
                    azureResourceGroupDto.getName())).build();
        } else {
            return validationResultBuilder.build();
        }
    }
    //CHECKSTYLE:OFF:FallThroughCheck
    private ValidationResult validateEntitlement(ValidationResultBuilder validationResultBuilder, AzureResourceGroupDto azureResourceGroupDto,
            String accountId) {

        switch (azureResourceGroupDto.getResourceGroupUsagePattern()) {
            case USE_SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT:
                if (!entitlementService.azureSingleResourceGroupDedicatedStorageAccountEnabled(INTERNAL_ACTOR_CRN, accountId)) {
                    LOGGER.info("Invalid request, singleResourceGroupDedicatedStorageAccountEnabled entitlement turned off for account {}", accountId);
                    return validationResultBuilder.error(
                            "You specified to use a single resource group with dedicated storage account for the images, "
                                    + "but that feature is currently disabled").
                            build();
                }
            case USE_SINGLE:
                if (!entitlementService.azureSingleResourceGroupDeploymentEnabled(INTERNAL_ACTOR_CRN, accountId)) {
                    LOGGER.info("Invalid request, singleResourceGroupDeploymentEnabled entitlement turned off for account {}", accountId);
                    return validationResultBuilder.error(
                            "You specified to use a single resource group for all of your resources, "
                                    + "but that feature is currently disabled").build();
                }
            default:
                return validationResultBuilder.build();
        }
    }
    //CHECKSTYLE:ON

    @Override
    public CloudPlatform getcloudPlatform() {
        return CloudPlatform.AZURE;
    }
}