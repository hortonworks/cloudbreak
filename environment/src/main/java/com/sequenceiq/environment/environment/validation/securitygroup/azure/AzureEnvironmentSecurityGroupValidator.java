package com.sequenceiq.environment.environment.validation.securitygroup.azure;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;
import com.sequenceiq.environment.environment.validation.securitygroup.EnvironmentSecurityGroupValidator;

@Component
public class AzureEnvironmentSecurityGroupValidator implements EnvironmentSecurityGroupValidator {

    private PlatformParameterService platformParameterService;

    public AzureEnvironmentSecurityGroupValidator(PlatformParameterService platformParameterService) {
        this.platformParameterService = platformParameterService;
    }

    @Override
    public void validate(EnvironmentDto environmentDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        SecurityAccessDto securityAccessDto = environmentDto.getSecurityAccess();
        if (securityAccessDto != null) {
            if (onlyOneSecurityGroupIdDefined(securityAccessDto)) {
                resultBuilder.error(securityGroupIdsMustBePresent());
            } else if (isSecurityGroupIdDefined(securityAccessDto)) {
                if (!Strings.isNullOrEmpty(securityAccessDto.getDefaultSecurityGroupId())) {
                    validateSecurityGroup(environmentDto, resultBuilder, environmentDto.getSecurityAccess().getDefaultSecurityGroupId());
                }
                if (!Strings.isNullOrEmpty(securityAccessDto.getSecurityGroupIdForKnox())) {
                    validateSecurityGroup(environmentDto, resultBuilder, environmentDto.getSecurityAccess().getSecurityGroupIdForKnox());
                }
            }
        }
    }

    private void validateSecurityGroup(EnvironmentDto environmentDto, ValidationResult.ValidationResultBuilder resultBuilder, String securityGroupId) {
        Region region = environmentDto.getRegions().iterator().next();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                environmentDto.getAccountId(),
                environmentDto.getCredential().getName(),
                null,
                region.getName(),
                getCloudPlatform().name(),
                null);

        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(request);

        boolean securityGroupFoundInRegion = false;
        if (Objects.nonNull(securityGroups.getCloudSecurityGroupsResponses())
                && Objects.nonNull(securityGroups.getCloudSecurityGroupsResponses().get(region.getName()))) {
            for (CloudSecurityGroup cloudSecurityGroup : securityGroups.getCloudSecurityGroupsResponses().get(region.getName())) {
                String groupId = cloudSecurityGroup.getGroupId();
                if (groupId.equalsIgnoreCase(securityGroupId)) {
                    securityGroupFoundInRegion = true;
                    break;
                }
            }
        }
        if (!securityGroupFoundInRegion) {
            resultBuilder.error(securityGroupNotInTheSameRegion(securityGroupId, region.getName()));
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AZURE;
    }

}
