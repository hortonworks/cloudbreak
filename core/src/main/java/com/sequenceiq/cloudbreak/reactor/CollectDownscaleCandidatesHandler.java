package com.sequenceiq.cloudbreak.reactor;

import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariDecommissioner;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CollectDownscaleCandidatesHandler implements ReactorEventHandler<CollectDownscaleCandidatesRequest> {

    static final String ERROR_STATUS_REASON = "FQDN and Scaling Adjustment is missing, instance cannot be removed.";

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectDownscaleCandidatesHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private AmbariDecommissioner ambariDecommissioner;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CollectDownscaleCandidatesRequest.class);
    }

    @Override
    public void accept(Event<CollectDownscaleCandidatesRequest> event) {
        CollectDownscaleCandidatesRequest request = event.getData();
        CollectDownscaleCandidatesResult result;
        try {
            Stack stack = stackService.getByIdWithLists(request.getStackId());
            result = verifyRequestAndCreateResult(request, stack);
        } catch (Exception e) {
            result = new CollectDownscaleCandidatesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private CollectDownscaleCandidatesResult verifyRequestAndCreateResult(CollectDownscaleCandidatesRequest request, Stack stack) throws CloudbreakException {
        CollectDownscaleCandidatesResult result;
        if (isFqdnMissing(request) && Objects.isNull(request.getScalingAdjustment())) {
            LOGGER.error("No FQDN and scaling adjustment was found in the CollectDownscaleCandidatesRequest.");
            result = new CollectDownscaleCandidatesResult(ERROR_STATUS_REASON, new CloudbreakException(ERROR_STATUS_REASON), request);
        } else if (isFqdnMissing(request)) {
            LOGGER.warn("No FQDN was found in the CollectDownscaleCandidatesRequest.");
            Set<String> hostNames = ambariDecommissioner.collectDownscaleCandidates(stack, request.getHostGroupName(), request.getScalingAdjustment());
            result = new CollectDownscaleCandidatesResult(request, hostNames);
        } else {
            Set<String> hostNames = request.getHostNames();
            ambariDecommissioner.verifyNodeCount(stack, stack.getCluster(), hostNames.stream().findFirst().get());
            result = new CollectDownscaleCandidatesResult(request, hostNames);
        }
        return result;
    }

    private boolean isFqdnMissing(CollectDownscaleCandidatesRequest request) {
        try {
            return CollectionUtils.isEmpty(request.getHostNames()) || StringUtils.isEmpty(request.getHostNames().stream().findFirst().get());
        } catch (NullPointerException e) {
            return true;
        }
    }
}
