package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.restart.ClusterServicesRestartRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.restart.ClusterServicesRestartResult;
import com.sequenceiq.cloudbreak.service.ClusterServicesRestartService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class ClusterServicesRestartHandler implements EventHandler<ClusterServicesRestartRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServicesRestartHandler.class);

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
    private ClusterServicesRestartService clusterServicesRestartService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterServicesRestartRequest.class);
    }

    @Override
    public void accept(Event<ClusterServicesRestartRequest> event) {
        ClusterServicesRestartRequest request = event.getData();
        ClusterServicesRestartResult result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            Optional<Stack> datalakeStack = datalakeService.getDatalakeStackByDatahubStack(stack);
            CmTemplateProcessor blueprintProcessor = getCmTemplateProcessor(stack.getCluster());
            if (datalakeStack.isPresent() && clusterServicesRestartService.isRDCRefreshNeeded(stack, datalakeStack.get())) {
                LOGGER.info("Deploying client config and restarting services");
                clusterServicesRestartService.refreshClusterOnRestart(stack, datalakeStack.get(), blueprintProcessor);
            } else {
                LOGGER.info("Restarting services");
                apiConnectors.getConnector(stack).clusterModificationService().restartClusterServices();
            }
            result = new ClusterServicesRestartResult(request);
        } catch (Exception e) {
            result = new ClusterServicesRestartResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private CmTemplateProcessor getCmTemplateProcessor(Cluster cluster) {
        String blueprintText = cluster.getBlueprint().getBlueprintText();
        return cmTemplateProcessorFactory.get(blueprintText);
    }
}
