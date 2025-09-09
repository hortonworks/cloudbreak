package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.restart.ClusterServicesRestartRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.restart.ClusterServicesRestartResult;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.ClusterServicesRestartService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterServicesRestartHandler extends ExceptionCatcherEventHandler<ClusterServicesRestartRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServicesRestartHandler.class);

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Inject
    private StackService stackService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private ClusterServicesRestartService clusterServicesRestartService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterServicesRestartRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterServicesRestartRequest> event) {
        return new ClusterServicesRestartResult(e.getMessage(), e, event.getData());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterServicesRestartRequest> event) {
        ClusterServicesRestartRequest request = event.getData();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            Optional<SdxBasicView> sdxBasicView = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(stack.getEnvironmentCrn());
            CmTemplateProcessor blueprintProcessor = getCmTemplateProcessor(stack);
            if (request.isReallocateMemory()) {
                apiConnectors.getConnector(stack).reallocateMemory();
            }
            if ((sdxBasicView.isPresent() && clusterServicesRestartService.isRemoteDataContextRefreshNeeded(stack, sdxBasicView.get()))
                    || request.isDatahubRefreshNeeded()) {
                LOGGER.info("Deploying client config and restarting services");
                clusterServicesRestartService.refreshClusterOnRestart(stack, sdxBasicView.get(), blueprintProcessor, request.isRollingRestart());
            } else {
                LOGGER.info("Restarting services");
                if (request.isRollingRestart()) {
                    apiConnectors.getConnector(stack).clusterModificationService().rollingRestartServices(request.isRestartStaleServices());
                } else {
                    apiConnectors.getConnector(stack).clusterModificationService().restartClusterServices();
                }
            }
            return new ClusterServicesRestartResult(request);
        } catch (Exception e) {
            return new ClusterServicesRestartResult(e.getMessage(), e, request);
        }
    }

    private CmTemplateProcessor getCmTemplateProcessor(Stack stack) {
        return cmTemplateProcessorFactory.get(stack.getBlueprintJsonText());
    }
}
