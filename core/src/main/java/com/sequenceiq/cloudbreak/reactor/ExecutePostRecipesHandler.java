package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExecutePostRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExecutePostRecipesResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterUpscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ExecutePostRecipesHandler implements ClusterEventHandler<ExecutePostRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public Class<ExecutePostRecipesRequest> type() {
        return ExecutePostRecipesRequest.class;
    }

    @Override
    public void accept(Event<ExecutePostRecipesRequest> event) {
        ExecutePostRecipesRequest request = event.getData();
        ExecutePostRecipesResult result;
        try {
            clusterUpscaleService.executePostRecipes(request.getStackId(), request.getHostGroupName());
            result = new ExecutePostRecipesResult(request);
        } catch (Exception e) {
            result = new ExecutePostRecipesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }
}
