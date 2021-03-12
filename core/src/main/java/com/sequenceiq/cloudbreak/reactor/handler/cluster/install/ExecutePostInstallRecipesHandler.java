package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostInstallRecipesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostInstallRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostInstallRecipesSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class ExecutePostInstallRecipesHandler extends ExceptionCatcherEventHandler<ExecutePostInstallRecipesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutePostInstallRecipesHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExecutePostInstallRecipesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ExecutePostInstallRecipesRequest> event) {
        LOGGER.error("ExecutePostInstallRecipesHandler step failed with the following message: {}", e.getMessage());
        return new ExecutePostInstallRecipesFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.executePostInstallRecipes(stackId);
            response = new ExecutePostInstallRecipesSuccess(stackId);
        } catch (RuntimeException | CloudbreakException e) {
            LOGGER.error("ExecutePostInstallRecipesHandler step failed with the following message: {}", e.getMessage());
            response = new ExecutePostInstallRecipesFailed(stackId, e);
        }
        return response;
    }
}
