package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostServiceDeploymentRecipesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostServiceDeploymentRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostServiceDeploymentRecipesSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ExecutePostServiceDeploymentRecipesHandler extends ExceptionCatcherEventHandler<ExecutePostServiceDeploymentRecipesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutePostServiceDeploymentRecipesHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExecutePostServiceDeploymentRecipesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ExecutePostServiceDeploymentRecipesRequest> event) {
        LOGGER.error("ExecutePostServiceDeploymentRecipesHandler step failed with the following message: {}", e.getMessage());
        return new ExecutePostServiceDeploymentRecipesFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ExecutePostServiceDeploymentRecipesRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.executePostServiceDeploymentRecipes(stackId);
            response = new ExecutePostServiceDeploymentRecipesSuccess(stackId);
        } catch (RuntimeException | CloudbreakException e) {
            LOGGER.error("ExecutePostServiceDeploymentRecipesHandler step failed with the following message: {}", e.getMessage());
            response = new ExecutePostServiceDeploymentRecipesFailed(stackId, e);
        }
        return response;
    }
}
