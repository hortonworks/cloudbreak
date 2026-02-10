package com.sequenceiq.environment.environment.flow.encryptionprofile.validator;

import static java.util.function.Predicate.not;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class EncryptionProfileValidator {
    private static final String MINIMUM_RUNTIME_VERSION = "7.3.2";

    @Inject
    private EntitlementService entitlementService;

    public void validate(List<StackViewV4Response> stackViewV4Responses) {
        if (!entitlementService.isConfigureEncryptionProfileEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            throw new CloudbreakServiceException("Account not entitled for encryption profile. Please contact your CDP administrator to enable it.");
        }

        VersionComparator versionComparator = new VersionComparator();
        List<String> stacksNotSupported = stackViewV4Responses
                .stream()
                .filter(stack -> versionComparator.compare(stack::getStackVersion, () -> MINIMUM_RUNTIME_VERSION) < 0)
                .map(StackViewV4Response::getName)
                .toList();
        if (!stacksNotSupported.isEmpty()) {
            throw new BadRequestException("All clusters runtime need to be 7.3.2 or above to enable encryption profile. Upgrade cluster(s): "
                    + String.join(",", stacksNotSupported));
        }

        List<String> stacksNotAvailable = stackViewV4Responses
                .stream()
                .filter(not(stack -> stack.getStatus().isAvailable()))
                .map(StackViewV4Response::getName)
                .toList();
        if (!stacksNotAvailable.isEmpty()) {
            throw new IllegalStateException("All clusters need to be available to enable encryption profile. Cluster(s) not available: "
                    + String.join(",", stacksNotAvailable));
        }
    }
}
