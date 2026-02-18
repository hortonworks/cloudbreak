package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_VARIANT;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_VARIANT;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class StackUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpgradeService.class);

    @Inject
    private EntitlementService entitlementService;

    public String calculateUpgradeVariant(StackView stack, String userCrn, boolean keepVariant) {
        String variant = stack.getPlatformVariant();
        LOGGER.debug("About to calculate variant (current is: {})", variant);
        if (keepVariant) {
            LOGGER.debug("Keeping the original variant is requested, therefore the following one is going to be returned: {}", variant);
        } else {
            String accountId = Crn.safeFromString(userCrn).getAccountId();
            if (AWS_VARIANT.variant().value().equals(variant) && entitlementService.awsVariantMigrationEnabled(accountId)) {
                LOGGER.debug("Variant migration is enabled and the {} variant is detected, therefore the following one is going to return " +
                        "to change the original one {}", AWS_VARIANT.variant().value(), AWS_NATIVE_VARIANT.variant().value());
                variant = AWS_NATIVE_VARIANT.variant().value();
            }
        }
        LOGGER.debug("The following variant is going to be returned: {}", variant);
        return variant;
    }

    public boolean awsVariantMigrationIsFeasible(StackView stackView, String triggeredVariant) {
        Crn crn = Crn.safeFromString(stackView.getResourceCrn());
        String originalPlatformVariant = stackView.getPlatformVariant();
        return AWS_VARIANT.variant().value().equals(originalPlatformVariant)
                && AWS_NATIVE_VARIANT.variant().value().equals(triggeredVariant)
                && entitlementService.awsVariantMigrationEnabled(crn.getAccountId());
    }

    public String calculateUpgradeVariant(StackDto stack, String userCrn, boolean keepVariant,
            Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart) {
        String variant = null;
        if (repairStart.isSuccess()) {
            Set<String> discoveryFqdnsToRepair = repairStart.getSuccess().entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream())
                    .filter(im -> StringUtils.isNotEmpty(im.getDiscoveryFQDN()))
                    .map(InstanceMetadataView::getDiscoveryFQDN)
                    .collect(Collectors.toSet());
            if (allNodesSelectedForRepair(stack, discoveryFqdnsToRepair)) {
                LOGGER.info("All the not terminated instances have been requested in repair. Triggering AWS_NATIVE migration if feasible.");
                variant = calculateUpgradeVariant(stack.getStack(), userCrn, keepVariant);
            }
        }
        return variant;
    }

    public boolean allNodesSelectedForRepair(StackDto stack, Set<String> discoveryFqdnsToRepair) {
        Set<String> allNotTerminatedInstanceDiscoveryFqdn = stack.getAllNotTerminatedInstanceMetaData().stream()
                .filter(im -> StringUtils.isNotEmpty(im.getDiscoveryFQDN()))
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toSet());
        return CollectionUtils.isNotEmpty(discoveryFqdnsToRepair)
                && discoveryFqdnsToRepair.containsAll(allNotTerminatedInstanceDiscoveryFqdn);
    }

}
