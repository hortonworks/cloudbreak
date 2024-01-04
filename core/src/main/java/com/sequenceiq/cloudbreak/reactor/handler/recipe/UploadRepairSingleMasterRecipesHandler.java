package com.sequenceiq.cloudbreak.reactor.handler.recipe;


import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRepairSingleMasterRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRepairSingleMasterRecipesResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class UploadRepairSingleMasterRecipesHandler implements EventHandler<UploadRepairSingleMasterRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UploadRepairSingleMasterRecipesRequest.class);
    }

    @Override
    public void accept(Event<UploadRepairSingleMasterRecipesRequest> event) {
        UploadRepairSingleMasterRecipesRequest request = event.getData();
        UploadRepairSingleMasterRecipesResult result;
        try {
            // TODO: because of CB-17116 - step removed - cleanup the code
            result = new UploadRepairSingleMasterRecipesResult(request);
        } catch (Exception e) {
            result = new UploadRepairSingleMasterRecipesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
