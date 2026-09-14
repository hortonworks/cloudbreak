package com.sequenceiq.redbeams.service;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.gcp.tag.CloudPlatformTagKeyNormalizerProvider;
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

    @Inject
    private CloudPlatformTagKeyNormalizerProvider tagKeyNormalizerProvider;

    public FlowIdentifier triggerUserDefinedTagsUpdate(String resourceCrn, Map<String, String> userDefinedTags) {
        DBStack dbStack = dbStackService.getByCrn(resourceCrn);
        validateUserDefinedTagsAgainstDefaultTags(dbStack, userDefinedTags);
        return redbeamsFlowManager.triggerUserDefinedTagsUpdate(dbStack.getId(), userDefinedTags);
    }

    public FlowIdentifier triggerUserDefinedTagsDelete(String resourceCrn, Set<String> tagKeys) {
        DBStack dbStack = dbStackService.getByCrn(resourceCrn);
        validateTagKeysToRemove(dbStack, tagKeys);
        return redbeamsFlowManager.triggerUserDefinedTagsDelete(dbStack.getId(), tagKeys);
    }

    private void validateUserDefinedTagsAgainstDefaultTags(DBStack dbStack, Map<String, String> userDefinedTags) {
        if (dbStack.getTags() != null) {
            StackTags stackTags = dbStack.getTags().getUnchecked(StackTags.class);
            ValidationResult validationResult = userDefinedTagValidator.validateAgainstDefaultTags(userDefinedTags, stackTags.getDefaultTags());
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
        }
    }

    private void validateTagKeysToRemove(DBStack dbStack, Set<String> tagKeys) {
        if (dbStack.getTags() != null) {
            StackTags stackTags = dbStack.getTags().getUnchecked(StackTags.class);
            ValidationResult validationResult = userDefinedTagValidator.validateTagKeysToRemove(
                    tagKeys, stackTags.getDefaultTags(), stackTags.getApplicationTags(),
                    tagKeyNormalizerProvider.forPlatform(dbStack.getCloudPlatform()));
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
        }
    }
}
