package com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

/**
 * Collects the security-group IDs the validation flow should ask the provider about, plus the AWS VPC ID resolved
 * from the environment. Split out of the action so it stays unit-testable and doesn't force the action to depend on
 * both {@link ResourceService} and {@link EnvironmentService} directly.
 */
@Component
public class AwsSecurityGroupValidationRequestProvider {

    @Inject
    private ResourceService resourceService;

    @Inject
    private EnvironmentService environmentService;

    /**
     * Merges SG IDs referenced from instance-group metadata with SG IDs we already created and persisted as
     * {@link ResourceType#AWS_SECURITY_GROUP} rows (status CREATED). Instance-group SGs may reference customer-managed
     * groups; the resource rows cover the SGs Cloudbreak itself created. Both need to still exist before repair.
     */
    public Set<String> collectSecurityGroupIds(StackDtoDelegate stack) {
        Set<String> securityGroupIds = new HashSet<>();
        for (InstanceGroupDto instanceGroupDto : stack.getInstanceGroupDtos()) {
            SecurityGroup securityGroup = instanceGroupDto.getInstanceGroup().getSecurityGroup();
            if (securityGroup != null && securityGroup.getSecurityGroupIds() != null) {
                securityGroupIds.addAll(securityGroup.getSecurityGroupIds());
            }
        }
        resourceService.findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus.CREATED, ResourceType.AWS_SECURITY_GROUP, stack.getId())
                .stream()
                .map(Resource::getResourceReference)
                .filter(StringUtils::isNotBlank)
                .forEach(securityGroupIds::add);
        return securityGroupIds;
    }

    /**
     * Returns the AWS VPC ID for the environment, or null if the environment carries no AWS network params. Null
     * causes the handler to skip the network-membership check (existence only). Only AWS callers pass through here
     * today so a null return effectively means "environment misconfigured" and is worth surfacing separately.
     */
    public String resolveAwsVpcId(String environmentCrn) {
        DetailedEnvironmentResponse environment = environmentService.getByCrn(environmentCrn);
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getNetwork)
                .map(EnvironmentNetworkResponse::getAws)
                .map(EnvironmentNetworkAwsParams::getVpcId)
                .orElse(null);
    }
}
