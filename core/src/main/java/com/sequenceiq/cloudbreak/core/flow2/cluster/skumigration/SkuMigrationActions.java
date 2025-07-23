package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ATTACH_PUBLIC_IPS_AND_ADD_LB;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CHECK_LOAD_BALANCERS_SKU;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DETACH_PUBLIC_IPS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.REMOVE_LOAD_BALACERS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.SKU_MIGRATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.SKU_MIGRATION_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.UPDATE_DNS_FOR_LB;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.SkuMigrationFinished;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.attach.AttachPublicIpsAddLBRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.attach.AttachPublicIpsAddLBResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.check.CheckSkuRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.check.CheckSkuResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.detachpublicips.DetachPublicIpsRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.detachpublicips.DetachPublicIpsResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.refreshdns.UpdateDnsRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.removeloadbalancer.RemoveLoadBalancerRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.removeloadbalancer.RemoveLoadBalancerResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.ProviderSyncState;

@Configuration
public class SkuMigrationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkuMigrationActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private SkuMigrationService skuMigrationService;

    @Inject
    private StackService stackService;

    @Bean(name = "SKU_MIGRATION_CHECK_SKU_STATE")
    public Action<?, ?> checkSkuAction() {
        return new AbstractSkuMigrationAction<>(SkuMigrationTriggerEvent.class) {

            @Override
            protected void doExecute(SkuMigrationContext context, SkuMigrationTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Checking Load Balancer SKU");
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.MIGRATING_SKU);
                flowMessageService.fireEventAndLog(payload.getResourceId(), Status.UPDATE_IN_PROGRESS.name(), CHECK_LOAD_BALANCERS_SKU);
                boolean skuMigrationNeededBySync = context.getProviderSyncStates().contains(ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED);
                boolean force = payload.isForce() || skuMigrationNeededBySync;
                LOGGER.debug("Provider sync states: {}, SKU migration needed by sync: {}, Force migration: {}",
                        context.getProviderSyncStates(), skuMigrationNeededBySync, force);
                CheckSkuRequest checkSkuRequest = new CheckSkuRequest(context.getStack(), context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudConnector(), context.getCloudStack(), force);
                sendEvent(context, checkSkuRequest);
            }
        };
    }

    @Bean(name = "SKU_MIGRATION_DETACH_PUBLIC_IPS_STATE")
    public Action<?, ?> detachPublicIpsAction() {
        return new AbstractSkuMigrationAction<>(CheckSkuResult.class) {

            @Override
            protected void doExecute(SkuMigrationContext context, CheckSkuResult payload, Map<Object, Object> variables) {
                LOGGER.info("Detach public IPs");
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.MIGRATING_SKU);
                flowMessageService.fireEventAndLog(payload.getResourceId(), Status.UPDATE_IN_PROGRESS.name(), DETACH_PUBLIC_IPS);
                DetachPublicIpsRequest detachPublicIpsRequest = new DetachPublicIpsRequest(context.getStack(), context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudConnector(), context.getCloudStack());
                sendEvent(context, detachPublicIpsRequest);
            }
        };
    }

    @Bean(name = "SKU_MIGRATION_REMOVE_LOAD_BALANCER_STATE")
    public Action<?, ?> removeLoadBalancerAction() {
        return new AbstractSkuMigrationAction<>(DetachPublicIpsResult.class) {

            @Override
            protected void doExecute(SkuMigrationContext context, DetachPublicIpsResult payload, Map<Object, Object> variables) {
                LOGGER.info("Remove load balancer");
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.MIGRATING_SKU);
                flowMessageService.fireEventAndLog(payload.getResourceId(), Status.UPDATE_IN_PROGRESS.name(), REMOVE_LOAD_BALACERS);
                RemoveLoadBalancerRequest removeLoadBalancerRequest = new RemoveLoadBalancerRequest(context.getStack(), context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudConnector(), context.getCloudStack());
                sendEvent(context, removeLoadBalancerRequest);
            }
        };
    }

    @Bean(name = "SKU_MIGRATION_ATTACH_PUBLIC_IPS_ADD_LB_STATE")
    public Action<?, ?> attachPublicIpsAddLbAction() {
        return new AbstractSkuMigrationAction<>(RemoveLoadBalancerResult.class) {

            @Override
            protected void doExecute(SkuMigrationContext context, RemoveLoadBalancerResult payload, Map<Object, Object> variables) {
                LOGGER.info("Attach public IPs and Add LB");
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.MIGRATING_SKU);
                flowMessageService.fireEventAndLog(payload.getResourceId(), Status.UPDATE_IN_PROGRESS.name(), ATTACH_PUBLIC_IPS_AND_ADD_LB);
                AttachPublicIpsAddLBRequest attachPublicIpsAddLBRequest = new AttachPublicIpsAddLBRequest(context.getStack(), context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudConnector(), context.getCloudStack());
                sendEvent(context, attachPublicIpsAddLBRequest);
            }
        };
    }

    @Bean(name = "SKU_MIGRATION_UPDATE_DNS_STATE")
    public Action<?, ?> refreshMetadataAndDnsAction() {
        return new AbstractSkuMigrationAction<>(AttachPublicIpsAddLBResult.class) {

            @Override
            protected void doExecute(SkuMigrationContext context, AttachPublicIpsAddLBResult payload, Map<Object, Object> variables) {
                LOGGER.info("Updating DNS Records for LB");
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.MIGRATING_SKU);
                flowMessageService.fireEventAndLog(payload.getResourceId(), Status.UPDATE_IN_PROGRESS.name(), UPDATE_DNS_FOR_LB);
                UpdateDnsRequest updateDnsRequest = new UpdateDnsRequest(context.getStack(), context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudConnector(), context.getCloudStack());
                sendEvent(context, updateDnsRequest);
            }
        };
    }

    @Bean(name = "SKU_MIGRATION_FINISHED_STATE")
    public Action<?, ?> skuMigrationFinishedAction() {
        return new AbstractSkuMigrationAction<>(SkuMigrationFinished.class) {

            @Override
            protected void doExecute(SkuMigrationContext context, SkuMigrationFinished payload, Map<Object, Object> variables) {
                LOGGER.info("Load Balancer Migration successfully completed from Basic to Standard SKU");
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.AVAILABLE);
                stackService.updateTemplateForStackToLatest(payload.getResourceId());
                Set<ProviderSyncState> providerSyncStates = context.getStack().getProviderSyncStates();
                LOGGER.info("Removing BASIC_SKU_MIGRATION_NEEDED from provider sync states for stack: {}", context.getStack().getName());
                providerSyncStates.remove(ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED);
                stackUpdater.updateProviderState(payload.getResourceId(), providerSyncStates);
                skuMigrationService.setSkuMigrationParameter(payload.getResourceId());
                flowMessageService.fireEventAndLog(payload.getResourceId(), Status.AVAILABLE.name(), SKU_MIGRATION_FINISHED);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SkuMigrationContext context) {
                return new StackEvent(SkuMigrationFlowEvent.SKU_MIGRATION_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "SKU_MIGRATION_FAILED_STATE")
    public Action<?, ?> skuMigrationFailedAction() {
        return new AbstractStackFailureAction<SkuMigrationFlowState, SkuMigrationFlowEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Migration failed to complete with the following errors: {}", payload.getException().getMessage(), payload.getException());
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.MIGRATING_SKU_FAILED);
                flowMessageService.fireEventAndLog(payload.getResourceId(), Status.UNREACHABLE.name(), SKU_MIGRATION_FAILED,
                        payload.getException().getMessage());
                sendEvent(context, SkuMigrationFlowEvent.SKU_MIGRATION_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

}
