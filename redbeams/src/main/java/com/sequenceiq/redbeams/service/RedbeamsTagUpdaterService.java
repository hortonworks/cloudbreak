package com.sequenceiq.redbeams.service;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Service
public class RedbeamsTagUpdaterService {

    @Inject
    private RedbeamsFlowManager redbeamsFlowManager;

    @Inject
    private DBStackService dbStackService;

    public FlowIdentifier triggerUserDefinedTagsUpdate(String resourceCrn, Map<String, String> userDefinedTags) {
        DBStack dbStack = dbStackService.getByCrn(resourceCrn);
        return redbeamsFlowManager.triggerUserDefinedTagsUpdate(dbStack.getId(), userDefinedTags);
    }
}
