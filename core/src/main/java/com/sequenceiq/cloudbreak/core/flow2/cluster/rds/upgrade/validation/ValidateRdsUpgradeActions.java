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
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradePushSaltStatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeTriggerRequest;
import com.sequenceiq.cloudbreak.view.StackView;

@Configuration
public class ValidateRdsUpgradeActions {

    private static final String TARGET_MAJOR_VERSION_KEY = "TARGET_MAJOR_VERSION";

    @Inject
    private ValidateRdsUpgradeService validateRdsUpgradeService;

    @Bean(name = "VALIDATE_RDS_UPGRADE_PUSH_SALT_STATES_STATE")
    public Action<?, ?> pushSaltStates() {
        return new AbstractValidateRdsUpgradeAction<>(ValidateRdsUpgradeTriggerRequest.class) {
            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, ValidateRdsUpgradeTriggerRequest payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                variables.put(TARGET_MAJOR_VERSION_KEY, payload.getVersion());
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
                ValidateRdsUpgradeOnCloudProviderRequest validateEvent = new ValidateRdsUpgradeOnCloudProviderRequest(context.getStackId(),
                        (TargetMajorVersion) variables.get(TARGET_MAJOR_VERSION_KEY));
                sendEvent(context, validateEvent.selector(), validateEvent);
            }
        };
    }

    @Bean(name = "VALIDATE_RDS_UPGRADE_FINISHED_STATE")
    public Action<?, ?> validateRdsUpgradeFinished() {
        return new AbstractValidateRdsUpgradeAction<>(ValidateRdsUpgradeOnCloudProviderResult.class) {
            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, ValidateRdsUpgradeOnCloudProviderResult payload, Map<Object, Object> variables) {
                validateRdsUpgradeService.validateRdsUpgradeFinished(payload.getResourceId(), context.getClusterId());
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