package com.sequenceiq.cloudbreak.reactor.handler.rollingvs;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_BROKER;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_ENTEREDCMMAINTMODE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_ENTERINGCMMAINTMODE;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleDecommissionInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleDecommissionInstancesResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RollingVerticalScaleDecommissionInstancesHandler extends ExceptionCatcherEventHandler<RollingVerticalScaleDecommissionInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingVerticalScaleDecommissionInstancesHandler.class);

    private static final long POLL_FOR_10_MINUTES = TimeUnit.MINUTES.toSeconds(10);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackService stackService;

    @Inject
    private RollingVerticalScaleService rollingVerticalScaleService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RollingVerticalScaleDecommissionInstancesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RollingVerticalScaleDecommissionInstancesRequest> event) {
        return new StackFailureEvent(RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_FAILURE_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RollingVerticalScaleDecommissionInstancesRequest> event) {
        LOGGER.info("RollingVerticalScaleDecommissionInstancesHandler: {}", event.getData().getResourceId());
        RollingVerticalScaleDecommissionInstancesRequest request = event.getData();
        RollingVerticalScaleResult rollingVerticalScaleResult = request.getRollingVerticalScaleResult();
        try {
            Stack stack = stackService.getByIdWithLists(request.getResourceId());
            CmTemplateProcessor processor = cmTemplateProcessorFactory.get(stack.getBlueprintJsonText());
            Set<String> nonGatewayComponents = processor.getNonGatewayComponentsByHostGroup()
                    .getOrDefault(request.getHostGroupName(), Set.of());
            if (!nonGatewayComponents.contains(KAFKA_BROKER) && !request.getHostsNameToDecommission().isEmpty()) {
                ClusterDecomissionService clusterDecomissionService = clusterApiConnectors.getConnector(stack).clusterDecomissionService();
                Set<String> hostNames = request.getHostsNameToDecommission();
                rollingVerticalScaleService.decommissionInstances(request.getResourceId(), hostNames, request.getHostGroupName());
                LOGGER.info("Attempting to decommission hosts. count={}, hostnames={}", hostNames.size(),
                        hostNames);
                Map<String, InstanceMetadataView> hostsToRemove = clusterDecomissionService.collectHostsToRemove(request.getHostGroupName(),
                        hostNames);

                Set<String> decommissionedHostNames = clusterDecomissionService.decommissionClusterNodesStopStart(hostsToRemove, POLL_FOR_10_MINUTES);
                List<String> decommissionedInstanceIds = hostsToRemove.entrySet()
                        .stream()
                        .filter(i -> decommissionedHostNames.contains(i.getKey()))
                        .map(i -> i.getValue().getInstanceId())
                        .toList();
                rollingVerticalScaleService.finishDecommissionInstances(request.getResourceId(),
                        decommissionedInstanceIds, request.getHostGroupName());
                Set<String> failedToDecommissionInstances = hostNames
                        .stream()
                        .filter(i -> !decommissionedHostNames.contains(i))
                        .collect(Collectors.toSet());
                rollingVerticalScaleService.failedToDecommissionInstances(request.getResourceId(),
                        failedToDecommissionInstances, request.getHostGroupName(), "Node did not report as decommissioned within the poll window");
                if (!decommissionedHostNames.isEmpty()) {
                    LOGGER.debug("Attempting to put decommissioned hosts into maintenance mode. count={}", decommissionedHostNames.size());
                    flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_VERTICALSCALE_ENTERINGCMMAINTMODE,
                            String.valueOf(decommissionedHostNames.size()));

                    clusterDecomissionService.enterMaintenanceMode(decommissionedHostNames);

                    flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_VERTICALSCALE_ENTEREDCMMAINTMODE,
                            String.valueOf(decommissionedHostNames.size()));
                    LOGGER.debug("Successfully put decommissioned hosts into maintenance mode. count={}", decommissionedHostNames.size());
                } else {
                    LOGGER.debug("No nodes decommissioned, hence no nodes being put into maintenance mode");
                }
            } else if (nonGatewayComponents.contains(KAFKA_BROKER)) {
                rollingVerticalScaleService.updateClusterStatus(stack.getId());
                LOGGER.info("Instances of Hostgroup: '{}' has KAFKA_BROKER running on them. " +
                        "Moving forward for vertical scaling without decommissioning them.", request.getHostGroupName());
            }
            return new RollingVerticalScaleDecommissionInstancesResult(request.getResourceId(), rollingVerticalScaleResult);
        } catch (Exception e) {
            String message = "Failed while attempting to decommission nodes via CM";
            LOGGER.error(message, e);
            rollingVerticalScaleService.failedToDecommissionInstances(request.getResourceId(), request.getHostsNameToDecommission(),
                    request.getHostGroupName(), e.getMessage());
            return new RollingVerticalScaleDecommissionInstancesResult(request.getResourceId(), rollingVerticalScaleResult);
        }
    }
}
