package com.sequenceiq.freeipa.flow.freeipa.upscale.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_VALIDATE_NEW_INSTANCES_HEALTH_FINISHED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.ValidateInstancesHealthEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.stack.FreeIpaSafeInstanceHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Component
public class ValidateInstancesHealthHandler extends ExceptionCatcherEventHandler<ValidateInstancesHealthEvent> {

    private static final String PHASE = "Instance health validation";

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateInstancesHealthHandler.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private FreeIpaSafeInstanceHealthDetailsService healthService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateInstancesHealthEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateInstancesHealthEvent> event) {
        LOGGER.error("Unexpected exception during the validation of FreeIPA instances' health", e);
        return new UpscaleFailureEvent(resourceId, PHASE, Set.of(), FailureType.ERROR, Map.of(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ValidateInstancesHealthEvent> event) {
        LOGGER.debug("Validate instances health: {}", event.getData().getInstanceIds());
        List<NodeHealthDetails> healthDetails = fetchInstancesHealth(event.getData());
        List<NodeHealthDetails> notHealthyNodes = filterNotHealthyNodes(healthDetails);
        if (notHealthyNodes.isEmpty()) {
            LOGGER.info("Every checked instance seems to be healthy");
            return new StackEvent(UPSCALE_VALIDATE_NEW_INSTANCES_HEALTH_FINISHED_EVENT.event(), event.getData().getResourceId());
        } else {
            return handleUnhealthyInstancesPresent(event, healthDetails, notHealthyNodes);
        }
    }

    private UpscaleFailureEvent handleUnhealthyInstancesPresent(HandlerEvent<ValidateInstancesHealthEvent> event, List<NodeHealthDetails> healthDetails,
            List<NodeHealthDetails> notHealthyNodes) {
        LOGGER.warn("Non healthy instances found: {}", notHealthyNodes);
        Set<String> healthyInstances = collectHealthyInstanceIds(healthDetails);
        Map<String, String> failureDetails = constructFailureDetails(notHealthyNodes);
        Exception exceptionForFailureEvent = new Exception("Unhealthy instances found: " + failureDetails.keySet());
        return new UpscaleFailureEvent(event.getData().getResourceId(), PHASE, healthyInstances, VALIDATION, failureDetails, exceptionForFailureEvent);
    }

    private Map<String, String> constructFailureDetails(List<NodeHealthDetails> notHealthyNodes) {
        return notHealthyNodes.stream()
                .collect(Collectors.toMap(NodeHealthDetails::getInstanceId, hd -> Joiner.on("; ").join(hd.getIssues())));
    }

    private Set<String> collectHealthyInstanceIds(List<NodeHealthDetails> healthDetails) {
        return healthDetails.stream()
                .filter(healthDetail -> healthDetail.getStatus().isAvailable())
                .map(NodeHealthDetails::getInstanceId).collect(Collectors.toSet());
    }

    private List<NodeHealthDetails> filterNotHealthyNodes(List<NodeHealthDetails> healthDetails) {
        return healthDetails.stream()
                .filter(healthDetail -> !healthDetail.getStatus().isAvailable())
                .collect(Collectors.toList());
    }

    private List<NodeHealthDetails> fetchInstancesHealth(ValidateInstancesHealthEvent event) {
        Stack stack = stackService.getStackById(event.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDataService.getNotTerminatedByInstanceIds(stack.getId(), event.getInstanceIds());
        List<NodeHealthDetails> healthDetails = instanceMetaDatas.stream()
                .map(im -> healthService.getInstanceHealthDetails(stack, im))
                .collect(Collectors.toList());
        LOGGER.info("Fetched healthdetails for instances {} - {}", event.getInstanceIds(), healthDetails);
        return healthDetails;
    }
}
