package com.sequenceiq.environment.environment.flow.encryptionprofile.validator;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Component
public class EncryptionProfileValidator {

    private static final String MINIMUM_RUNTIME_VERSION = "7.3.2";

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private EncryptionProfileClusterResolver encryptionProfileClusterResolver;

    public void validate(String environmentCrn) {
        if (!entitlementService.isConfigureEncryptionProfileEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            throw new ForbiddenException("Account not entitled for encryption profile. Please contact your CDP administrator to enable it.");
        }

        Optional<DescribeFreeIpaResponse> freeIpaResponseOpt = freeIpaService.describe(environmentCrn);
        if (freeIpaUsingCentos7(freeIpaResponseOpt)) {
            throw new BadRequestException("FreeIPA running on CentOS 7 which does not support to apply encryption profile.");
        }

        List<StackViewV4Response> stackViewV4Responses = stackService.getAllNotDeletedClustersByEnvironmentCrn(environmentCrn);

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

        List<String> datalakes = stackViewV4Responses
                .stream()
                .filter(encryptionProfileClusterResolver::isDatalake)
                .map(StackViewV4Response::getName)
                .toList();
        if (datalakes.size() > 1) {
            throw new BadRequestException("Encryption profile can be enabled only on environments with a single Data Lake. Data Lakes found: "
                    + String.join(",", datalakes));
        }

        List<String> stacksNotAvailable = stackViewV4Responses
                .stream()
                .filter(not(this::isAvailable))
                .filter(this::inheritsEnvironmentEncryptionProfile)
                .map(StackViewV4Response::getName)
                .toList();
        if (!stacksNotAvailable.isEmpty()) {
            throw new BadRequestException("All Data Lake and Data Hub clusters that inherit the environment encryption profile need to be available "
                    + "and not in maintenance mode to enable encryption profile. Cluster(s) not available: " + String.join(",", stacksNotAvailable));
        }
    }

    private boolean isAvailable(StackViewV4Response stack) {
        return Status.AVAILABLE.equals(stack.getStatus()) && Status.AVAILABLE.equals(stack.getCluster().getStatus());
    }

    private boolean inheritsEnvironmentEncryptionProfile(StackViewV4Response stack) {
        return !encryptionProfileClusterResolver.hasOwnEncryptionProfile(stack);
    }

    private boolean freeIpaUsingCentos7(Optional<DescribeFreeIpaResponse> freeIpaResponseOpt) {
        return freeIpaResponseOpt.isPresent()
                && freeIpaResponseOpt.get().getImage() != null
                && freeIpaResponseOpt.get().getImage().getOs().equals(OsType.CENTOS7.getOs());
    }
}
