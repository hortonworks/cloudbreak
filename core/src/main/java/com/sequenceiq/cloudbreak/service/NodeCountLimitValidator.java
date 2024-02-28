package com.sequenceiq.cloudbreak.service;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.conf.LimitConfiguration;
import com.sequenceiq.cloudbreak.conf.PrimaryGatewayRequirement;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class NodeCountLimitValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeCountLimitValidator.class);

    private static final String WEAK_PRIMARY_GATEWAY_INSTANCE_MESSAGE =
            "%s doesn't have enough cpu and memory resources to handle a cluster with %s nodes." +
                    " The current instance type %s has %s vCPU and %s GB memory, the recommended instance type is %s which has %s vCPU and %s GB memory." +
                    " In order to proceed please scale vertically the primary gateway and execute a repair on it to make the changes take effect.";

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private LimitConfiguration nodeCountLimitConfiguration;

    @Inject
    private Map<CloudPlatform, PricingCache> pricingCacheMap;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    public void validateScale(StackView stackView, Integer scalingAdjustment, String accountId) {
        if (scalingAdjustment > 0) {
            Integer currentNodeCount = instanceMetaDataService.countByStackId(stackView.getId()).getInstanceCount();
            Integer targetNodeCount = currentNodeCount + scalingAdjustment;
            validateNodeCount(targetNodeCount, accountId);
            nodeCountLimitConfiguration.getPrimaryGatewayRequirement(targetNodeCount).ifPresent(nodeRequirement -> {
                InstanceMetadataView primaryGateway = instanceMetaDataService.getPrimaryGatewayInstanceMetadataOrError(stackView.getId());
                InstanceGroup primaryGatewayGroup = instanceGroupService.getPrimaryGatewayInstanceGroupByStackId(stackView.getId());
                CloudPlatform cloudPlatform = CloudPlatform.fromName(stackView.getCloudPlatform());
                validatePrimaryGatewayInstanceType(nodeRequirement, stackView.getEnvironmentCrn(), cloudPlatform, stackView.getRegion(),
                        getInstanceType(primaryGatewayGroup), primaryGateway.getInstanceId(), targetNodeCount);
            });
        }
    }

    public void validateProvision(StackV4Request stackRequest, String region) {
        int targetNodeCount = stackRequest.getInstanceGroups().stream().mapToInt(InstanceGroupV4Base::getNodeCount).sum();
        validateNodeCount(targetNodeCount, Crn.safeFromString(stackRequest.getEnvironmentCrn()).getAccountId());
        nodeCountLimitConfiguration.getPrimaryGatewayRequirement(targetNodeCount).ifPresent(nodeRequirement -> {
            String instanceType = stackRequest.getInstanceGroups()
                    .stream()
                    .filter(group -> InstanceGroupType.isGateway(group.getType()))
                    .map(InstanceGroupV4Request::getTemplate)
                    .map(InstanceTemplateV4Base::getInstanceType)
                    .findFirst()
                    .orElse(null);
            validatePrimaryGatewayInstanceType(nodeRequirement, stackRequest.getEnvironmentCrn(), stackRequest.getCloudPlatform(), region, instanceType, null,
                    targetNodeCount);
        });
    }

    private void validateNodeCount(Integer targetNodeCount, String accountId) {
        Integer nodeCountLimit = nodeCountLimitConfiguration.getNodeCountLimit(Optional.ofNullable(accountId));
        if (targetNodeCount > nodeCountLimit) {
            throw new BadRequestException(String.format("The maximum count of nodes for this cluster cannot be higher than %s", nodeCountLimit));
        }
    }

    private void validatePrimaryGatewayInstanceType(PrimaryGatewayRequirement primaryGatewayRequirement, String environmentCrn, CloudPlatform cloudPlatform,
            String region, String instanceType, String instanceId, Integer targetNodeCount) {
        try {
            if (MapUtils.isNotEmpty(primaryGatewayRequirement.getRecommendedInstance()) &&
                    NullUtil.allNotNull(instanceType, cloudPlatform) &&
                    primaryGatewayRequirement.getRecommendedInstance().containsKey(cloudPlatform.name()) &&
                    pricingCacheMap.containsKey(cloudPlatform)) {
                checkPrimaryGatewayStrongEnough(environmentCrn, cloudPlatform, region, instanceType, instanceId, targetNodeCount, primaryGatewayRequirement);
            }
        } catch (Exception e) {
            if (e instanceof BadRequestException) {
                throw e;
            } else {
                LOGGER.warn("Couldn't check if primary gateway is strong enough to scale to {} node. Instance type is {}.", targetNodeCount, instanceType, e);
            }
        }
    }

    private void checkPrimaryGatewayStrongEnough(String environmentCrn, CloudPlatform cloudPlatform, String region, String instanceType, String instanceId,
            Integer targetNodeCount, PrimaryGatewayRequirement nodeRequirement) {
        PricingCache pricingCache = pricingCacheMap.get(cloudPlatform);
        Credential credential = credentialClientService.getByEnvironmentCrn(environmentCrn);
        ExtendedCloudCredential extendedCloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
        Optional<Integer> cpu = pricingCache.getCpuCountForInstanceType(region, instanceType, extendedCloudCredential);
        Optional<Integer> memory = pricingCache.getMemoryForInstanceType(region, instanceType, extendedCloudCredential);
        if (isLessThanRequired(nodeRequirement.getMinCpu(), cpu) || isLessThanRequired(nodeRequirement.getMinMemory(), memory)) {
            throw new BadRequestException(getWeakPrimaryGatewayMessage(instanceId, targetNodeCount, instanceType, cpu.orElse(null),
                    memory.orElse(null), nodeRequirement.getRecommendedInstance().get(cloudPlatform.name()), nodeRequirement.getMinCpu(),
                    nodeRequirement.getMinMemory()));
        }
    }

    private String getInstanceType(InstanceGroup instanceGroup) {
        return Optional.ofNullable(instanceGroup)
                .map(InstanceGroup::getTemplate)
                .map(Template::getInstanceType)
                .orElse(null);
    }

    private boolean isLessThanRequired(Integer minimumValue, Optional<Integer> currentValue) {
        if (currentValue.isEmpty()) {
            return false;
        } else {
            return minimumValue > currentValue.get();
        }
    }

    //CHECKSTYLE:OFF
    private String getWeakPrimaryGatewayMessage(String instanceId, Integer targetNodeCount, String currentInstanceType, Integer currentCpu,
            Integer currentMemory, String recommendedInstanceType, Integer requiredCpu, Integer requiredMemory) {
        //CHECKSTYLE:ON
        String prePart = instanceId == null ? "Primary gateway instance type" : String.format("Primary gateway instance '%s'", instanceId);
        return String.format(WEAK_PRIMARY_GATEWAY_INSTANCE_MESSAGE, prePart, targetNodeCount, currentInstanceType, orNotAvailable(currentCpu),
                orNotAvailable(currentMemory), recommendedInstanceType, requiredCpu, requiredMemory);
    }

    private String orNotAvailable(Integer value) {
        return value == null ? "N/A" : value.toString();
    }
}
