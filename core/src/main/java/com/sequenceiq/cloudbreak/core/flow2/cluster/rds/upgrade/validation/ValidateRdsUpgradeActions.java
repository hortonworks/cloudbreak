package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeBackupValidationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeBackupValidationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradePushSaltStatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradePushSaltStatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeTriggerRequest;
import com.sequenceiq.cloudbreak.view.StackView;

@Configuration
public class ValidateRdsUpgradeActions {

    @Inject
    private ValidateRdsUpgradeService validateRdsUpgradeService;

    @Bean(name = "VALIDATE_RDS_UPGRADE_PUSH_SALT_STATES_STATE")
    public Action<?, ?> pushSaltStates() {
        return new AbstractValidateRdsUpgradeAction<>(ValidateRdsUpgradeTriggerRequest.class) {
            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, ValidateRdsUpgradeTriggerRequest payload, Map<Object, Object> variables) {
                if (validateRdsUpgradeService.shouldRunDataBackupRestore(context)) {
                    validateRdsUpgradeService.pushSaltStates(payload.getResourceId());
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ValidateRdsUpgradeContext context) {
                return validateRdsUpgradeService.shouldRunDataBackupRestore(context) ?
                        new ValidateRdsUpgradePushSaltStatesRequest(context.getStackId()) :
                        new ValidateRdsUpgradePushSaltStatesResult(context.getStackId());
            }
        };
    }

    @Bean(name = "VALIDATE_RDS_UPGRADE_BACKUP_VALIDATION_STATE")
    public Action<?, ?> validateBackup() {
        return new AbstractValidateRdsUpgradeAction<>(ValidateRdsUpgradePushSaltStatesResult.class) {
            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, ValidateRdsUpgradePushSaltStatesResult payload, Map<Object, Object> variables) {
                if (validateRdsUpgradeService.shouldRunDataBackupRestore(context)) {
                    validateRdsUpgradeService.validateBackup(payload.getResourceId());
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ValidateRdsUpgradeContext context) {
                return validateRdsUpgradeService.shouldRunDataBackupRestore(context) ?
                        new ValidateRdsUpgradeBackupValidationRequest(context.getStackId()) :
                        new ValidateRdsUpgradeBackupValidationResult(context.getStackId());
            }
        };
    }

    @Bean(name = "VALIDATE_RDS_UPGRADE_FINISHED_STATE")
    public Action<?, ?> validateRdsUpgradeFinished() {
        return new AbstractValidateRdsUpgradeAction<>(ValidateRdsUpgradeBackupValidationResult.class) {
            @Override
            protected void doExecute(ValidateRdsUpgradeContext context, ValidateRdsUpgradeBackupValidationResult payload, Map<Object, Object> variables) {
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