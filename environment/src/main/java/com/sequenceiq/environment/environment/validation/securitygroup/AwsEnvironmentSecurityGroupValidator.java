package com.sequenceiq.environment.environment.validation.securitygroup;

import static com.sequenceiq.environment.CloudPlatform.AWS;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Component
public class AwsEnvironmentSecurityGroupValidator implements EnvironmentSecurityGroupValidator {

    private PlatformParameterService platformParameterService;

    public AwsEnvironmentSecurityGroupValidator(PlatformParameterService platformParameterService) {
        this.platformParameterService = platformParameterService;
    }

    @Override
    public void validate(EnvironmentCreationDto environmentDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        SecurityAccessDto securityAccessDto = environmentDto.getSecurityAccess();
        if (securityAccessDto != null) {
            if (onlyOneSecurityGroupIdDefined(securityAccessDto)) {
                resultBuilder.error(securityGroupIdsMustBePresented());
                return;
            } else if (isSecurityGroupIdDefined(securityAccessDto)) {
                if (!Strings.isNullOrEmpty(environmentDto.getNetwork().getNetworkCidr())) {
                    resultBuilder.error(networkIdMustBePresented(getCloudPlatform().name()));
                    return;
                }
                if (!Strings.isNullOrEmpty(securityAccessDto.getDefaultSecurityGroupId())) {
                    checkSecurityGroupVpc(environmentDto, resultBuilder, environmentDto.getSecurityAccess().getDefaultSecurityGroupId());
                }
                if (!Strings.isNullOrEmpty(securityAccessDto.getSecurityGroupIdForKnox())) {
                    checkSecurityGroupVpc(environmentDto, resultBuilder, environmentDto.getSecurityAccess().getSecurityGroupIdForKnox());
                }
            }
        }
    }

    private void checkSecurityGroupVpc(EnvironmentCreationDto environmentDto, ValidationResult.ValidationResultBuilder resultBuilder, String securityGroupId) {
        String region = environmentDto.getRegions().iterator().next();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                environmentDto.getAccountId(),
                environmentDto.getCredential().getCredentialName(),
                null,
                region,
                getCloudPlatform().name(),
                null);

        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(request);

        boolean securityGroupInVpc = false;
        for (CloudSecurityGroup cloudSecurityGroup : securityGroups.getCloudSecurityGroupsResponses().get(region)) {
            Object vpcId = cloudSecurityGroup.getProperties().get("vpcId");
            if (cloudSecurityGroup.getGroupId().equals(securityGroupId)) {
                if (vpcId != null && vpcId.toString().equals(environmentDto.getNetwork().getAws().getVpcId())) {
                    securityGroupInVpc = true;
                    break;
                }
            }
        }
        if (!securityGroupInVpc) {
            resultBuilder.error(securityGroupNotInTheSameVpc(securityGroupId));
            return;
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AWS;
    }

}
