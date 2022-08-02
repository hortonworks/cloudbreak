package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.AbstractUpgradeEmbeddedDbEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDbPreparationFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDbPrepareRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDbPrepareResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDbPrepareTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStopServicesRequest;

@Configuration
public class UpgradeEmbeddedDBPreparationActions {

    @Inject
    private UpgradeEmbeddedDBPreparationService upgradeRdsService;

    @Bean(name = "UPGRADE_EMBEDDED_DB_PREPARATION_STATE")
    public Action<?, ?> prepareEmbeddedDbUpgrade() {
        return new AbstractEmbeddedDbUpgradePreparationAction<>(UpgradeEmbeddedDbPrepareTriggerRequest.class) {
            @Override
            protected void doExecute(UpgradeEmbeddedDbPreparationContext context, UpgradeEmbeddedDbPrepareTriggerRequest payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeEmbeddedDbPreparationContext context) {
                return new UpgradeEmbeddedDbPrepareRequest(context.getStackId(), context.getVersion());
            }
        };
    }

    @Bean(name = "UPGRADE_EMBEDDED_DB_PREPARATION_FINISHED_STATE")
    public Action<?, ?> upgradeRdsFinished() {
        return new AbstractEmbeddedDbUpgradePreparationAction<>(UpgradeEmbeddedDbPrepareResult.class) {
            @Override
            protected void doExecute(UpgradeEmbeddedDbPreparationContext context, UpgradeEmbeddedDbPrepareResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeEmbeddedDbPreparationContext context) {
                return new StackEvent(UpgradeEmbeddedDBPreparationEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "UPGRADE_EMBEDDED_DB_PREPARATION_FAILED_STATE")
    public Action<?, ?> upgradeRdsFailed() {
        return new AbstractStackFailureAction<UpgradeEmbeddedDBPreparationState, UpgradeEmbeddedDBPreparationEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                UpgradeEmbeddedDbPreparationFailedEvent concretePayload = (UpgradeEmbeddedDbPreparationFailedEvent) payload;
                upgradeRdsService.rdsUpgradeFailed(concretePayload.getResourceId(),
                        Optional.ofNullable(context.getStackView().getClusterView()).map(ClusterView::getId).orElse(null),
                        concretePayload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(UpgradeEmbeddedDBPreparationEvent.FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
