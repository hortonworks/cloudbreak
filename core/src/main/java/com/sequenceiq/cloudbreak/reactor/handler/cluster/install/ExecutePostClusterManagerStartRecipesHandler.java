package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostClusterManagerStartRecipesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostClusterManagerStartRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostClusterManagerStartRecipesSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ExecutePostClusterManagerStartRecipesHandler extends ExceptionCatcherEventHandler<ExecutePostClusterManagerStartRecipesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutePostClusterManagerStartRecipesHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExecutePostClusterManagerStartRecipesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ExecutePostClusterManagerStartRecipesRequest> event) {
        LOGGER.error("ExecutePostClusterManagerStartRecipesHandler step failed with the following message: {}", e.getMessage());
        return new ExecutePostClusterManagerStartRecipesFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ExecutePostClusterManagerStartRecipesRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.executePostClusterManagerStartRecipes(stackId);
            response = new ExecutePostClusterManagerStartRecipesSuccess(stackId);
        } catch (RuntimeException | CloudbreakException e) {
            LOGGER.error("ExecutePostClusterManagerStartRecipesHandler step failed with the following message: {}", e.getMessage());
            response = new ExecutePostClusterManagerStartRecipesFailed(stackId, e);
        }
        return response;
    }
}
