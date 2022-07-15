package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPillarConfigUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPillarConfigUpdateResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPollingRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DnsUpdateFinished;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Configuration
public class ClusterStartActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartActions.class);

    @Inject
    private ClusterStartService clusterStartService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Bean(name = "CLUSTER_START_UPDATE_PILLAR_CONFIG_STATE")
    public Action<?, ?> startingClusterPillarConfigUpdate() {
        return new AbstractClusterAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) {
                sendEvent(context, new ClusterStartPillarConfigUpdateRequest(context.getStackId()));
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new ClusterStartRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "UPDATING_DNS_IN_PEM_STATE")
    public Action<?, ?> updateClusterDnsEntriesInPem() {
        return new AbstractClusterAction<>(ClusterStartPillarConfigUpdateResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterStartPillarConfigUpdateResult payload, Map<Object, Object> variables) {
                StackDto stack = stackDtoService.getById(payload.getResourceId());
                clusterStartService.updateDnsEntriesInPem(stack);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new DnsUpdateFinished(context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_STARTING_STATE")
    public Action<?, ?> startingCluster() {
        return new AbstractClusterAction<>(DnsUpdateFinished.class) {
            @Override
            protected void doExecute(ClusterViewContext context, DnsUpdateFinished payload, Map<Object, Object> variables) {
                InstanceMetadataView primaryGatewayInstanceMetadata = instanceMetaDataService.getPrimaryGatewayInstanceMetadata(context.getStackId())
                        .orElseThrow(NotFoundException.notFound("Primary gateway instance not found for the cluster"));
                clusterStartService.startingCluster(context.getStack(), context.getCluster(), primaryGatewayInstanceMetadata);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new ClusterStartRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_START_POLLING_STATE")
    public Action<?, ?> clusterStartPolling() {
        return new AbstractClusterAction<>(ClusterStartResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterStartResult payload, Map<Object, Object> variables) {
                sendEvent(context, new ClusterStartPollingRequest(context.getStackId(), payload.getRequestId()));
            }
        };
    }

    @Bean(name = "CLUSTER_START_FINISHED_STATE")
    public Action<?, ?> clusterStartFinished() {
        return new AbstractClusterAction<>(ClusterStartPollingResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterStartPollingResult payload, Map<Object, Object> variables) {
                clusterStartService.clusterStartFinished(context.getStack());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_START_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(ClusterStartEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_START_FAILED_STATE")
    public Action<?, ?> clusterStartFailedAction() {
        return new AbstractStackFailureAction<ClusterStartState, ClusterStartEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                clusterStartService.handleClusterStartFailure(context.getStack(), payload.getException().getMessage());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_START_FAILED, context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterStartEvent.FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}
