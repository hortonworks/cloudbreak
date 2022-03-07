package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.service.stackpatch.MockPatchService.STACK_PATCH_RESULT_TAG_KEY;
import static com.sequenceiq.cloudbreak.service.stackpatch.MockPatchService.STACK_PATCH_RESULT_TAG_VALUE_FAILURE;
import static com.sequenceiq.cloudbreak.service.stackpatch.MockPatchService.STACK_PATCH_RESULT_TAG_VALUE_SKIP;
import static com.sequenceiq.cloudbreak.service.stackpatch.MockPatchService.STACK_PATCH_RESULT_TAG_VALUE_SUCCESS;
import static com.sequenceiq.cloudbreak.service.stackpatch.MockPatchService.STACK_PATCH_TRIES_TAG_KEY;
import static com.sequenceiq.cloudbreak.service.stackpatch.MockPatchService.STACK_STATUS;
import static com.sequenceiq.cloudbreak.service.stackpatch.MockPatchService.STATUS_REASON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.StackPatchRepository;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
class MockPatchServiceTest {

    private static final long STACK_ID = 123L;

    @InjectMocks
    private MockPatchService underTest;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private StackService stackService;

    @Mock
    private StackPatchRepository stackPatchRepository;

    @Mock
    private FlowLogService flowLogService;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setResourceCrn("crn:cdp:datalake:us-west-1:tenant:datalake:935ad382-fe9c-400b-bf38-2156c1f09b6d");
        stack.setCloudPlatform(CloudPlatform.MOCK.name());

        lenient().when(stackUpdater.updateStackStatus(any(), any(), anyString())).thenReturn(stack);
    }

    @Test
    void shouldNotApplyToNotMocks() {
        stack.setCloudPlatform(CloudPlatform.AWS.name());

        boolean result = underTest.isAffected(stack);

        assertThat(result).isFalse();
    }

    @Test
    void shouldNotApplyToMockWithoutTag() {
        stack.setTags(new Json(new StackTags(Map.of(), Map.of(), Map.of())));

        boolean result = underTest.isAffected(stack);

        assertThat(result).isFalse();
    }

    @Test
    void shouldApplyToMockWithTag() {
        setStackTag(STACK_PATCH_RESULT_TAG_VALUE_SUCCESS);

        boolean result = underTest.isAffected(stack);

        assertThat(result).isTrue();
    }

    @Test
    void shouldSucceedWithSuccessTag() throws ExistingStackPatchApplyException {
        setStackTag(STACK_PATCH_RESULT_TAG_VALUE_SUCCESS);

        boolean result = underTest.doApply(stack);

        assertThat(result).isTrue();
        verify(stackUpdater).updateStackStatus(STACK_ID, STACK_STATUS, STATUS_REASON);
    }

    @Test
    void shouldFailWithFailureTag() throws IOException {
        setStackTag(STACK_PATCH_RESULT_TAG_VALUE_FAILURE);

        assertThatThrownBy(() -> underTest.doApply(stack))
                .isInstanceOf(ExistingStackPatchApplyException.class);

        verify(stackUpdater).updateStackStatus(STACK_ID, STACK_STATUS, STATUS_REASON);
        assertStackPatchTriesTagValue(1);

        assertThatThrownBy(() -> underTest.doApply(stack))
                .isInstanceOf(ExistingStackPatchApplyException.class);

        verify(stackService, times(2)).save(stack);
        assertStackPatchTriesTagValue(2);
    }

    @Test
    void shouldNotSucceedWithSkipTag() throws IOException, ExistingStackPatchApplyException {
        setStackTag(STACK_PATCH_RESULT_TAG_VALUE_SKIP);

        boolean result = underTest.doApply(stack);
        assertThat(result).isFalse();

        verify(stackUpdater).updateStackStatus(STACK_ID, STACK_STATUS, STATUS_REASON);
        assertStackPatchTriesTagValue(1);

        result = underTest.doApply(stack);
        assertThat(result).isFalse();

        verify(stackService, times(2)).save(stack);
        assertStackPatchTriesTagValue(2);
    }

    private void setStackTag(String tagValue) {
        StackTags stackTags = new StackTags(Map.of(STACK_PATCH_RESULT_TAG_KEY, tagValue), Map.of(), Map.of());
        stack.setTags(new Json(stackTags));
    }

    private void assertStackPatchTriesTagValue(int expected) throws IOException {
        assertThat(stack.getTags().get(StackTags.class).getUserDefinedTags().get(STACK_PATCH_TRIES_TAG_KEY))
                .isEqualTo(String.valueOf(expected));
    }

}
