package com.sequenceiq.cloudbreak.service.rotaterdscert;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SKIP_CERTIFICATE_ROTATION_VALIDATION;
import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.ENABLED;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.cloudbreak.api.model.RotateRdsCertResponseType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class StackRotateRdsCertificateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackRotateRdsCertificateService.class);

    @Inject
    private StackDtoService stackService;

    @Inject
    private StackCommonService stackCommonService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private BlueprintService blueprintService;

    public StackRotateRdsCertificateV4Response rotateRdsCertificate(@NotNull NameOrCrn nameOrCrn, String accountId) {
        StackView stack = stackService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        validateThatRotationIsTriggerable(accountId, stack);
        return getStackRotateRdsCertificateV4Response(stack);
    }

    public void validateThatRotationIsTriggerable(String accountId, String crn) {
        StackView stack = stackService.getStackViewByCrn(crn);
        validateThatRotationIsTriggerable(accountId, stack);
    }

    private void validateThatRotationIsTriggerable(String accountId, StackView stack) {
        boolean rollingRotationTriggerable = rollingUpgradeEnabledBlueprint(stack)
                || accountEntitledForSkippingValidation(accountId);
        if (!rollingRotationTriggerable) {
            String msg = String.format("The cluster is not supporting rolling restart of services and you are not entitled to use RDS SSL certificate " +
                    "rotation without rolling restart. Please contact Cloudera to enable '%s' entitlement for your account",
                    CDP_SKIP_CERTIFICATE_ROTATION_VALIDATION);
            LOGGER.info(msg);
            throw new BadRequestException(msg);
        }
    }

    private boolean accountEntitledForSkippingValidation(String accountId) {
        LOGGER.debug("Checking that account '{}' is entitled for skipping RDS certificate rotation without rolling service restart", accountId);
        return entitlementService.cdpSkipRdsSslCertificateRollingRotationValidation(accountId);
    }

    private boolean rollingUpgradeEnabledBlueprint(StackView stack) {
        boolean result = false;
        Optional<Blueprint> blueprintOpt = blueprintService.getByClusterId(stack.getClusterId());
        if (blueprintOpt.isPresent()) {
            BlueprintUpgradeOption blueprintUpgradeOption = Optional.ofNullable(blueprintOpt.get().getBlueprintUpgradeOption()).orElse(ENABLED);
            LOGGER.debug("Checking blueprint's upgrade option for rolling upgrade/service-restart support: {}", blueprintUpgradeOption);
            result = blueprintUpgradeOption.isRollingUpgradeEnabled();
        }
        return result;
    }

    private StackRotateRdsCertificateV4Response getStackRotateRdsCertificateV4Response(StackView stack) {
        return new StackRotateRdsCertificateV4Response(RotateRdsCertResponseType.TRIGGERED,
                stackCommonService.rotateRdsCertificate(stack), null, stack.getResourceCrn());
    }

}
