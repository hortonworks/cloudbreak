package com.sequenceiq.cloudbreak.reactor.handler.recipe;


import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRepairSingleMasterRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRepairSingleMasterRecipesResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UploadRepairSingleMasterRecipesHandler implements ReactorEventHandler<UploadRepairSingleMasterRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UploadRepairSingleMasterRecipesRequest.class);
    }

    @Override
    public void accept(Event<UploadRepairSingleMasterRecipesRequest> event) {
        UploadRepairSingleMasterRecipesRequest request = event.getData();
        UploadRepairSingleMasterRecipesResult result;
        try {
            clusterUpscaleService.uploadRecipesOnNewHosts(request.getStackId(), request.getHostGroupName());
            result = new UploadRepairSingleMasterRecipesResult(request);
        } catch (Exception e) {
            result = new UploadRepairSingleMasterRecipesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
