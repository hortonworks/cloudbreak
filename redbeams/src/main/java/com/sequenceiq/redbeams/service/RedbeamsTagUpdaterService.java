package com.sequenceiq.redbeams.service;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.tag.UserDefinedTagValidator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
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

    @Inject
    private UserDefinedTagValidator userDefinedTagValidator;

    public FlowIdentifier triggerUserDefinedTagsUpdate(String resourceCrn, Map<String, String> userDefinedTags) {
        DBStack dbStack = dbStackService.getByCrn(resourceCrn);
        validateUserDefinedTagsAgainstDefaultTags(dbStack, userDefinedTags);
        return redbeamsFlowManager.triggerUserDefinedTagsUpdate(dbStack.getId(), userDefinedTags);
    }

    private void validateUserDefinedTagsAgainstDefaultTags(DBStack dbStack, Map<String, String> userDefinedTags) {
        if (dbStack.getTags() == null) {
            return;
        }
        StackTags stackTags = dbStack.getTags().getUnchecked(StackTags.class);
        ValidationResult validationResult = userDefinedTagValidator.validateAgainstDefaultTags(userDefinedTags, stackTags.getDefaultTags());
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }
}
