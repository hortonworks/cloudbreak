package com.sequenceiq.cloudbreak.service.multiaz;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class ProviderBasedMultiAzSetupValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderBasedMultiAzSetupValidator.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private StackService stackService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private EntitlementService entitlementService;

    public void validate(ValidationResultBuilder validationBuilder, Stack stack) {
        updateMultiAzFlagOnStackIfNecessary(stack, validationBuilder);
        if (stack.isMultiAz()) {
            AvailabilityZoneConnector availabilityZoneConnector = getAvailabilityZoneConnector(stack);
            if (availabilityZoneConnector != null && hasMultiAzEntitlementForPlatform(validationBuilder, stack)) {
                validateWithAvailabilityZoneConnector(validationBuilder, stack, availabilityZoneConnector);
            } else {
                LOGGER.debug("Implementation for AvailabilityZoneConnector is not present for CloudPlatform {} and PlatformVariant {} or the account is not "
                        + "entitled for multi-Az functionality on the platform", stack.getCloudPlatform(), stack.getPlatformVariant());
            }
        } else {
            LOGGER.debug("Multi-AZ flag is disabled on the Stack, no need to validate group level zones.");
        }
    }

    private void updateMultiAzFlagOnStackIfNecessary(Stack stack, ValidationResultBuilder validationBuilder) {
        boolean atLeastOneZoneDefinedForAllTheGroups = stack.getInstanceGroups().stream()
                .allMatch(group -> CollectionUtils.isNotEmpty(group.getAvailabilityZones()));
        boolean anyZoneConfiguredOnGroupLevel = stack.getInstanceGroups().stream().anyMatch(group -> CollectionUtils.isNotEmpty(group.getAvailabilityZones()));
        if (!stack.isMultiAz()) {
            if (atLeastOneZoneDefinedForAllTheGroups) {
                LOGGER.debug("Enabling the multi-AZ flag on the stack, because the instance group level network settings indicate that.");
                stackService.updateMultiAzFlag(stack.getId(), Boolean.TRUE);
                stack.setMultiAz(Boolean.TRUE);
            } else if (anyZoneConfiguredOnGroupLevel) {
                String msg = "The multi-AZ flag was not enabled, but zones were provided on some the groups of the deployment. " +
                        "Please use the multi-AZ flag or set explicit zone(s) for all the groups of the deployment!";
                LOGGER.info(msg);
                validationBuilder.error(msg);
            }
        }
    }

    public AvailabilityZoneConnector getAvailabilityZoneConnector(StackView stack) {
        LOGGER.debug("CloudPlatform is {} PlatformVariant is {}", stack.getCloudPlatform(), stack.getPlatformVariant());
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(stack.getCloudPlatform()),
                Variant.variant(stack.getPlatformVariant()));
        return cloudPlatformConnectors.get(cloudPlatformVariant).availabilityZoneConnector();
    }

    private boolean hasMultiAzEntitlementForPlatform(ValidationResultBuilder validationBuilder, Stack stack) {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        if (AZURE.equals(CloudPlatform.valueOf(stack.cloudPlatform())) && !entitlementService.isAzureMultiAzEnabled(accountId)) {
            String errorMsg = String.format("Provisioning a multi AZ cluster on Azure requires entitlement %s.", Entitlement.CDP_CB_AZURE_MULTIAZ.name());
            LOGGER.info(errorMsg);
            validationBuilder.error(errorMsg);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private void validateWithAvailabilityZoneConnector(ValidationResultBuilder validationBuilder, Stack stack, AvailabilityZoneConnector azConnector) {
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
        Credential credential = credentialConverter.convert(environment.getCredential());
        ExtendedCloudCredential cloudCredential = extendedCloudCredentialConverter.convert(credential);
        Set<String> environmentZones = environment.getNetwork()
                .getAvailabilityZones(CloudPlatform.valueOf(stack.getCloudPlatform()));
        if (CollectionUtils.isEmpty(environmentZones) && stack.isMultiAz()) {
            String msg = "No availability zone configured on the environment, multi/targeted availability zone could not be requested.";
            LOGGER.info(msg);
            validationBuilder.error(msg);
        } else {
            Region region = Region.region(environment.getLocation().getName());
            validateInstanceGroups(validationBuilder, stack, azConnector, cloudCredential, environmentZones, region);
        }
    }

    private void validateInstanceGroups(ValidationResultBuilder validationBuilder, Stack stack, AvailabilityZoneConnector azConnector,
            ExtendedCloudCredential cloudCredential, Set<String> environmentZones, Region region) {
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            String groupName = instanceGroup.getGroupName();
            String instanceType = instanceGroup.getTemplate().getInstanceType();
            Set<String> zonesToCheckOnProviderSide = getZonesToCheckWithEnvironmentValidation(validationBuilder, environmentZones, instanceGroup);
            if (stack.isMultiAz() && CollectionUtils.isNotEmpty(zonesToCheckOnProviderSide)) {
                LOGGER.debug("Validating zones('{}') on provider side for group: '{}' and instance type: '{}'",
                        String.join(",", zonesToCheckOnProviderSide), groupName, instanceType);
                Set<String> availabilityZones = azConnector.getAvailabilityZones(cloudCredential, zonesToCheckOnProviderSide, instanceType, region);
                LOGGER.info("Availability zones for instance group '{}' with instance type: '{}' in region '{}' for environment are: '{}'", groupName,
                        instanceType, region.getRegionName(), availabilityZones);
                if (CollectionUtils.isEmpty(availabilityZones)) {
                    LOGGER.warn("There are no availability zones configured");
                    validationBuilder.error(String.format("The %s region does not support Multi AZ configuration. " +
                                    "Please check https://learn.microsoft.com/en-us/azure/reliability/availability-zones-service-support for more details. " +
                                    "It is also possible that the given %s instances on %s group are not supported in any specified %s zones.",
                            region.getRegionName(),
                            instanceType,
                            groupName,
                            environmentZones.stream().sorted().collect(Collectors.toList())));
                }
            } else {
                LOGGER.debug("Validation is disabled because either the stack is not multi-AZ: '{}' or the zones set to check is empty: '{}'",
                        stack.isMultiAz(), CollectionUtils.isEmpty(zonesToCheckOnProviderSide));
            }
        }
    }

    private Set<String> getZonesToCheckWithEnvironmentValidation(ValidationResultBuilder validationBuilder, Set<String> environmentZones,
            InstanceGroup instanceGroup) {
        Set<String> zonesToCheckOnProviderSide;
        Set<String> zonesConfiguredOnGroup = instanceGroup.getAvailabilityZones();
        boolean allTheConfiguredZonesExistOnEnvironmentLevel = environmentZones.containsAll(zonesConfiguredOnGroup);
        if (CollectionUtils.isNotEmpty(zonesConfiguredOnGroup)) {
            if (allTheConfiguredZonesExistOnEnvironmentLevel) {
                zonesToCheckOnProviderSide = zonesConfiguredOnGroup;
            } else {
                String invalidZones = zonesConfiguredOnGroup.stream().filter(Predicate.not(environmentZones::contains)).collect(Collectors.joining(","));
                String msg = String.format("These zones '%s' are requested for group '%s' but not available on Environment level('%s')", invalidZones,
                        instanceGroup.getGroupName(), String.join(",", environmentZones));
                LOGGER.warn(msg);
                validationBuilder.error(msg);
                zonesToCheckOnProviderSide = null;
            }
        } else {
            zonesToCheckOnProviderSide = environmentZones;
            instanceGroupService.saveEnvironmentAvailabilityZones(instanceGroup, environmentZones);
        }
        return zonesToCheckOnProviderSide;
    }

}
