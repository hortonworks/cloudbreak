package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.resource.ResourceService;

/**
 * FreeIPA counterpart to core's {@code AwsSecurityGroupValidationRequestProvider}. Collects the security-group
 * IDs to validate and resolves the AWS VPC ID from the environment. Kept as a Component so the action stays
 * unit-testable and doesn't need to pull in {@link ResourceService} and {@link CachedEnvironmentClientService} itself.
 */
@Component
public class PrepareUpgradeSecurityGroupValidationRequestProvider {

    @Inject
    private ResourceService resourceService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    /**
     * Merges SG IDs referenced from instance-group metadata with SG IDs we already persisted as
     * {@link ResourceType#AWS_SECURITY_GROUP} rows (status CREATED).
     */
    public Set<String> collectSecurityGroupIds(Stack stack) {
        Set<String> securityGroupIds = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            SecurityGroup securityGroup = instanceGroup.getSecurityGroup();
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
     * Returns the AWS VPC ID for the environment, or null when the environment carries no AWS network params. Null
     * causes the handler to skip the network-membership check (existence only).
     */
    public String resolveAwsVpcId(String environmentCrn) {
        DetailedEnvironmentResponse environment = cachedEnvironmentClientService.getByCrn(environmentCrn);
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getNetwork)
                .map(EnvironmentNetworkResponse::getAws)
                .map(EnvironmentNetworkAwsParams::getVpcId)
                .orElse(null);
    }
}
