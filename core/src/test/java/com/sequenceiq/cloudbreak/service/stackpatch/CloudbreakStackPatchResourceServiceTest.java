package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class CloudbreakStackPatchResourceServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private CloudbreakStackPatchResourceService underTest;

    @Test
    void testLoadResource() {
        Stack stack = new Stack();
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        assertThat(underTest.loadResource(STACK_ID)).isSameAs(stack);
    }

    @Test
    void testIsUnschedulableForSchedulableAndUnschedulableStatus() {
        Stack availableStack = mock(Stack.class);
        when(availableStack.getStatus()).thenReturn(Status.AVAILABLE);
        Stack deletedStack = mock(Stack.class);
        when(deletedStack.getStatus()).thenReturn(Status.DELETE_COMPLETED);

        assertThat(underTest.isUnschedulable(availableStack)).isFalse();
        assertThat(underTest.isUnschedulable(deletedStack)).isTrue();
    }

    @Test
    void testFindAllSchedulableResources() {
        List<JobResource> jobResources = List.of(mock(JobResource.class));
        when(stackService.getAllWhereStatusNotIn(Status.getUnschedulableStatuses())).thenReturn(jobResources);

        assertThat(underTest.findAllSchedulableResources()).isSameAs(jobResources);
    }

    @Test
    void testGetMdcContextInfoProvider() {
        StackView stackView = mock(StackView.class);
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);

        Optional<MdcContextInfoProvider> result = underTest.getMdcContextInfoProvider(STACK_ID);

        assertThat(result).containsSame(stackView);
    }

    @Test
    void testGetMdcContextInfoProviderWhenStackViewIsNull() {
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(null);

        assertThat(underTest.getMdcContextInfoProvider(STACK_ID)).isEmpty();
    }

    @Test
    void testGetJobResource() {
        JobResource jobResource = mock(JobResource.class);
        when(stackService.getJobResource(STACK_ID)).thenReturn(jobResource);

        assertThat(underTest.getJobResource(STACK_ID)).isSameAs(jobResource);
    }
}
