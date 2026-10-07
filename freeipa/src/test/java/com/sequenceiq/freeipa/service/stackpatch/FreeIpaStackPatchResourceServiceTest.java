package com.sequenceiq.freeipa.service.stackpatch;

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

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaStackPatchResourceServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @InjectMocks
    private FreeIpaStackPatchResourceService underTest;

    @Test
    void testLoadResource() {
        Stack stack = new Stack();
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        assertThat(underTest.loadResource(STACK_ID)).isSameAs(stack);
    }

    @Test
    void testIsUnschedulableForSchedulableAndUnschedulableStatus() {
        assertThat(underTest.isUnschedulable(stackWithStatus(Status.AVAILABLE))).isFalse();
        assertThat(underTest.isUnschedulable(stackWithStatus(Status.DELETE_COMPLETED))).isTrue();
    }

    @Test
    void testFindAllSchedulableResources() {
        List<JobResource> jobResources = List.of(mock(JobResource.class));
        when(stackService.findAllForAutoSync()).thenReturn(jobResources);

        assertThat(underTest.findAllSchedulableResources()).isSameAs(jobResources);
    }

    @Test
    void testGetMdcContextInfoProvider() {
        Stack stack = new Stack();
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);

        Optional<MdcContextInfoProvider> result = underTest.getMdcContextInfoProvider(STACK_ID);

        assertThat(result).containsSame(stack);
    }

    @Test
    void testGetMdcContextInfoProviderWhenStackIsNull() {
        when(stackService.getStackById(STACK_ID)).thenReturn(null);

        assertThat(underTest.getMdcContextInfoProvider(STACK_ID)).isEmpty();
    }

    @Test
    void testGetJobResource() {
        JobResource jobResource = mock(JobResource.class);
        when(stackService.getJobResource(STACK_ID)).thenReturn(jobResource);

        assertThat(underTest.getJobResource(STACK_ID)).isSameAs(jobResource);
    }

    private Stack stackWithStatus(Status status) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        Stack stack = new Stack();
        stack.setStackStatus(stackStatus);
        return stack;
    }
}
