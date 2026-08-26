package com.sequenceiq.cloudbreak.reactor.handler.rollingvs;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_BROKER;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_CM_TIMEOUT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_WAITING_HOSTSTART;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterCommissionService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleCommissionInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleCommissionInstancesResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RollingVerticalScaleCommissionInstancesHandler extends ExceptionCatcherEventHandler<RollingVerticalScaleCommissionInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingVerticalScaleCommissionInstancesHandler.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private RollingVerticalScaleService rollingVerticalScaleService;

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RollingVerticalScaleCommissionInstancesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RollingVerticalScaleCommissionInstancesRequest> event) {
        LOGGER.error("Unexpected exception occurred in RollingVerticalScaleCommissionInstancesHandler for resourceId: {}", resourceId, e);
        return new StackFailureEvent(RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_FAILURE_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RollingVerticalScaleCommissionInstancesRequest> event) {
        LOGGER.info("RollingVerticalScaleCommissionInstancesHandler: {}", event.getData().getResourceId());
        RollingVerticalScaleCommissionInstancesRequest request = event.getData();
        RollingVerticalScaleResult rollingVerticalScaleResult = request.getRollingVerticalScaleResult();
        List<InstanceMetadataView> startedInstancesToCommission = request.getStartedInstancesToCommission();
        Long stackId = request.getResourceId();
        try {
            Stack stack = stackService.getByIdWithLists(request.getResourceId());
            CmTemplateProcessor processor = cmTemplateProcessorFactory.get(stack.getBlueprintJsonText());
            Set<String> nonGatewayComponents = processor.getNonGatewayComponentsByHostGroup()
                    .getOrDefault(request.getHostGroupName(), Set.of());
            if (!nonGatewayComponents.contains(KAFKA_BROKER) && !startedInstancesToCommission.isEmpty()) {
                flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_VERTICALSCALE_WAITING_HOSTSTART,
                        String.valueOf(startedInstancesToCommission.size()));

                ClusterSetupService clusterSetupService = clusterApiConnectors.getConnector(stack).clusterSetupService();
                ExtendedPollingResult extendedPollingResult = clusterSetupService.waitForHostsHealthy(new HashSet<>(startedInstancesToCommission));
                List<InstanceMetadataView> healthyInstancesToCommission;
                List<String> unhealthyInstanceIds = new ArrayList<>();
                if (!extendedPollingResult.isSuccess()) {
                    healthyInstancesToCommission = startedInstancesToCommission.stream().filter(instanceMetadataView ->
                            !extendedPollingResult.getFailedInstancePrivateIds().contains(instanceMetadataView.getPrivateId())).toList();
                    List<InstanceMetadataView> unhealthyInstances = startedInstancesToCommission.stream()
                            .filter(instanceMetadataView -> extendedPollingResult.getFailedInstancePrivateIds()
                                    .contains(instanceMetadataView.getPrivateId())).toList();
                    unhealthyInstanceIds = unhealthyInstances.stream()
                            .map(InstanceMetadataView::getInstanceId)
                            .collect(Collectors.toCollection(ArrayList::new));
                    if (healthyInstancesToCommission.isEmpty()) {
                        throw new BadRequestException(String.format("Operation timed out. " +
                                        "Failed while waiting for %d nodes to move into health state. MissingNodes=[%s]",
                                startedInstancesToCommission.size(),
                                startedInstancesToCommission.stream().map(InstanceMetadataView::getDiscoveryFQDN).toList()));
                    }
                    flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_VERTICALSCALE_CM_TIMEOUT,
                            String.valueOf(startedInstancesToCommission.size()), String.valueOf(unhealthyInstances.size()),
                            unhealthyInstances.stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.joining(", ")));
                } else {
                    healthyInstancesToCommission = List.copyOf(startedInstancesToCommission);
                }
                flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED,
                        String.valueOf(healthyInstancesToCommission.size()));

                ClusterCommissionService clusterCommissionService = clusterApiConnectors.getConnector(stack).clusterCommissionService();

                Set<String> hostNames = healthyInstancesToCommission.stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toSet());
                LOGGER.debug("HostNames to recommission: count={}, hostNames={}", hostNames.size(), hostNames);

                HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), request.getHostGroupName())
                        .orElseThrow(NotFoundException.notFound("hostgroup", request.getHostGroupName()));

                Map<String, InstanceMetaData> hostsToRecommission = clusterCommissionService.collectHostsToCommission(hostGroup, hostNames);

                rollingVerticalScaleService.waitingForServicesHealthy(stackId, request.getHostGroupName(), healthyInstancesToCommission.stream()
                        .map(InstanceMetadataView::getInstanceId).toList());
                Set<String> recommissionedHostnames = clusterCommissionService.recommissionClusterNodes(hostsToRecommission);
                unhealthyInstanceIds.addAll(
                        hostsToRecommission.entrySet().stream()
                                .filter(entry -> entry.getValue() != null && entry.getValue().getInstanceId() != null)
                                .filter(entry -> !recommissionedHostnames.contains(entry.getKey()))
                                .map(entry -> entry.getValue().getInstanceId())
                                .toList()
                );

                List<String> recommissionedInstanceIds = hostsToRecommission.entrySet().stream()
                        .filter(entry -> recommissionedHostnames.contains(entry.getKey()))
                        .map(entry -> entry.getValue().getInstanceId())
                        .toList();
                if (!unhealthyInstanceIds.isEmpty()) {
                    LOGGER.info("Recommissioned fewer instances than requested. recommissionedCount={}, " +
                                    "expectedCount={}, initialCount={}, notRecommissioned=[{}]",
                            recommissionedHostnames.size(), hostsToRecommission.size(), request.getStartedInstancesToCommission().size(), unhealthyInstanceIds);
                    updateRollingVerticalScaleResultWithNonHealthyServices(stackId, rollingVerticalScaleResult, unhealthyInstanceIds);
                }
                updateRollingVerticalScaleResult(stackId, rollingVerticalScaleResult, recommissionedInstanceIds);
            } else if (nonGatewayComponents.contains(KAFKA_BROKER)) {
                LOGGER.info("Instances of Hostgroup: '{}' has KAFKA_BROKER running on them. " +
                        "Starting the instances without commissioning the services.", request.getHostGroupName());
                List<String> startedInstanceIds = startedInstancesToCommission.stream().map(InstanceMetadataView::getInstanceId).toList();
                updateRollingVerticalScaleResult(stackId, rollingVerticalScaleResult, startedInstanceIds);
            }
            return new RollingVerticalScaleCommissionInstancesResult(stackId, rollingVerticalScaleResult);
        } catch (Exception e) {
            String message = "Failed while attempting to commission nodes via CM";
            LOGGER.error(message, e);
            rollingVerticalScaleService.failedCommissionInstances(stackId,
                    startedInstancesToCommission.stream().map(InstanceMetadataView::getInstanceId).toList(), rollingVerticalScaleResult.getGroup(),
                    e.getMessage());
            return new RollingVerticalScaleCommissionInstancesResult(stackId, rollingVerticalScaleResult);
        }
    }

    private void updateRollingVerticalScaleResultWithNonHealthyServices(Long stackId,
            RollingVerticalScaleResult rollingVerticalScaleResult, List<String> failedInstanceIds) {
        for (String instanceId : failedInstanceIds) {
            rollingVerticalScaleResult.setStatus(instanceId, RollingVerticalScaleStatus.SERVICES_UNHEALTHY);
        }
        rollingVerticalScaleService.updateInstancesToServiceUnhealthy(stackId, rollingVerticalScaleResult.getGroup(), failedInstanceIds);
    }

    private void updateRollingVerticalScaleResult(Long stackId, RollingVerticalScaleResult rollingVerticalScaleResult, List<String> instanceIds) {
        for (String instanceId : instanceIds) {
            rollingVerticalScaleResult.setStatus(instanceId, RollingVerticalScaleStatus.SUCCESS);
        }
        rollingVerticalScaleService.updateInstancesToServicesHealthy(stackId, instanceIds);
    }

}
