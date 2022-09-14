package com.sequenceiq.cloudbreak.service.stack;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class StackUpgradeService {

    @Inject
    private EntitlementService entitlementService;

    public String calculateUpgradeVariant(StackView stack, String userCrn) {
        String variant = stack.getPlatformVariant();
        String accountId = Crn.safeFromString(userCrn).getAccountId();
        boolean migrationEnable = entitlementService.awsVariantMigrationEnable(accountId);
        if (migrationEnable) {
            if (AwsConstants.AwsVariant.AWS_VARIANT.variant().value().equals(variant)) {
                variant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value();
            }
        }
        return variant;
    }

    public boolean awsVariantMigrationIsFeasible(StackView stackView, String triggeredVariant) {
        Crn crn = Crn.safeFromString(stackView.getResourceCrn());
        String originalPlatformVariant = stackView.getPlatformVariant();
        return AwsConstants.AwsVariant.AWS_VARIANT.variant().value().equals(originalPlatformVariant)
                && AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value().equals(triggeredVariant)
                && entitlementService.awsVariantMigrationEnable(crn.getAccountId());
    }

}
