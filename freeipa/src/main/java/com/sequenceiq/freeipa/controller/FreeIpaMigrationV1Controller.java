package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.crn.AccountIdService;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.api.v1.freeipa.migration.FreeIpaMigrationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.migration.model.FreeIpaMultiAzMigrationV1Request;
import com.sequenceiq.freeipa.api.v1.freeipa.migration.model.FreeIpaMultiAzMigrationV1Response;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.migration.MultiAzMigrationService;
import com.sequenceiq.freeipa.service.migration.MultiAzMigrationValidationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Controller
public class FreeIpaMigrationV1Controller implements FreeIpaMigrationV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaMigrationV1Controller.class);

    @Inject
    private AccountIdService accountIdService;

    @Inject
    private StackService stackService;

    @Inject
    private MultiAzMigrationValidationService multiAzMigrationValidationService;

    @Inject
    private MultiAzMigrationService multiAzMigrationService;

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = AuthorizationResourceAction.MIGRATE_FREEIPA)
    public FreeIpaMultiAzMigrationV1Response migrateToMultiAz(@RequestObject FreeIpaMultiAzMigrationV1Request request) {
        String environmentCrn = request.getEnvironmentCrn();
        String accountId = accountIdService.getAccountIdFromResourceCrn(environmentCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId);

        ValidationResult validationResult = multiAzMigrationValidationService.validateMultiAzMigrationRequest(environmentCrn, accountId, stack);
        if (validationResult.hasError()) {
            LOGGER.error("Validation failed for multi-AZ migration request for environment {}: {}", environmentCrn, validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
        }

        return multiAzMigrationService.triggerMultiAzMigration(accountId, stack);
    }
}
