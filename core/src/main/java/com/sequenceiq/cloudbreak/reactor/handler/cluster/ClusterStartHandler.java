package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartResult;
import com.sequenceiq.cloudbreak.service.ClusterServicesRestartService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterStartHandler implements EventHandler<ClusterStartRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartHandler.class);

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Inject
    private DatalakeService datalakeService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ClusterServicesRestartService clusterServicesRestartService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterStartRequest.class);
    }

    @Override
    public void accept(Event<ClusterStartRequest> event) {
        ClusterStartRequest request = event.getData();
        ClusterStartResult result;
        int requestId;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            Optional<Stack> datalakeStack = datalakeService.getDatalakeStackByDatahubStack(stack);
            CmTemplateProcessor blueprintProcessor = getCmTemplateProcessor(stack.getCluster());
            if (datalakeStack.isPresent() && clusterServicesRestartService.isRDCRefreshNeeded(stack, datalakeStack.get())) {
                requestId = clusterServicesRestartService.refreshClusterOnStart(stack, datalakeStack.get(), blueprintProcessor);
            } else {
                requestId = apiConnectors.getConnector(stack).startCluster();
            }
            handleStopStartScalingFeature(stack, blueprintProcessor);
            result = new ClusterStartResult(request, requestId);
        } catch (Exception e) {
            result = new ClusterStartResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    @VisibleForTesting
    void handleStopStartScalingFeature(Stack stack, CmTemplateProcessor blueprintProcessor) {
        if (stackUtil.stopStartScalingEntitlementEnabled(stack)) {
            Set<String> computeGroups = getComputeHostGroups(blueprintProcessor);
            if (computeGroups.isEmpty()) {
                return;
            }
            List<String> decommissionedHostsFromCM = apiConnectors.getConnector(stack).clusterStatusService().getDecommissionedHostsFromCM();
            if (decommissionedHostsFromCM.isEmpty()) {
                return;
            }
            Set<String> decommissionedComputeHosts = new HashSet<>();
            for (String group : computeGroups) {
                String groupWithPrefix = '-' + group;
                for (String hostName : decommissionedHostsFromCM) {
                    if (hostName.contains(groupWithPrefix)) {
                        decommissionedComputeHosts.add(hostName);
                    }
                }
            }
            if (!decommissionedComputeHosts.isEmpty()) {
                apiConnectors.getConnector(stack).clusterCommissionService().recommissionHosts(new ArrayList<>(decommissionedComputeHosts));
            }
        }
    }

    private CmTemplateProcessor getCmTemplateProcessor(Cluster cluster) {
        String blueprintText = cluster.getBlueprint().getBlueprintText();
        return cmTemplateProcessorFactory.get(blueprintText);
    }

    private Set<String> getComputeHostGroups(CmTemplateProcessor blueprintProcessor) {
        Versioned blueprintVersion = () -> blueprintProcessor.getVersion().get();
        return blueprintProcessor.getComputeHostGroups(blueprintVersion);
    }
}
