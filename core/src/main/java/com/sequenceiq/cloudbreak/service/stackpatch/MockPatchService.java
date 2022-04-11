package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.MOCK;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class MockPatchService extends ExistingStackPatchService {

    static final String STACK_PATCH_RESULT_TAG_KEY = "stack-patch-result";

    static final String STACK_PATCH_RESULT_TAG_VALUE_SUCCESS = "success";

    static final String STACK_PATCH_RESULT_TAG_VALUE_FAILURE = "failure";

    static final String STACK_PATCH_RESULT_TAG_VALUE_SKIP = "skip";

    static final String STACK_PATCH_TRIES_TAG_KEY = "stack-patch-tries";

    /**
     * The status to wait for in mock e2e tests
     */
    static final DetailedStackStatus STACK_STATUS = DetailedStackStatus.STACK_STARTED;

    /**
     * The status reason to check for in mock e2e tests
     */
    static final String STATUS_REASON = "Stack patch applied";

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private StackService stackService;

    @Override
    public StackPatchType getStackPatchType() {
        return MOCK;
    }

    @Override
    public int getIntervalInMinutes() {
        return 1;
    }

    @Override
    public Date getFirstStart() {
        return new Date();
    }

    @Override
    public boolean isAffected(Stack stack) {
        try {
            return CloudPlatform.MOCK.equalsIgnoreCase(stack.getCloudPlatform())
                    && stack.getTags().get(StackTags.class).getUserDefinedTags().get(STACK_PATCH_RESULT_TAG_KEY) != null;
        } catch (IOException e) {
            throw new CloudbreakRuntimeException("Failed to parse stack tags", e);
        }
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        try {
            StackTags stackTags = stack.getTags().get(StackTags.class);
            String stackPatchResultTag = stackTags.getUserDefinedTags().get(STACK_PATCH_RESULT_TAG_KEY);
            stack = stackUpdater.updateStackStatus(stack.getId(), STACK_STATUS, STATUS_REASON);
            switch (stackPatchResultTag) {
                case STACK_PATCH_RESULT_TAG_VALUE_SUCCESS:
                    return true;
                case STACK_PATCH_RESULT_TAG_VALUE_SKIP:
                    incrementTriesTag(stack, stackTags);
                    return false;
                case STACK_PATCH_RESULT_TAG_VALUE_FAILURE:
                    incrementTriesTag(stack, stackTags);
                    throw new ExistingStackPatchApplyException("Failing mock stack patch apply");
                default:
                    throw new IllegalStateException("Unexpected stack patch result tag value");
            }
        } catch (IOException e) {
            throw new ExistingStackPatchApplyException("Failed to parse stack tags", e);
        }
    }

    private void incrementTriesTag(Stack stack, StackTags stackTags) {
        int tryCount = Optional.ofNullable(stackTags.getUserDefinedTags().get(STACK_PATCH_TRIES_TAG_KEY))
                .map(Integer::parseInt)
                .orElse(0);
        stackTags.getUserDefinedTags().put(STACK_PATCH_TRIES_TAG_KEY, String.valueOf(tryCount + 1));
        stack.setTags(new Json(stackTags));
        stackService.save(stack);
    }
}
