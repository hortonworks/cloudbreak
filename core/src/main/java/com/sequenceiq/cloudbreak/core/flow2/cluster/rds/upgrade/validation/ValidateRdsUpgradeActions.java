package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeBackupValidationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeBackupValidationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeCleanupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeCleanupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeConnectionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeConnectionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradePushSaltStatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeCleanupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeCleanupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeOnCloudProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeOnCloudProviderResult;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.cloudbreak.view.StackView;

@Configuration
public class ValidateRdsUpgradeActions {

    private static final String VALIDATE_ON_PROVIDER_WARNING_MESSAGE = "VALIDATE_ON_PROVIDER_WARNING_MESSAGE";

    private static final String VALIDATE_CONNECTION_ERROR_MESSAGE = "VALIDATE_CONNECTION_ERROR_MESSAGE";

    @Inject
    private ValidateRdsUpgradeService validateRdsUpgradeService;

    @Bean(name = "VALIDATE_RDS_UPGRADE_PUSH_SALT_STATES_STATE")
    public Action<?, ?> pushSaltStates() {
        return new AbstractValidateRdsUpgradeAction<>(ValidateRdsUpgradeTriggerRequest.class) {

            @Override
            protected void prepareExecution(ValidateRdsUpgradeTriggerRequest payload, Map<Object, Object> variables) {
                super.prepareExecution(payload, variables);
                variables.put(TARGET_MAJOR_VERSION_KEY, payload.getVersion());
            }

            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, ValidateRdsUpgradeTriggerRequest payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                validateRdsUpgradeService.rdsUpgradeStarted(stackId, payload.getVersion());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ValidateRdsUpgradeContext context) {
                return new ValidateRdsUpgradePushSaltStatesResult(context.getStackId());
            }
        };
    }

    @Bean(name = "VALIDATE_RDS_UPGRADE_BACKUP_VALIDATION_STATE")
    public Action<?, ?> validateBackup() {
        return new AbstractValidateRdsUpgradeAction<>(ValidateRdsUpgradePushSaltStatesResult.class) {
            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, ValidateRdsUpgradePushSaltStatesResult payload, Map<Object, Object> variables) {
                if (validateRdsUpgradeService.shouldRunDataBackupRestore(context.getStack(), context.getCluster(), context.getDatabase())) {
                    validateRdsUpgradeService.validateBackup(payload.getResourceId());
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ValidateRdsUpgradeContext context) {
                return validateRdsUpgradeService.shouldRunDataBackupRestore(context.getStack(), context.getCluster(), context.getDatabase()) ?
                        new ValidateRdsUpgradeBackupValidationRequest(context.getStackId()) :
                        new ValidateRdsUpgradeBackupValidationResult(context.getStackId());
            }
        };
    }

    @Bean(name = "VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_STATE")
    public Action<?, ?> validateUpgradeOnCloudProvider() {
        return new AbstractValidateRdsUpgradeAction<>(ValidateRdsUpgradeBackupValidationResult.class) {
            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, ValidateRdsUpgradeBackupValidationResult payload, Map<Object, Object> variables) {
                validateRdsUpgradeService.validateOnCloudProvider(payload.getResourceId());
                ValidateRdsUpgradeOnCloudProviderRequest validateRequest = new ValidateRdsUpgradeOnCloudProviderRequest(context.getStackId(),
                        (TargetMajorVersion) variables.get(TARGET_MAJOR_VERSION_KEY));
                sendEvent(context, validateRequest.selector(), validateRequest);
            }
        };
    }

    @Bean(name = "WAIT_FOR_VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_STATE")
    public Action<?, ?> waitForValidateUpgradeOnCloudProvider() {
        return new AbstractValidateRdsUpgradeAction<>(ValidateRdsUpgradeOnCloudProviderResult.class) {
            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, ValidateRdsUpgradeOnCloudProviderResult payload, Map<Object, Object> variables) {
                WaitForValidateRdsUpgradeOnCloudProviderRequest validateEvent = new WaitForValidateRdsUpgradeOnCloudProviderRequest(context.getStackId(),
                        payload.getFlowIdentifier(), payload.getCanaryProperties());
                sendEvent(context, validateEvent.selector(), validateEvent);
            }
        };
    }

    @Bean(name = "VALIDATE_RDS_UPGRADE_CONNECTION_STATE")
    public Action<?, ?> validateConnection() {
        return new AbstractValidateRdsUpgradeAction<>(WaitForValidateRdsUpgradeOnCloudProviderResult.class) {

            @Override
            protected void prepareExecution(WaitForValidateRdsUpgradeOnCloudProviderResult payload, Map<Object, Object> variables) {
                super.prepareExecution(payload, variables);
                variables.put(CANARY_RDS_PROPERTIES_KEY, payload.getCanaryProperties());
                NullUtil.doIfNotNull(payload.getReason(), reason -> variables.put(VALIDATE_ON_PROVIDER_WARNING_MESSAGE, reason));
            }

            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, WaitForValidateRdsUpgradeOnCloudProviderResult payload, Map<Object, Object> variables) {
                if (validateRdsUpgradeService.shouldRunDataBackupRestore(context.getStack(), context.getCluster(), context.getDatabase())) {
                    validateRdsUpgradeService.validateConnection(payload.getResourceId());
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ValidateRdsUpgradeContext context) {
                return validateRdsUpgradeService.shouldRunDataBackupRestore(context.getStack(), context.getCluster(), context.getDatabase()) ?
                        new ValidateRdsUpgradeConnectionRequest(context.getStackId(), context.getCanaryProperties()) :
                        new ValidateRdsUpgradeConnectionResult(context.getStackId(), null);
            }
        };
    }

    @Bean(name = "VALIDATE_RDS_UPGRADE_CLEANUP_STATE")
    public Action<?, ?> cleanupValidateResources() {
        return new AbstractValidateRdsUpgradeAction<>(ValidateRdsUpgradeConnectionResult.class) {

            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, ValidateRdsUpgradeConnectionResult payload, Map<Object, Object> variables) {
                NullUtil.doIfNotNull(payload.getReason(), reason -> variables.put(VALIDATE_CONNECTION_ERROR_MESSAGE, reason));
                if (validateRdsUpgradeService.shouldRunDataBackupRestore(context.getStack(), context.getCluster(), context.getDatabase())) {
                    validateRdsUpgradeService.validateCleanup(payload.getResourceId());
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ValidateRdsUpgradeContext context) {
                return validateRdsUpgradeService.shouldRunDataBackupRestore(context.getStack(), context.getCluster(), context.getDatabase()) ?
                        new ValidateRdsUpgradeCleanupRequest(context.getStackId()) :
                        new ValidateRdsUpgradeCleanupResult(context.getStackId(), null);
            }
        };
    }

    @Bean(name = "WAIT_FOR_VALIDATE_RDS_UPGRADE_CLEANUP_STATE")
    public Action<?, ?> waitForCleanupValidateResources() {
        return new AbstractValidateRdsUpgradeAction<>(ValidateRdsUpgradeCleanupResult.class) {

            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, ValidateRdsUpgradeCleanupResult payload, Map<Object, Object> variables) {
                String connectionErrorMessage = (String) variables.getOrDefault(VALIDATE_CONNECTION_ERROR_MESSAGE, "");
                WaitForValidateRdsUpgradeCleanupRequest validateEvent = new WaitForValidateRdsUpgradeCleanupRequest(context.getStackId(),
                        connectionErrorMessage, payload.getFlowIdentifier());
                sendEvent(context, validateEvent.selector(), validateEvent);
            }
        };
    }

    @Bean(name = "VALIDATE_RDS_UPGRADE_FINISHED_STATE")
    public Action<?, ?> validateRdsUpgradeFinished() {
        return new AbstractValidateRdsUpgradeAction<>(WaitForValidateRdsUpgradeCleanupResult.class) {

            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, WaitForValidateRdsUpgradeCleanupResult payload, Map<Object, Object> variables) {
                String validationMessage = (String) variables.getOrDefault(VALIDATE_ON_PROVIDER_WARNING_MESSAGE, "");
                validateRdsUpgradeService.validateRdsUpgradeFinished(payload.getResourceId(), context.getClusterId(), validationMessage);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ValidateRdsUpgradeContext context) {
                return new StackEvent(ValidateRdsUpgradeEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "VALIDATE_RDS_UPGRADE_FAILED_STATE")
    public Action<?, ?> validateRdsUpgradeFailed() {
        return new AbstractStackFailureAction<ValidateRdsUpgradeState, ValidateRdsUpgradeEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                ValidateRdsUpgradeFailedEvent concretePayload = (ValidateRdsUpgradeFailedEvent) payload;
                validateRdsUpgradeService.validateRdsUpgradeFailed(concretePayload.getResourceId(),
                        Optional.ofNullable(context.getStack()).map(StackView::getClusterId).orElse(null),
                        concretePayload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ValidateRdsUpgradeEvent.FAIL_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}