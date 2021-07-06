package com.sequenceiq.environment.environment.validation.securitygroup.aws;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.util.SecurityGroupSeparator.getSecurityGroupIds;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.validation.securitygroup.EnvironmentSecurityGroupValidator;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
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
    public void validate(EnvironmentValidationDto environmentValidationDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        SecurityAccessDto securityAccessDto = environmentDto.getSecurityAccess();
        if (securityAccessDto != null) {
            if (onlyOneSecurityGroupIdDefined(securityAccessDto)) {
                LOGGER.error("Only one existing security group definied by the user: {}", securityAccessDto);
                resultBuilder.error(securityGroupIdsMustBePresent());
            } else if (isSecurityGroupIdDefined(securityAccessDto)) {
                LOGGER.info("Both existing security group defined: {}", securityAccessDto);
                NetworkDto networkDto = environmentDto.getNetwork();
                if (RegistrationType.CREATE_NEW == networkDto.getRegistrationType()) {
                    LOGGER.error("Both existing security group defined and user wants to create a new network with cidr: {}",
                            networkDto.getNetworkCidr());
                    resultBuilder.error(networkIdMustBePresent(getCloudPlatform().name()));
                    return;
                }
                if (!Strings.isNullOrEmpty(securityAccessDto.getDefaultSecurityGroupId())) {
                    LOGGER.info("Validate Security group {} that is related to {} network",
                            securityAccessDto.getDefaultSecurityGroupId(), networkDto.getAws());
                    checkSecurityGroupVpc(environmentDto, resultBuilder, environmentDto.getSecurityAccess().getDefaultSecurityGroupId());
                }
                if (!Strings.isNullOrEmpty(securityAccessDto.getSecurityGroupIdForKnox())) {
                    LOGGER.info("Validate Security group {} that is related to {} network",
                            securityAccessDto.getSecurityGroupIdForKnox(), networkDto.getAws());
                    checkSecurityGroupVpc(environmentDto, resultBuilder, environmentDto.getSecurityAccess().getSecurityGroupIdForKnox());
                }
            }
        }
    }

    private void checkSecurityGroupVpc(EnvironmentDto environmentDto, ValidationResult.ValidationResultBuilder resultBuilder, String securityGroupIds) {
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
        String awsVpcId = environmentDto.getNetwork().getAws().getVpcId();
        Map<String, Set<CloudSecurityGroup>> cloudSecurityGroupsResponses = securityGroups.getCloudSecurityGroupsResponses();
        if (Objects.nonNull(cloudSecurityGroupsResponses)) {
            Set<CloudSecurityGroup> cloudSecurityGroups = cloudSecurityGroupsResponses.get(region.getName());
            if (Objects.nonNull(cloudSecurityGroups)) {
                for (String securityGroupId : getSecurityGroupIds(securityGroupIds)) {
                    securityGroupInVpc = isSecurityGroupInVpc(
                            awsVpcId,
                            cloudSecurityGroups,
                            securityGroupId);
                    if (!securityGroupInVpc) {
                        break;
                    }
                }
            }
        }
        if (!securityGroupInVpc) {
            LOGGER.error("Security group {} does not belongs to the {} network.", securityGroupIds, environmentDto.getNetwork());
            resultBuilder.error(securityGroupNotInTheSameVpc(securityGroupIds));
            return;
        }
    }

    private boolean isSecurityGroupInVpc(String awsVpcId, Set<CloudSecurityGroup> cloudSecurityGroups, String securityGroupId) {
        boolean securityGroupInVpc = false;
        for (CloudSecurityGroup cloudSecurityGroup : cloudSecurityGroups) {
            Object vpcId = cloudSecurityGroup.getProperties().get("vpcId");
            String groupId = cloudSecurityGroup.getGroupId();
            if (!Strings.isNullOrEmpty(groupId) && groupId.equals(securityGroupId)) {
                LOGGER.info("Security group {} was found on AWS side.", securityGroupId);
                if (vpcId != null && vpcId.toString().equals(awsVpcId)) {
                    securityGroupInVpc = true;
                    break;
                }
            }
        }
        return securityGroupInVpc;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AWS;
    }

}
