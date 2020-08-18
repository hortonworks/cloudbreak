package com.sequenceiq.environment.environment.validation.securitygroup.aws;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.environment.validation.securitygroup.EnvironmentSecurityGroupValidator;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Component
public class AwsEnvironmentSecurityGroupValidator implements EnvironmentSecurityGroupValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEnvironmentSecurityGroupValidator.class);

    private PlatformParameterService platformParameterService;

    public AwsEnvironmentSecurityGroupValidator(PlatformParameterService platformParameterService) {
        this.platformParameterService = platformParameterService;
    }

    @Override
    public void validate(EnvironmentDto environmentDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        SecurityAccessDto securityAccessDto = environmentDto.getSecurityAccess();
        if (securityAccessDto != null) {
            if (onlyOneSecurityGroupIdDefined(securityAccessDto)) {
                LOGGER.error("Only one existing security group definied by the user: {}", securityAccessDto);
                resultBuilder.error(securityGroupIdsMustBePresent());
            } else if (isSecurityGroupIdDefined(securityAccessDto)) {
                LOGGER.info("Both existing security group defined: {}", securityAccessDto);
                if (RegistrationType.CREATE_NEW == environmentDto.getNetwork().getRegistrationType()) {
                    LOGGER.error("Both existing security group defined and user wants to create a new network with cidr: {}",
                            environmentDto.getNetwork().getNetworkCidr());
                    resultBuilder.error(networkIdMustBePresent(getCloudPlatform().name()));
                    return;
                }
                if (!Strings.isNullOrEmpty(securityAccessDto.getDefaultSecurityGroupId())) {
                    LOGGER.info("Validate Security group {} that is related to {} network",
                            securityAccessDto.getDefaultSecurityGroupId(), environmentDto.getNetwork().getAws());
                    checkSecurityGroupVpc(environmentDto, resultBuilder, environmentDto.getSecurityAccess().getDefaultSecurityGroupId());
                }
                if (!Strings.isNullOrEmpty(securityAccessDto.getSecurityGroupIdForKnox())) {
                    LOGGER.info("Validate Security group {} that is related to {} network",
                            securityAccessDto.getSecurityGroupIdForKnox(), environmentDto.getNetwork().getAws());
                    checkSecurityGroupVpc(environmentDto, resultBuilder, environmentDto.getSecurityAccess().getSecurityGroupIdForKnox());
                }
            }
        }
    }

    private void checkSecurityGroupVpc(EnvironmentDto environmentDto, ValidationResult.ValidationResultBuilder resultBuilder, String securityGroupId) {
        Region region = environmentDto.getRegions().iterator().next();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                environmentDto.getAccountId(),
                environmentDto.getCredential().getName(),
                null,
                region.getName(),
                getCloudPlatform().name(),
                null);

        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(request);

        boolean securityGroupInVpc = false;
        for (CloudSecurityGroup cloudSecurityGroup : securityGroups.getCloudSecurityGroupsResponses().get(region.getName())) {
            Object vpcId = cloudSecurityGroup.getProperties().get("vpcId");
            if (cloudSecurityGroup.getGroupId().equals(securityGroupId)) {
                LOGGER.info("Security group {} was found on AWS side.", securityGroupId);
                if (vpcId != null && vpcId.toString().equals(environmentDto.getNetwork().getAws().getVpcId())) {
                    securityGroupInVpc = true;
                    break;
                }
            }
        }
        if (!securityGroupInVpc) {
            LOGGER.error("Security group {} does not belongs to the {} network.", securityGroupId, environmentDto.getNetwork());
            resultBuilder.error(securityGroupNotInTheSameVpc(securityGroupId));
            return;
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AWS;
    }

}
