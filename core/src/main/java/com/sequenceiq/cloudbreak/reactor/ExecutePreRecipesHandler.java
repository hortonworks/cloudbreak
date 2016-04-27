package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExecutePreRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExecutePreRecipesResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterUpscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ExecutePreRecipesHandler implements ClusterEventHandler<ExecutePreRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public Class<ExecutePreRecipesRequest> type() {
        return ExecutePreRecipesRequest.class;
    }

    @Override
    public void accept(Event<ExecutePreRecipesRequest> event) {
        ExecutePreRecipesRequest request = event.getData();
        ExecutePreRecipesResult result;
        try {
            clusterUpscaleService.executePreRecipes(request.getStackId(), request.getHostGroupName());
            result = new ExecutePreRecipesResult(request);
        } catch (Exception e) {
            result = new ExecutePreRecipesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }
}
