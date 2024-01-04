package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDBPreparationFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDBPreparationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDBPreparationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDBPreparationTriggerRequest;
import com.sequenceiq.cloudbreak.view.StackView;

@Configuration
public class UpgradeEmbeddedDBPreparationActions {

    @Inject
    private UpgradeEmbeddedDBPreparationService upgradeRdsService;

    @Bean(name = "UPGRADE_EMBEDDED_DB_PREPARATION_STATE")
    public Action<?, ?> prepareEmbeddedDbUpgrade() {
        return new AbstractUpgradeEmbeddedDBPreparationAction<>(UpgradeEmbeddedDBPreparationTriggerRequest.class) {
            @Override
            protected void doExecute(UpgradeEmbeddedDBPreparationContext context, UpgradeEmbeddedDBPreparationTriggerRequest payload,
                Map<Object, Object> variables) {
                upgradeRdsService.prepareEmbeddedDbUpgrade(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeEmbeddedDBPreparationContext context) {
                return new UpgradeEmbeddedDBPreparationRequest(context.getStackId(), context.getVersion());
            }
        };
    }

    @Bean(name = "UPGRADE_EMBEDDED_DB_PREPARATION_FINISHED_STATE")
    public Action<?, ?> prepareEmbeddedDbUpgradeFinished() {
        return new AbstractUpgradeEmbeddedDBPreparationAction<>(UpgradeEmbeddedDBPreparationResult.class) {
            @Override
            protected void doExecute(UpgradeEmbeddedDBPreparationContext context, UpgradeEmbeddedDBPreparationResult payload, Map<Object, Object> variables) {
                upgradeRdsService.prepareEmbeddedDbUpgradeFinished(payload.getResourceId(), context.getClusterId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeEmbeddedDBPreparationContext context) {
                return new StackEvent(UpgradeEmbeddedDBPreparationEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "UPGRADE_EMBEDDED_DB_PREPARATION_FAILED_STATE")
    public Action<?, ?> prepareEmbeddedDbUpgradeFailed() {
        return new AbstractStackFailureAction<UpgradeEmbeddedDBPreparationState, UpgradeEmbeddedDBPreparationEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                UpgradeEmbeddedDBPreparationFailedEvent concretePayload = (UpgradeEmbeddedDBPreparationFailedEvent) payload;
                upgradeRdsService.prepareEmbeddedDatabaseUpgradeFailed(concretePayload.getResourceId(),
                        Optional.ofNullable(context.getStack()).map(StackView::getClusterId).orElse(null),
                        concretePayload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(UpgradeEmbeddedDBPreparationEvent.FAIL_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}
